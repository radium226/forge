package com.github.radium226.forge.maven

import cats._
import cats.data._
import cats.implicits._
import cats.effect.concurrent._
import cats.effect._
import org.http4s._
import org.http4s.dsl.io.{Path => Http4sPath}
import org.http4s.dsl.io._
import org.http4s.server._
import org.http4s.server.blaze._
import org.http4s.implicits._
import scopt._
import java.nio.file.{Files, Path, Paths}
import java.util.stream.Collectors

import scala.concurrent.ExecutionContext
import scala.util.matching.Regex
import scala.collection.JavaConverters._

import com.github.radium226.io._

import fs2._

object Main extends IOApp {

  val ChunkSize = 1024

  def parseArguments(arguments: List[Argument]): IO[Arguments] = {
    val builder = OParser.builder[Arguments]
    val parser = {
      import builder._
      OParser.sequence(
        opt[Port]("port")
          .action({ (port, arguments) =>
            arguments.copy(port = port)
          })
          .required(),
        opt[Path]("folder")
          .action({ (folderPath, arguments) =>
            arguments.copy(folderPath = folderPath)
          })
          .required()
      )
    }

    IO.delay(OParser.parse(parser, arguments, Arguments.default)).flatMap({
      case Some(arguments) =>
        IO.pure(arguments)

      case None =>
        IO.raiseError(new Exception("Arguments are invalid! "))
    })
  }

  def upload(filePath: Path): Pipe[IO, Byte, Unit] = { stream =>
    for {
      _ <- Stream.eval[IO, Unit](makeParentFolder[IO](filePath))
      _ <- stream.through(fs2.io.file.writeAll(filePath, ExecutionContext.global))
    } yield ()
  }

  def upload(entityBody: EntityBody[IO], filePath: Path): IO[Unit] = {
    entityBody.through(upload(filePath)).compile.drain
  }

  def upload(request: Request[IO], filePath: Path): IO[Unit] = {
    upload(request.body, filePath)
  }

  case class AbsoluteFilePathVar(parentFolderPath: Path) {

    def unapply(path: Http4sPath): Option[Path] = {
      path.toPathOption.map(parentFolderPath.resolve(_))
    }

  }

  case class MavenMetadataVar(rootFolderPath: Path) {

    def unapply(path: Http4sPath): Option[Path] = {
      val segments = path.toList
      segments.lastOption match {
        case Some("maven-metadata.xml") =>
          segments
            .dropRight(1)
            .toPathOption
            .map(rootFolderPath.resolve)

        case _ =>
          None
      }
    }

  }

  def locatePOMFile(artifactFolderPath: Path): IO[Path] = {
    locateFiles[IO](artifactFolderPath, "\\.pom$".r)
      .map(_.headOption)
      .flatMap({
        case Some(pomFilePath) =>
          IO.pure(pomFilePath)

        case None =>
          IO.raiseError(new Exception(s"Unable to locate a POM file in ${artifactFolderPath}"))
      })
  }

  def peekGroupIDAndArtifactID(artifactFolderPath: Path): IO[(GroupID, ArtifactID)] = {
    for {
      pomFilePath <- locatePOMFile(artifactFolderPath)
      pom         <- POM.read[IO](pomFilePath)
    } yield (pom.groupID, pom.artifactID)
  }

  def makeRoutes(port: Port, rootFolderPath: Path): HttpRoutes[IO] = {
    val filePathVar = AbsoluteFilePathVar(rootFolderPath)
    val mavenMetadataVar = MavenMetadataVar(rootFolderPath)

    val service = HttpRoutes.of[IO] {
      case request @ PUT -> filePathVar(filePath) =>
        for {
          _        <- upload(request, filePath)
          response <- Ok(())
        } yield response

      /*case request @ GET -> mavenMetadataVar(artifactFolderPath) =>
        val response = for {
          mavenMetadata <- MavenMetadata.generate[IO](artifactFolderPath)
          xml           <- MavenMetadata.write[IO](mavenMetadata)
          response      <- Ok()
        } yield response

        response.recoverWith({
          case throwable =>
            throwable.printStackTrace()
            IO.raiseError(throwable)
        })*/

      case request @ GET -> filePathVar(filePath) =>
        IO.delay(filePath.exists()).flatMap({
          case true =>
            Ok(fs2.io.file.readAll[IO](filePath, ExecutionContext.global, ChunkSize))
          case false =>
            NotFound()
        })
    }

    val routes = Router("/" -> service)
    routes
  }

  override def run(arguments: List[Argument]): IO[ExitCode] = {
    val server = (for {
      arguments    <- parseArguments(arguments)
      routes        = makeRoutes(arguments.port, arguments.folderPath)

      serverBuilder = BlazeServerBuilder[IO]
                        .withHttpApp(routes.orNotFound)
                        .bindHttp(arguments.port, "0.0.0.0")
      _            <- serverBuilder.resource.use(_ => IO.never)
    } yield ())

    server.redeem({ throwable => throwable.printStackTrace(System.err) ; ExitCode.Error}, { _ => ExitCode.Success })
  }

}
