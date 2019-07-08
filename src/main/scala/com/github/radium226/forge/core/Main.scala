package com.github.radium226.forge.core

import java.nio.file.Paths

import cats._
import cats.data._
import cats.effect._
import cats.implicits._

import scala.concurrent.duration._


object Main extends IOApp {

  override def run(arguments: List[String]): IO[ExitCode] = {
    ForgeBuilder[IO](Paths.get("/tmp/projects")).resource.use({ forge =>
      (for {
        _        <- forge.emptyTrash

        projects <- forge.projects.enumerate
        _  = println(show"${projects}")

        _ <- IO.sleep(10 seconds)

        project <- forge.projects.clone("krapout", "")
        _ = println(show"${project}")


        _ <- IO.sleep(10 seconds)

        _ <- forge.projects.trash(project)

      } yield ()) *> IO.never
    })
  }

}
