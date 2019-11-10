package com.github.radium226.forge.client

import java.nio.file.Paths

import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import com.github.radium226.forge.project.Project
import org.http4s.{Method, Request, Uri}
import org.http4s.blaze.http.HttpClient
import org.http4s.client.blaze.BlazeClientBuilder
import com.github.radium226.git._

import scala.concurrent.ExecutionContext

import com.github.radium226.forge.client.Settings

import com.github.radium226.config._

object Main extends IOApp {

  type App = ReaderT[IO, Settings, Unit]
  
  object App {

    def apply(f: Settings => IO[Unit]): App = {
      ReaderT(f)
    }
    
  }

  override def run(arguments: List[String]): IO[ExitCode] = {
    (for {
      settings <- Config[IO, Settings].parse(arguments: _*)
      _      <- app.run(settings)
    } yield ExitCode.Success).recoverWith({
      case throwable: Throwable =>
        IO(println("Something happened :(")) *> IO(throwable.printStackTrace(System.err)).as(ExitCode.Error)
    })
  }

  import Action._
  def app: App = App { settings: Settings =>
    settings.action match {
      case Help =>
        IO(println("THIS IS THE HELP! "))

      case Init(folderPath, projectName, templateProjectName) =>
        BlazeClientBuilder[IO](ExecutionContext.global).resource.use({ client =>
          for {
            // We create the project
            port      <- settings.port.liftTo[IO](new Exception("Unable to retrieve port"))
            host      <- settings.host.liftTo[IO](new Exception("Unable to retrieve host"))
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

            // We add the template if needed
            /*_         <- templateProjectName.map({ templateProjectName =>
              templateProject <- Project.lookUp()
              repo.addRemote("template", )
            }).getOrElse(IO.unit)*/
          } yield ()
        })

      case EmitHook(Some(hookName), projectName) =>
        BlazeClientBuilder[IO](ExecutionContext.global).resource.use({ client =>
          for {
            port    <- settings.port.liftTo[IO](new Exception("Unable to retrieve port"))
            host    <- settings.host.liftTo[IO](new Exception("Unable to retrieve host"))
            baseUri <- Uri.fromString(s"http://${host}:${port}").liftTo[IO]
            uri      = baseUri.withPath(s"/projects/${projectName}/hooks/${hookName}")
            _       <- client.expect[Unit](Request[IO](uri = uri, method = Method.PUT))
          } yield ()
        })

      /*case UpdateTemplate =>
        val repoFolderPath = Paths.get("")
        repo <- Repo.in[IO](repoFolderPath)
        _    <- repo.fetch("template")
        _    <- repo.rebase(
          branchName = "master",
          remoteName = Some("template")
        )*/

      case _ =>
        IO(???)
    }
  }

}
