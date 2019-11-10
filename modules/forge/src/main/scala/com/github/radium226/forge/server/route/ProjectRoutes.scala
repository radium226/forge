package com.github.radium226.forge.server.route

import cats.effect._
import com.github.radium226.forge.project.Project
import com.github.radium226.forge.server._
import org.http4s.{HttpRoutes, Response, Status}
import org.http4s.dsl.io._
import cats.implicits._

object ProjectRoutes {

  object ProjectNameQueryParamMatcher extends QueryParamDecoderMatcher[String]("projectName")

  def apply[F[_]](settings: Settings)(implicit F: Sync[F]): Resource[F, HttpRoutes[F]] = {
    Resource.pure(HttpRoutes.of[F]({
      case POST -> Root / "projects" :? ProjectNameQueryParamMatcher(projectName) =>
        for {
          project  <- Project.init[F](settings.baseFolderPath, settings.scriptFolderPath, projectName)
          response  = Response[F](status = Status.Ok)
        } yield response

      case DELETE -> Root / "projects" / projectName =>
        for {
          project  <- Project.lookUp[F](settings.baseFolderPath, projectName)
          _        <- project.trash
          response  = Response[F](status = Status.Ok)
        } yield response
    }))
  }

}
