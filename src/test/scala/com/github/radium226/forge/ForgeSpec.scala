package com.github.radium226.forge

import java.nio.file.Path

import cats.effect._
import cats.implicits._
import com.github.radium226._
import com.github.radium226.forge.core._
import com.github.radium226.system.execute._

import scala.concurrent.duration._

class ForgeSpec extends AbstractIOSpec {

  def withForge(thunk: Forge[IO] => IO[Unit]): IO[Unit] = {
    (for {
      tempFolderPath <- testFolderResource
      forge          <- ForgeBuilder[IO](tempFolderPath).resource
    } yield forge).use(thunk)
  }

  def addAndCommit(fileName: String, fileContent: String, repoFolderPath: Path): IO[Unit] = {
    val executor = Executor[IO](workingFolderPath = Some(repoFolderPath))
    for {
      _ <- writeTextToFile(s"${fileContent}", repoFolderPath.resolve(s"${fileName}"))
      _ <- executor.execute("git", "add", s"./${fileName}").foreground
      _ <- executor.execute("git", "commit", "-m", s"Add ${fileName}").foreground
    } yield ()
  }

  def initAndClone(forge: Forge[IO], name: String): IO[Path] = {
    for {
      repoFoldePath <- initRepoWithReadMe
      _             <- IO.sleep(5 second)
      project       <- forge.projects.clone(name, repoFoldePath.toAbsolutePath.toString)
      _             <- IO.sleep(1 second)
    } yield repoFoldePath
  }

  it should "be able to build multiple projects" in withIO {
    withForge { forge =>
      (for {
        oldRepoFolderPath <- initAndClone(forge, "test1")
        _               = println(s"existingRepoFolderPath=${oldRepoFolderPath}")
        _              <- IO.sleep(5 second)
      } yield oldRepoFolderPath).flatMap({ oldRepoFolderPath =>
        during(for {
          _ <- addAndCommit("Makefile", "# Makefile", oldRepoFolderPath)
          _ <- IO.sleep(10 seconds)
          _ <- addAndCommit("build.sbt", "// Build.sbt", oldRepoFolderPath)
          _ <- IO.sleep(10 seconds)
          /*newRepoFolderPath <- initAndClone(forge, "test2")*/
          _ <- addAndCommit("pom.xml", "<!-- -->", oldRepoFolderPath)
        } yield ()) {
          forge.statuses.interruptAfter(50 seconds).compile.toList.map({ statuses =>
            println(statuses)
          })
        }
      })
    }
  }

}
