package com.github.radium226.git

import java.nio.file.{Files, Paths}

import cats.effect._
import com.github.radium226.git.implicits._
import com.google.common.io.MoreFiles

class GitSpec extends AbstractGitSpec {

  it should "be able to init a bare repo and clone it" in withTempFolders('remoteRepo, 'firstLocalRepo, 'secondLocalRepo) { tempFolderPaths =>
    val remoteRepoFolderPath      = tempFolderPaths('remoteRepo)
    val firstLocalRepoFolderPath  = tempFolderPaths('firstLocalRepo)
    val secondLocalRepoFolderPath = tempFolderPaths('secondLocalRepo)

    for {
      _               <- Repo.init[IO](remoteRepoFolderPath, bare = true)

      firstLocalRepo  <- Repo.clone[IO](remoteRepoFolderPath.toString, firstLocalRepoFolderPath)
      _               <- IO(MoreFiles.touch(firstLocalRepoFolderPath.resolve("README.md")))
      _               <- firstLocalRepo.add(All)
      _               <- firstLocalRepo.commit("Add README.md")
      _               <- firstLocalRepo.push("origin")

      secondLocalRepo <- Repo.clone[IO](remoteRepoFolderPath.toString, secondLocalRepoFolderPath)
      _               <- IO(Files.exists(secondLocalRepoFolderPath.resolve("README.md")) shouldBe true)

      _               <- IO(MoreFiles.touch(secondLocalRepoFolderPath.resolve("TODO.md")))
      _               <- secondLocalRepo.add(Paths.get("TODO.md"))
      _               <- secondLocalRepo.commit("Add TODO.md")
      _               <- secondLocalRepo.push("origin")

      _               <- firstLocalRepo.pull("origin")
      _               <- IO(Files.exists(firstLocalRepoFolderPath.resolve("TODO.md")) shouldBe true)
    } yield ()
  }

}
