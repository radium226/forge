package com.github.radium226.forge.server

import java.nio.file.{Files, Path => JavaPath}

import cats.data.{NonEmptyList, ReaderT}
import cats.effect._
import com.github.radium226.fastcgi.FastCGI
import com.github.radium226.git.Repo
import com.github.radium226.http4s.fastcgi.FastCGIAppBuilder
import org.http4s.server.Server
import org.http4s.{HttpApp, HttpRoutes, QueryParamDecoder, Request, Response}
import io.chrisdavenport.vault._
import org.http4s.dsl._
import org.http4s.dsl.io._
import org.http4s.server._
import org.http4s.server.blaze.BlazeServerBuilder
import com.github.radium226.forge.config.ConfigBuilder
import com.github.radium226.forge.project.Project
import com.github.radium226.forge.server.route.{GitRoutes, HookRoutes, ProjectRoutes}
import org.http4s.implicits._
import cats.implicits._
import com.github.radium226.forge.model.Hook
import fs2.concurrent.Queue

object Main extends IOApp with ConfigSupport {

  def serve(config: Config[IO], httpApp: HttpApp[IO]): Resource[IO, Server[IO]] = {
    for {
      port   <- Resource.liftF[IO, Int](config.port.liftTo[IO](new Exception("Unable to retreive port")))
      server <- BlazeServerBuilder[IO]
        .withHttpApp(httpApp)
        .bindHttp(port, "0.0.0.0")
        .resource
    } yield server

  }

  def makeRoutes(config: Config[IO], hookQueue: Queue[IO, Hook[IO]]): Resource[IO, HttpRoutes[IO]] = {
    NonEmptyList.of(
      GitRoutes[IO](config),
      ProjectRoutes[IO](config),
      HookRoutes[IO](config, hookQueue)
    ).sequence.map(_.reduceK)
  }

  def triggerBuild(config: Config[IO], hookQueue: Queue[IO, Hook[IO]]): Resource[IO, Unit] = {
    Resource.liftF(hookQueue.dequeue
      .evalMap({ hook =>
        IO(println(s" ------> We need to build ${hook} <------"))
      })
      .compile
      .drain
      .start
      .void)
  }

  override def run(arguments: List[String]): IO[ExitCode] = {
   (for {
      hookQueue      <- Resource.liftF(Queue.unbounded[IO, Hook[IO]])
      config         <- ConfigBuilder.resource[IO, Config[IO]](arguments)
      httpRoutes     <- makeRoutes(config, hookQueue)
      httpApp         = httpRoutes.orNotFound
      _              <- serve(config, httpApp)
      _              <- triggerBuild(config, hookQueue)
    } yield ()).use({ _ => IO.never })
  }

}
