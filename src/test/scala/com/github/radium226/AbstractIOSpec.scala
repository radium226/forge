package com.github.radium226

import java.util.concurrent.Executors

import cats.effect._

import scala.concurrent.ExecutionContext

abstract class AbstractIOSpec extends AbstractSpec[IO] {

  implicit def contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(50)))

  implicit def timer: Timer[IO] = IO.timer(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(50)))

  def withIO(thunk: => IO[Unit]): Unit = {
    thunk.unsafeRunSync()
  }

}
