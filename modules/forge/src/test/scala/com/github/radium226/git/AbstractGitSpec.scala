package com.github.radium226.git

import java.nio.file.{Files, Path}

import cats.effect.{IO, Resource, Sync}
import com.google.common.io.MoreFiles
import org.scalatest.{FlatSpec, Matchers}

import cats.implicits._

abstract class AbstractGitSpec extends FlatSpec with Matchers {

  case class Env[F[_]]() {

    def tempFolderResource(implicit F: Sync[F]) = Resource.make(F.delay(Files.createTempDirectory(getClass.getSimpleName)))({ folderPath => F.delay(MoreFiles.deleteRecursively(folderPath)) })

  }

  object Env {

    def apply[F[_]] = new Env[F]

  }

  def usingEnv(f: Env[IO] => IO[Unit]): Unit = {
    f(Env[IO]).attempt.unsafeRunSync().fold(fail(_), identity)
  }

  def withTempFolder(f: Path => IO[Unit]): Unit = {
    usingEnv(_.tempFolderResource.use(f))
  }

  def withTempFolders(keys: Symbol*)(f: Map[Symbol, Path] => IO[Unit]): Unit = {
    usingEnv { env =>
      keys
        .toList
        .map({ key =>
          env.tempFolderResource
            .map({ tempFolderPath =>
              (key, tempFolderPath)
            })
        })
        .traverse(identity)
        .map(_.toMap)
        .use(f)
    }
  }

}
