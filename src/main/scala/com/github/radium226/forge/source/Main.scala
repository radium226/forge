package com.github.radium226.forge.source

import cats.effect._
import cats.implicits._

import org.http4s.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.server.blaze._
import org.http4s.implicits._
import org.http4s.server._
import org.http4s._
import org.http4s.dsl.io.{Path => Http4sPath}

import java.nio.file.{Files, Paths, Path => JavaPath}

import com.github.radium226.forge.PimpedHttp4sPath

import scala.concurrent.ExecutionContext


object ProjectNameVar {

  def unapply(path: String): Option[ProjectName] = {
    Some(path.dropRight(".git".length))
  }

}

object Main extends IOApp {

  def makeRoutes(rootFolderPath: JavaPath): IO[HttpRoutes[IO]] = {
    FastCGIRoute.of[IO]()
    /*IO.pure(HttpRoutes.of[IO]({
      case request @ _ -> ProjectNameVar(projectName) /: path =>
        val projectFolderPath = rootFolderPath.resolve(projectName)
        path
          .toPathOption
          .map(projectFolderPath.resolve)
          .map({ filePath =>
            println(s"methodll=${request.method}")
            println(s"filePath=${filePath}")
            if (Files.exists(filePath))
              Ok(fs2.io.file.readAll[IO](filePath, ExecutionContext.global, 32))
            else
              NotFound()
          })
          .getOrElse(NotFound())
    }))*/
  }

  override def run(arguments: List[Argument]): IO[ExitCode] = {
    val server = (for {
      routes <- makeRoutes(Paths.get("/tmp/git-server"))

      serverBuilder = BlazeServerBuilder[IO]
        .withHttpApp(routes.orNotFound)
        .bindHttp(8080, "0.0.0.0")
      _      <- serverBuilder.resource.use(_ => IO.never)
    } yield ())

    server.recover({
      case t =>
        t.printStackTrace(System.err)
        ()
    }).as(ExitCode.Success)
  }

}
