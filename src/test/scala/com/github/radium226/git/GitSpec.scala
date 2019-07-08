package com.github.radium226.git

import java.nio.file.Paths

import com.github.radium226.AbstractSpec
import cats._
import cats.effect._
import cats.effect.internals.IOAppPlatform
import cats.implicits._

import scala.concurrent.duration._

import scala.concurrent.ExecutionContext


class GitSpec extends AbstractSpec[IO] {

  protected implicit def contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  protected implicit def timer: Timer[IO] = IO.timer(ExecutionContext.global)

  it should "be able to poll for commits" in withRepos({ (remoteRepo, localRepo) =>
    whenThen({
      for {
        _ <- writeTextToFile("class Main extends App {}", remoteRepo.folderPath.resolve("Main.scala"))
        _ <- timer.sleep(1 second)
        _ <- remoteRepo.add(Paths.get("./Main.scala"))
        _ <- timer.sleep(1 second)
        _ <- remoteRepo.commit("Add application")
      } yield()
    })(localRepo.commits.interruptAfter(10 seconds).compile.toList.map({ commits =>
      println(s"commits=${commits}")
      commits.size should be(1)
    }))
  }).unsafeRunSync()

}
