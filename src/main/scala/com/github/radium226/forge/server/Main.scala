package com.github.radium226.forge.server

import java.nio.file.{Files, Path => JavaPath}

import cats.data.ReaderT
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
import cats.implicits._
import com.github.radium226.forge.config.ConfigBuilder
import com.github.radium226.forge.project.Project
import org.http4s.implicits._

object Main extends IOApp with ConfigSupport {

  object ProjectNameQueryParamMatcher extends QueryParamDecoderMatcher[String]("projectName")

  object ProjectNameEndingWithGitVar {

    def unapply(segment: String): Option[(String)] = {
      if (segment.endsWith(".git")) Some(segment.dropRight(".git".length))
      else None
    }

  }

  def serve(config: Config[IO], httpApp: HttpApp[IO]): Resource[IO, Server[IO]] = {
    for {
      port   <- Resource.liftF[IO, Int](config.port.liftTo[IO](new Exception("Unable to retreive port")))
      server <- BlazeServerBuilder[IO]
        .withHttpApp(httpApp)
        .bindHttp(port, "0.0.0.0")
        .resource
    } yield server

  }

  def makeRoutes(config: Config[IO]): Resource[IO, HttpRoutes[IO]] = {
    for {
      baseFolderPath    <- Resource.liftF(config.baseFolderPath.liftTo[IO](new Exception("Unable to retreive baseFolderPath")))
      gitProjectRootKey <- Resource.liftF[IO, Key[JavaPath]](Key.newKey[IO, JavaPath])
      gitApp            <- FastCGIAppBuilder[IO]
        .withParam("SCRIPT_FILENAME" -> "/usr/lib/git-core/git-http-backend")
        .withParam({ request =>
          "GIT_PROJECT_ROOT" -> request.attributes.lookup(gitProjectRootKey).map(_.toString)
        })
        .withParam("GIT_HTTP_EXPORT_ALL")
        .build

      gitRoutes          = HttpRoutes.of[IO]({
        case oldRequest @ _ -> "git" /: ProjectNameEndingWithGitVar(projectName) /: _ =>
          val oldCaret = oldRequest.attributes
            .lookup(Request.Keys.PathInfoCaret)
            .getOrElse(0)

          val newCaret = s"git/${projectName}.git".length + 1
          val newRequest = oldRequest
            .withAttribute(Request.Keys.PathInfoCaret, oldCaret + newCaret)
            .withAttribute(gitProjectRootKey, baseFolderPath.resolve(projectName).resolve("git"))

          gitApp.run(newRequest)
      })

      projectRoutes      = HttpRoutes.of[IO]({
        case POST -> Root / "projects" :? ProjectNameQueryParamMatcher(projectName) =>
          for {
            project  <- Project.init[IO](baseFolderPath, projectName)
            response <- Ok()
          } yield response

        case DELETE -> Root / "projects" / projectName =>
          for {
            project  <- Project.lookUp[IO](baseFolderPath, projectName)
            _        <- project.trash
            response <- Ok()
          } yield response
      })
    } yield gitRoutes <+> projectRoutes
  }

  override def run(arguments: List[String]): IO[ExitCode] = {
   (for {
      config         <- ConfigBuilder.resource[IO, Config[IO]](arguments)
      httpRoutes     <- makeRoutes(config)
      httpApp         = httpRoutes.orNotFound
      _              <- serve(config, httpApp)
    } yield ()).use({ _ => IO.never })
  }

}
