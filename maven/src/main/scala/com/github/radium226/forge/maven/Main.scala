package com.github.radium226.forge.maven

import java.nio.file.Path

import cats._
import cats.data._
import cats.implicits._
import cats.effect._
import com.github.radium226.io._
import fs2._
import org.http4s.{AuthedRoutes, _}
import org.http4s.dsl.io._
import org.http4s.headers._
import org.http4s.implicits._
import org.http4s.server._
import org.http4s.server.blaze._
import org.jvnet.libpam.PAM
import scopt._

import scala.concurrent.ExecutionContext
import scala.util.Try

object Main extends IOApp {

  val ChunkSize = 1024

  val authUser: Kleisli[OptionT[IO, ?], Request[IO], User] = Kleisli({ request =>
    for {
      header <- OptionT(IO.pure(request.headers.get(Authorization)))
      (user, password) <- OptionT(IO.pure(header.credentials match {
        case BasicCredentials(user, password) =>
          Some((user, password))

        case _ =>
          None
      }))
      unixUser <- OptionT(IO.delay({
        val pam = new PAM("forge")
        Try(pam.authenticate(user, password)).map(Some(_)).getOrElse(None)
      }))
    } yield unixUser.getUserName
  })

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

  def makeRoutes(port: Port, rootFolderPath: Path): HttpRoutes[IO] = {
    val middleware: AuthMiddleware[IO, User] = AuthMiddleware[IO, User](authUser)
    val filePathVar = AbsoluteFilePathVar(rootFolderPath)

    val service = AuthedRoutes.of[User, IO] {
      case (request @ PUT -> filePathVar(filePath)) as user =>
        for {
          _        <- upload(request, filePath)
          response <- Ok(())
        } yield response

      case request @ GET -> filePathVar(filePath) as user =>
        IO.delay(filePath.exists()).flatMap({
          case true =>
            Ok(fs2.io.file.readAll[IO](filePath, ExecutionContext.global, ChunkSize))
          case false =>
            NotFound()
        })
    }

    val routes = Router("/maven2" -> middleware(service))
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
