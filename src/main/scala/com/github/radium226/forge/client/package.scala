package com.github.radium226.forge

import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import java.nio.file._

import com.github.radium226.forge.client.Project
import com.typesafe.config.{Config => TypesafeConfig, ConfigFactory => TypesafeConfigFactory}
import scopt._
import scopt.Read._
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader

import scala.annotation.tailrec


package object client {

  type Argument = String

  type Line = String

  type URL = String

  type ID = Int

  case class Job[F[_]](id: Option[ID])

  sealed trait Action[F[_]]

  case object Build extends Action[Nothing]

  case class Logs[F[_]](job: Option[Job[F]]) extends Action[F]

  case class Project[F[_]](name: String)

  case class Config[F[_]](url: Option[URL] = None, project: Option[Project[F]] = None, action: Option[Action[F]] = None)

  object Config {

    implicit def projectValueReader[F[_]]: ValueReader[Project[F]] = new ValueReader[Project[F]] {

      override def read(typesafeConfig: TypesafeConfig, path: String): Project[F] = {
        //println(s"projectValueReader.path=${path} / projectValueReader.typesafeConfig=${typesafeConfig}")
        Project[F](typesafeConfig.as[String](s"${path}.name"))
      }

    }

    implicit def configValueReader[F[_]]: ValueReader[Config[F]] = new ValueReader[Config[F]] {

      def read(typesafeConfig: TypesafeConfig, path: String): Config[F] = {
        //println(s"configValueReader.typesafeConfig=${typesafeConfig} / path=${path}")
        val url = typesafeConfig.as[Option[URL]](s"url")
        val project = typesafeConfig.as[Option[Project[F]]]("project")
        //println(url)
        //println(s"path=${path} / typesafeConfig=${typesafeConfig}")
        Config[F](
          url = url,
          project = project
        )
      }

    }

    implicit def readProject[F[_]]: Read[Project[F]] = reads { argument =>
      Project[F](argument)
    }

    implicit def readJob[F[_]]: Read[Job[F]] = {
      intRead.map({ id => Job[F](Some(id)) })
    }

    def load[F[_]](arguments: List[Argument])(implicit F: Sync[F]): F[Config[F]] = {
      (parseArguments[F](arguments) +: List(project[F], user[F], system[F]).map(_.recover({ case _ => Config.empty[F] })))
        .traverse(identity)
        .map(_.combineAll)
    }

    def parseArguments[F[_]](arguments: List[Argument])(implicit F: Sync[F]): F[Config[F]] = {
      val builder = OParser.builder[Config[F]]

      import builder._

      val parser = OParser.sequence(
        opt[URL]("url")
          .action({ (url, config) =>
            config.copy(url = Some(url))
          })
          .optional(),
        opt[Project[F]]("project")
          .action({ (project, config) =>
            config.copy(project = Some(project))
          })
          .optional(),
        cmd("logs")
          .action({ (_, config) =>
            config.copy(action = Some(Logs[F](None)))
          })
          .children(
            opt[Job[F]]("job-id")
              .action({ (id, config) =>
                config.copy(action = config.action.collect({
                  case logs @ Logs(_) =>
                    logs.copy(job = Some(id))
                }))
              })
          )
      )

      F.delay(OParser.parse(parser, arguments, Config.empty[F])).flatMap({
        case Some(config) =>
          F.pure(config)

        case None =>
          F.raiseError(new Exception("Arguments are invalid! "))
      })
    }

    def parseFile[F[_]](filePath: Path)(implicit F: Sync[F]): F[Config[F]] = {
      F.delay({
        TypesafeConfigFactory.parseFile(filePath.toFile).as[Config[F]]
      })
    }

    def combineAll[F[_]](configs: Config[F]*): Config[F] = {
      configs.toList.combineAll
    }

    def system[F[_]](implicit F: Sync[F]): F[Config[F]] = {
      parseFile(Paths.get("/etc/forge/forge.conf"))
    }

    def user[F[_]](implicit F: Sync[F]): F[Config[F]] = {
      parseFile(Paths.get(System.getProperty("user.home")).resolve(".config/forge/forge.conf"))
    }

    def project[F[_]](implicit F: Sync[F]): F[Config[F]] = {
      for {
        filePath <- locateFile[F](Paths.get("forge.conf"))
        config   <- Config.parseFile[F](filePath)
      } yield config
    }

    def environment[F[_]](implicit F: Sync[F]): F[Config[F]] = {
      parseFile(Paths.get(System.getenv("FORGE_CONFIG_FILE_PATH")))
    }

    private def locateFile[F[_]](fileName: Path)(implicit F: Sync[F]): F[Path] = {
      // We start from the current working folder, and we move to parent until the / path

      @tailrec
      def go(folderPath: Path): F[Path] = {
        val filePath = folderPath.resolve(fileName)
        if (folderPath.equals(Paths.get("/"))) {
          F.raiseError(new Exception("We're out of filesystem boundaries"))
        } else {
          if (Files.exists(filePath)) F.pure(filePath)
          else go(folderPath.getParent)
        }
      }

      go(Paths.get(System.getProperty("user.dir")))
    }

    def empty[F[_]]: Config[F] = {
      Config[F]()
    }

  }

  implicit def configMonoid[F[_]]: Monoid[Config[F]] = new Monoid[Config[F]] {

    override def empty: Config[F] = {
      Config.empty[F]
    }

    override def combine(x: Config[F], y: Config[F]): Config[F] = {
      Config[F](
        url = x.url.orElse(y.url),
        project = x.project.orElse(y.project),
        action = x.action.orElse(y.action)
      )
    }
  }

}
