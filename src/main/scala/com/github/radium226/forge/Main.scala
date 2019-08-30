package com.github.radium226.forge

import java.nio.file.Path

import cats.data.{Kleisli, OptionT}
import cats.effect.{ExitCode, IO, IOApp}
import com.github.radium226.forge._
import com.github.radium226.forge.pacman.Pacman
import com.github.radium226.io.makeParentFolder
import fs2.{Pipe, Stream}
import org.http4s.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.server.blaze._
import org.http4s.implicits._
import org.http4s.server._
import org.http4s._
import org.http4s.server.middleware.authentication.BasicAuth
import org.http4s.server.middleware.authentication.BasicAuth.BasicAuthenticator
import org.jvnet.libpam.PAM
import scopt.OParser
import com.github.radium226.forge.maven.Maven

import scala.concurrent.ExecutionContext
import scala.util._

object Main extends IOApp {

  val ChunkSize = 1024

  val authUser: Kleisli[OptionT[IO, ?], Request[IO], User] = Kleisli({ request =>
    println(request)
    for {
      header <- OptionT(IO.pure(request.headers.get(Authorization)))
      (user, password) <- OptionT(IO.pure(header.credentials match {
        case BasicCredentials(user, password) =>
          println(s"Trying to auth ${user} using ${password}")
          Option((user, password))

        case _ =>
          println(s"Not even trying to auth")
          None
      }))
      unixUser <- OptionT(IO.delay({
        val pam = new PAM("forge")
        Try(pam.authenticate(user, password)).map(Some(_)).getOrElse(None)
      }))
      _        <- if (unixUser.getGroups.contains("forge")) OptionT.some[IO](()) else OptionT.none[IO, Unit]
    } yield unixUser.getUserName
    //OptionT.some[IO]("forge")
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
          .optional(),
        opt[Path]("folder")
          .action({ (folderPath, arguments) =>
            arguments.copy(folderPath = folderPath)
          })
          .optional()
      )
    }

    IO.delay(OParser.parse(parser, arguments, Arguments.default)).flatMap({
      case Some(arguments) =>
        IO.pure(arguments)

      case None =>
        IO.raiseError(new Exception("Arguments are invalid! "))
    })
  }

  def authenticator: BasicAuthenticator[IO, User] = { case BasicCredentials(user, password) =>
    IO.delay({
      val pam = new PAM("forge")
      Try(pam.authenticate(user, password)) match {
        case Success(unixUser) =>
          Some(unixUser.getUserName)
        case Failure(throwable) =>
          throwable.printStackTrace()
          None
      }
    })
  }

  def makeRoutes(folderPath: Path): HttpRoutes[IO] = {

    val middleware: AuthMiddleware[IO, User] = BasicAuth[IO, User]("forge", authenticator)
    val routes = Router(
      "/maven2" -> middleware(Maven.makeRoutes[IO](folderPath.resolve("maven2"))),
      "/archlinux" -> middleware(Pacman.makeRoutes[IO](folderPath.resolve("archlinux")))
    )
    routes
  }

  override def run(arguments: List[Argument]): IO[ExitCode] = {

    val server = (for {
      arguments    <- parseArguments(arguments)
      routes        = makeRoutes(arguments.folderPath)

      serverBuilder = BlazeServerBuilder[IO]
                        .withHttpApp(routes.orNotFound)
                        .bindHttp(arguments.port, "0.0.0.0")
      _            <- serverBuilder.resource.use(_ => IO.never)
    } yield ())

    server.redeem({ throwable => throwable.printStackTrace(System.err) ; ExitCode.Error}, { _ => ExitCode.Success })
  }

}
