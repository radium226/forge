package com.github.radium226.forge.client

import java.nio.file.{Path, Paths}

import cats.effect.Sync
import cats.kernel.Monoid
import com.github.radium226.forge.config.ConfigBuilder
import com.typesafe.config.{Config => TypeSafeConfig, ConfigFactory => TypeSafeConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader
import scopt._
import scopt.Read._
import com.github.radium226.scopt.{ implicits => ScoptImplicits }
import com.github.radium226.ficus.{ implicits => FicusImplicits }
import cats.implicits._

trait ConfigSupport extends ScoptImplicits with FicusImplicits {

  implicit def catsConfigMonoid[F[_]]: Monoid[Config[F]] = new Monoid[Config[F]] {

    override def empty: Config[F] = {
      Config.empty[F]
    }

    override def combine(fallbackConfig: Config[F], config: Config[F]): Config[F] = {
      Config[F](
        port = config.port.orElse(fallbackConfig.port),
        host = config.host.orElse(fallbackConfig.host),
        action = config.action,
        folderPath = config.folderPath
      )
    }
  }

  implicit def ficusConfigValueReader[F[_]]: ValueReader[Config[F]] = new ValueReader[Config[F]] {

    def read(typeSafeConfig: TypeSafeConfig, path: String): Config[F] = {
      val fixedTypeSafeConfig = if (path == ".") typeSafeConfig else typeSafeConfig.getConfig(path)
      val port = fixedTypeSafeConfig.as[Option[Int]](s"port")
      val host = fixedTypeSafeConfig.as[Option[String]]("host")
      Config[F](
        port = port,
        host = host
      )
    }

  }

  implicit def configBuilder[F[_]]: ConfigBuilder[F, Config[F]] = new ConfigBuilder[F, Config[F]] {

    def appName: String = "forge"

    def moduleName: String = "client"

    def default(implicit F: Sync[F]) : F[Config[F]] = {
      F.delay({
        TypeSafeConfigFactory.defaultReference().as[Config[F]](s"${appName}.${moduleName}")
      })
    }

    def parseArguments(arguments: List[String])(implicit F: Sync[F]): F[Config[F]] = {
      val builder = OParser.builder[Config[F]]
      import builder._

      val parser = OParser.sequence(
        opt[Int]("port")
          .action({ (port, config) =>
            config.copy(port = Some(port))
          })
          .optional(),
        opt[Path]("folder-path")
          .action({ (folderPath, config) =>
            config.copy(folderPath = folderPath)
          })
          .optional(),
        opt[String]("host")
          .action({ (host, config) =>
            config.copy(host = Some(host))
          })
          .optional(),
        cmd("init")
          .action({ (_, config) =>
            config.copy(action = Action.Init.default)
          })
          .children(
            opt[String]("project-name")
              .action({ (projectName, config) =>
                config.copyAction({
                  case action: Action.Init =>
                    action.copy(projectName = projectName)
                })
              })
              .optional(),
            opt[String]("template-project-name")
              .action({ (templateProjectName, config) =>
                config.copyAction({
                  case action: Action.Init =>
                    action.copy(templateProjectName = Some(templateProjectName))
                })
              })
              .optional()
          ),
        cmd("trash")
          .action({ (_, config) =>
            config.copy(action = Action.Trash)
          }),
        cmd("emit-hook")
          .action({ (_, config) =>
            config.copy(action = Action.EmitHook.default)
          })
          .children(
            opt[String]("hook-name")
              .action({ (hookName, config) =>
                config.copyAction({
                  case action: Action.EmitHook =>
                    action.copy(hookName = Some(hookName))
                })
              }),
            opt[String]("project-name")
              .action({ (projectName, config) =>
                config.copyAction({
                  case action: Action.Init =>
                    action.copy(projectName = projectName)
                })
              })
              .optional()
          ),
        cmd("help")
          .action({ (_, config) =>
            config.copy(action = Action.Help)
          })
      )

      F.delay(OParser.parse(parser, arguments, Config.empty[F])).flatMap(_.liftTo[F](new Exception("Unable to parse")))
    }

    def parseFile(filePath: Path)(implicit F: Sync[F]): F[Config[F]] = {
      F.delay(TypeSafeConfigFactory.parseFile(filePath.toFile).as[Config[F]])
    }

  }

}
