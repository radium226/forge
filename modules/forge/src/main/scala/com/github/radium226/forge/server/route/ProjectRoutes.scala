package com.github.radium226.forge.server.route

import cats.effect._
import com.github.radium226.forge.project.Project
import com.github.radium226.forge.server._
import org.http4s.{HttpRoutes, Response, Status}
import org.http4s.dsl.io._
import cats.implicits._

object ProjectRoutes {

  object ProjectNameQueryParamMatcher extends QueryParamDecoderMatcher[String]("projectName")

  def apply[F[_]](config: Config[F])(implicit F: Sync[F]): Resource[F, HttpRoutes[F]] = {
    Resource.liftF(for {
      baseFolderPath    <- config.baseFolderPath.liftTo[F](new Exception("Unable to retreive baseFolderPath"))
      scriptFolderPath  <- config.baseFolderPath.liftTo[F](new Exception("Unable to retreive baseFolderPath"))
      routes          = HttpRoutes.of[F]({
        case POST -> Root / "projects" :? ProjectNameQueryParamMatcher(projectName) =>
          for {
            project  <- Project.init[F](baseFolderPath, scriptFolderPath, projectName)
            response  = Response[F](status = Status.Ok)
          } yield response

        case DELETE -> Root / "projects" / projectName =>
          for {
            project  <- Project.lookUp[F](baseFolderPath, projectName)
            _        <- project.trash
            response  = Response[F](status = Status.Ok)
          } yield response
      })
    } yield routes)
  }

}
