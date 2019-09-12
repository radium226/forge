package com.github.radium226.forge.client

import cats._
import cats.data._
import cats.effect._
import com.github.radium226.forge.config.ConfigBuilder
import cats.implicits._
import org.http4s.{Method, Request, Uri}
import org.http4s.blaze.http.HttpClient
import org.http4s.client.blaze.BlazeClientBuilder

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

      case Init(projectName) =>
        BlazeClientBuilder[IO](ExecutionContext.global).resource.use({ client =>
          for {
            port     <- config.port.liftTo[IO](new Exception("Unable to retrieve port"))
            host     <- config.host.liftTo[IO](new Exception("Unable to retrieve host"))
            baseUri  <- Uri.fromString(s"http://${host}:${port}").liftTo[IO]
            uri       = baseUri.withPath("/projects").withQueryParam("projectName", projectName)
            _        <- client.expect[Unit](Request[IO](uri = uri, method = Method.POST))
          } yield ()
        })

      case _ =>
        IO(???)
    }
  }

}
