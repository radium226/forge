package com.github.radium226.forge.server

import java.net.http.HttpResponse

import cats._
import cats.data._
import cats.effect.concurrent._
import cats.effect._
import cats.implicits._
import com.github.radium226.system.execute._
import com.google.common.io.Resources
import fs2.concurrent._
import fs2._
import _root_.io.circe._
import _root_.io.circe.syntax._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.websocket._
import org.http4s.websocket.WebSocketFrame._
import org.http4s.server.blaze._
import org.http4s.implicits._
import org.http4s.server._
import org.http4s._

import scala.concurrent.ExecutionContext
import java.nio.file.{Files, Path, Paths}

import _root_.com.github.radium226.io._

object Main extends IOApp {

  def makeRoutes[F[_]]()(implicit F: Concurrent[F], contextShift: ContextShift[F]): F[HttpRoutes[F]] = {
   F.delay({
      HttpRoutes.of[F] {
        case POST -> Root / "projects" / projectName / "builds" =>
          for {
            project  <- Project.byName[F](projectName)
            job      <- project.build
            response  = Response[F](Ok).withEntity(s"${job.index}")
          } yield response

        case GET -> Root / "projects" / projectName / "builds" / IntVar(jobIndex) / "output" =>
          for {
            project  <- Project.byName[F](projectName)
            jobs     <- project.jobs
            _         = println(jobs)
            job      <- jobs.find(_.index == jobIndex).map(F.pure).getOrElse(F.raiseError[Job[F]](new Exception(s"There is no job #${jobIndex}! ")))
            output   <- job.output
            response  = Response[F](Ok).withEntity(output.through(fs2.text.utf8Encode[F]))
          } yield response
      }
    })
  }

  def makeServer[F[_]](port: Int, routes: HttpRoutes[F])(implicit F: ConcurrentEffect[F], timer: Timer[F]): F[Unit] = {
    BlazeServerBuilder[F]
      .withHttpApp(routes.recover({ case t => println(t) ; Response.notFound[F] }).orNotFound)
      .bindHttp(port, "0.0.0.0")
      .resource.use(_ => F.never)
  }

  override def run(arguments: List[String]): IO[ExitCode] = {
    for {
      routes <- makeRoutes[IO]()
      _      <- makeServer[IO](8080, routes)
    } yield ExitCode.Success
  }
}