package com.github.radium226.forge.client

import java.nio.file.Path

import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import cats.effect.implicits._

object Main extends IOApp {

  override def run(arguments: List[Argument]): IO[ExitCode] = {
    (for {
      config   <- OptionT.liftF[IO, Config[IO]](Config.load[IO](arguments))
      project  <- OptionT.fromOption[IO](config.project)
      _         = println(project)

      action   <- OptionT.fromOption[IO](config.action)
      _         = println(action)



    } yield ExitCode.Success).value.map(_.getOrElse(ExitCode.Error))
  }

}
