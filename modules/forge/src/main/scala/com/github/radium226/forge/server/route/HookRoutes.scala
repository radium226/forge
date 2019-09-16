package com.github.radium226.forge.server.route

import cats.effect._
import com.github.radium226.forge.model.Hook
import com.github.radium226.forge.project._
import com.github.radium226.forge.server.Config
import fs2.concurrent.Queue
import org.http4s._
import org.http4s.Method._
import org.http4s.server._
import org.http4s.dsl.io._
import org.http4s.Method.PUT
import org.http4s.Status.Accepted

import cats.implicits._

object HookRoutes {

  def apply[F[_]](config: Config[F], hookQueue: Queue[F, Hook[F]])(implicit F: Sync[F]): Resource[F, HttpRoutes[F]] = {
    Resource.pure[F, HttpRoutes[F]](HttpRoutes.of[F]({
      case request @ PUT -> Root / "projects" / projectName / "hooks" / hookName =>
        for {
          baseFolderPath <- config.baseFolderPath.liftTo[F](new Exception("Unable to retreive baseFolderPath"))
          project <- Project.lookUp(baseFolderPath, projectName)
          hook     = Hook[F](project, "kikoo")
          _        = println(s" ====> hook=${hook} <======")
          _       <- hookQueue.enqueue1(hook)
        } yield Response[F](status = Accepted)
    }))
  }

}
