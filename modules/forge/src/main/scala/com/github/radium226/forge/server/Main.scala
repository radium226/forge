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
import com.github.radium226.forge.project.Project
import com.github.radium226.forge.server.route.{GitRoutes, HookRoutes, ProjectRoutes}
import org.http4s.implicits._
import cats.implicits._
import com.github.radium226.forge.model.Hook
import com.github.radium226.forge.run.{Phase, Runner}
import fs2.concurrent.Queue

import com.github.radium226.config._

object Main extends IOApp {

  def serve(settings: Settings, httpApp: HttpApp[IO]): Resource[IO, Server[IO]] = {
    BlazeServerBuilder[IO]
      .withHttpApp(httpApp)
      .bindHttp(settings.port, "0.0.0.0")
      .resource
  }

  def makeRoutes(settings: Settings, hookQueue: Queue[IO, Hook[IO]]): Resource[IO, HttpRoutes[IO]] = {
    NonEmptyList.of(
      GitRoutes[IO](settings),
      ProjectRoutes[IO](settings),
      HookRoutes[IO](settings, hookQueue)
    ).sequence.map(_.reduceK)
  }

  def build(project: Project[IO]): IO[Unit] = {
    val runnerFolderPath = project.folderPath.resolve("runner")
    for {
      _      <- IO(Files.createDirectories(runnerFolderPath))
      _      <- project.repo.cloneTo(runnerFolderPath)
      runner <- Runner[IO](runnerFolderPath)
      _      <- runner.run(Phase.Package)
    } yield ()
  }

  def triggerBuild(config: Settings, hookQueue: Queue[IO, Hook[IO]]): Resource[IO, Unit] = {
    Resource.liftF(hookQueue.dequeue
      .evalMap({ hook =>
        build(hook.project)
      })
      .compile
      .drain
      .start
      .void)
  }

  override def run(arguments: List[String]): IO[ExitCode] = {
   (for {
      hookQueue      <- Resource.liftF(Queue.unbounded[IO, Hook[IO]])
      settings       <- Resource.liftF(Config[IO, Settings].parse(arguments: _*))
      _               = println(settings)
      httpRoutes     <- makeRoutes(settings, hookQueue)
      httpApp         = httpRoutes.orNotFound
      _              <- serve(settings, httpApp)
      _              <- triggerBuild(settings, hookQueue)
    } yield ()).use({ _ => IO.never })
  }

}
