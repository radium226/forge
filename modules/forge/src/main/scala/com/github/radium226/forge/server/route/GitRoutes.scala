package com.github.radium226.forge.server.route

import java.nio.file.{ Path => JavaPath }

import cats.effect._
import com.github.radium226.forge.server._
import com.github.radium226.http4s.fastcgi.FastCGIAppBuilder
import io.chrisdavenport.vault.Key
import org.http4s._
import org.http4s.dsl.io._

import cats.implicits._

object GitRoutes {

  object ProjectNameEndingWithGitVar {

    def unapply(segment: String): Option[(String)] = {
      if (segment.endsWith(".git")) Some(segment.dropRight(".git".length))
      else None
    }

  }

  def apply[F[_]](settings: Settings)(implicit F: Concurrent[F], contextShift: ContextShift[F]): Resource[F, HttpRoutes[F]] = {
    for {
      gitProjectRootKey <- Resource.liftF[F, Key[JavaPath]](Key.newKey[F, JavaPath])
      gitApp            <- FastCGIAppBuilder[F]
          .withParam("SCRIPT_FILENAME" -> "/usr/lib/git-core/git-http-backend")
          .withParam({ request =>
            "GIT_PROJECT_ROOT" -> request.attributes.lookup(gitProjectRootKey).map(_.toString)
          })
          .withParam("GIT_HTTP_EXPORT_ALL")
          .build

      gitRoutes          = HttpRoutes.of[F]({
        case oldRequest @ _ -> "git" /: ProjectNameEndingWithGitVar(projectName) /: _ =>
          val oldCaret = oldRequest.attributes
              .lookup(Request.Keys.PathInfoCaret)
              .getOrElse(0)

          val newCaret = s"git/${projectName}.git".length + 1
          val newRequest = oldRequest
              .withAttribute(Request.Keys.PathInfoCaret, oldCaret + newCaret)
              .withAttribute(gitProjectRootKey, settings.baseFolderPath.resolve(projectName).resolve("git"))

          gitApp.run(newRequest)
        })
    } yield gitRoutes
  }

}
