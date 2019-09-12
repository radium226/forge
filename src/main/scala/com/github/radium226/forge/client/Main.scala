package com.github.radium226.forge.client

import cats._
import cats.data._
import cats.effect._
import com.github.radium226.forge.config.ConfigBuilder
import cats.implicits._
import org.http4s.{Method, Request, Uri}
import org.http4s.blaze.http.HttpClient
import org.http4s.client.blaze.BlazeClientBuilder
import com.github.radium226.git._

import scala.concurrent.ExecutionContext

object Main extends IOApp with ConfigSupport {

  type App = ReaderT[IO, Config[IO], Unit]
  
  object App {

    def apply(f: Config[IO] => IO[Unit]): App = {
      ReaderT(f)
    }
    
  }

  override def run(arguments: List[String]): IO[ExitCode] = {
    (for {
      config <- ConfigBuilder.build[IO, Config[IO]](arguments)
      _      <- app.run(config)
    } yield ExitCode.Success).recoverWith({
      case throwable: Throwable =>
        IO(println("Something happened :(")) *> IO(throwable.printStackTrace(System.err)).as(ExitCode.Error)
    })
  }

  import Action._
  def app: App = App { config: Config[IO] =>
    config.action match {
      case Help =>
        IO(println("THIS IS THE HELP! "))

      case Init(folderPath, projectName) =>
        BlazeClientBuilder[IO](ExecutionContext.global).resource.use({ client =>
          for {
            // We create the project
            port      <- config.port.liftTo[IO](new Exception("Unable to retrieve port"))
            host      <- config.host.liftTo[IO](new Exception("Unable to retrieve host"))
            baseUri   <- Uri.fromString(s"http://${host}:${port}").liftTo[IO]
            uri        = baseUri.withPath("/projects").withQueryParam("projectName", projectName)
            _         <- client.expect[Unit](Request[IO](uri = uri, method = Method.POST))

            // We add the existing sources
            remoteUri  = baseUri.withPath(s"/git/${projectName}.git")
            repo      <- Repo.init[IO](folderPath, bare = false, shared = false)
            _         <- repo.addRemote("origin", remoteUri)
            _         <- repo.git("add", "--all")
            _         <- repo.git("commit", "--message", s"Init the ${projectName} project! ")
            _         <- repo.git("push", "--set-upstream", "origin", "master")
          } yield ()
        })

      case _ =>
        IO(???)
    }
  }

}
