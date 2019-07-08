package com.github.radium226

import java.nio.charset.StandardCharsets
import java.nio.file._

import com.github.radium226.forge.git._
import org.scalatest._
import cats.implicits._
import cats.effect._
import com.github.radium226.system.execute.Executor
import com.google.common.io.{MoreFiles, RecursiveDeleteOption}
import org.scalatest.concurrent.Eventually

import scala.concurrent.duration._

class AbstractSpec[F[_]] extends FlatSpec with Matchers {

  def info[F[_]](message: String)(implicit F: Sync[F]): F[Unit] = {
    F.delay(println(message))
  }

  def testFolderResource(implicit F: Sync[F]): Resource[F, Path] = {
    Resource.make[F, Path](createTempFolder)({ folderPath => F.delay(MoreFiles.deleteRecursively(folderPath))})
  }

  def createTempFolder(implicit F: Sync[F]): F[Path] = {
    F.delay(Files.createTempDirectory(getClass.getSimpleName))
  }

  def createFolder(parentFolderPath: Path, folderName: String)(implicit F: Sync[F]): F[Path] = {
    val folderPath = parentFolderPath.resolve(folderName)
    F.delay(Files.createDirectories(folderPath)).as(folderPath)
  }

  def writeTextToFile(text: String, filePath: Path)(implicit F: Sync[F]): F[Unit] = F.delay({
    Files.write(filePath, text.getBytes(StandardCharsets.UTF_8))
  })

  def initRepoWithReadMe(implicit F: Sync[F]): F[Path] = {
    for {
      // Init
      remoteFolderPath <- createTempFolder
      remoteExecutor    = Executor[F](workingFolderPath = Some(remoteFolderPath))
      _                <- remoteExecutor.execute("git", "init", ".").foreground
      _                <- writeTextToFile("This is the README", remoteFolderPath.resolve("README.md"))
      _                <- remoteExecutor.execute("git", "add", "./README.md").foreground
      _                <- remoteExecutor.execute("git", "commit", "-m", "Init repo! ").foreground
    } yield remoteFolderPath
  }

  def initRemoteRepo(remoteFolderPath: Path)(implicit F: Sync[F]): F[Unit] = {
    val remoteExecutor = Executor[F](workingFolderPath = Some(remoteFolderPath))
    for {
      // Init
      _ <- remoteExecutor.execute("git", "init", ".").foreground
      _ <- writeTextToFile("This is the README", remoteFolderPath.resolve("README.md"))
      _ <- remoteExecutor.execute("git", "add", "./README.md").foreground
      _ <- remoteExecutor.execute("git", "commit", "-m", "Init repo! ").foreground
    } yield ()
  }

  def cloneRemoteRepoToLocalRepo(remoteRepoFolderPath: Path, localRepoFolderPath: Path)(implicit F: Sync[F]): F[Unit] = {
    val localExecutor = Executor[F](workingFolderPath = Some(localRepoFolderPath))
    localExecutor.execute("git", "clone", remoteRepoFolderPath.toAbsolutePath.toString, ".").foreground.void
  }

  def withRepos(thunk: (Repo[F], Repo[F]) => F[Unit])(implicit F: Concurrent[F], timer: Timer[F]): F[Unit] = {
    testFolderResource.use({ testFolderPath =>
      for {
        polling             <- Polling[F]
        remoteFolderPath <- createFolder(testFolderPath, "remote")
        localFolderPath  <- createFolder(testFolderPath, "local")
        _                <- initRemoteRepo(remoteFolderPath)
        localRepo        <- Git[F](localFolderPath, polling).cloneRepo(remoteFolderPath.toAbsolutePath.toString)
        remoteRepo       <- Git[F](remoteFolderPath, polling).useRepo
        _                <- Executor.execute[F]("ls", "-alrth", localFolderPath.toString).foreground
        _                <- thunk(remoteRepo, localRepo)
      } yield ()
    })
  }

  def whenThen(whenThunk: => F[Unit])(thenThunk: => F[Unit])(implicit F: ConcurrentEffect[F]): F[Unit] = {
    for {
      whenFiber <- F.start(whenThunk)
      thenFiber <- F.start(thenThunk)
      _         <- thenFiber.join
      t         <- whenFiber.cancel
    } yield ()
  }

  def during(eventsF: => F[Unit])(testsF: => F[Unit])(implicit F: ConcurrentEffect[F]): F[Unit] = {
    for {
      events <- F.start(eventsF)
      tests  <- F.start(testsF)
      _      <- tests.join
      _      <- events.cancel
    } yield ()
  }

  def eventually(thunk: => F[Unit]) (implicit F: Sync[F], timer: Timer[F]): F[Unit] = {
    def retryWithBackoff(thunk: => F[Unit], initialDelay: FiniteDuration, maxRetries: Int): F[Unit] = {
      thunk.handleErrorWith { error =>
        if (maxRetries > 0)
          timer.sleep(initialDelay) *> retryWithBackoff(thunk, initialDelay * 2, maxRetries - 1)
        else
          F.raiseError(error)
      }
    }

    retryWithBackoff(thunk, 1 second, 5)
  }

  def withTempFolder[T](thunk: Path => F[T])(implicit F: Sync[F]): F[T] = {
    testFolderResource.use(thunk)
  }

}
