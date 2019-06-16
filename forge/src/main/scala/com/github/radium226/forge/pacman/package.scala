package com.github.radium226.forge

import java.io.{InputStream, OutputStream}
import java.nio.file.Path

import cats.implicits._
import cats.effect._

import scala.concurrent.ExecutionContext
import sys.process._

package object pacman {

  type Name = String

  type Version = String

  type PkgInfo = Map[String, String]

  implicit class PimpedPkgInfo(pkgInfo: PkgInfo) {

    def read[F[_]](key: String)(implicit F: Sync[F]): F[String] = {
      pkgInfo.get(key) match {
        case Some(value) =>
          F.pure(value)

        case None =>
          F.raiseError(new Exception(s"Unable to read ${key} in ${pkgInfo} PkgInfo"))
      }
    }

  }

  object PkgInfo {

    private def open[F[_]](filePath: Path)(implicit F: Async[F]): F[InputStream] = F.async { callback =>
      val processIO = new ProcessIO(
        { _ => () },
        { inputStream => callback(inputStream.asRight[Throwable]) },
        { _ => () }
      )

      Seq("tar", "xf", filePath.toAbsolutePath.toString, ".PKGINFO", "-O").run(processIO)
    }

    def empty: PkgInfo = Map.empty

    def read[F[_]](filePath: Path)(implicit F: Async[F], contextShift: ContextShift[F]): F[PkgInfo] = {
      fs2.io.readInputStream(open(filePath), 1024, ExecutionContext.global)
        .through(fs2.text.utf8Decode)
        .through(fs2.text.lines)
        .fold(PkgInfo.empty) { case (pkginfo, line) =>
          val regex = "^(.*) = (.*)$".r("key", "value")
          line match {
            case regex(key, value) =>
              pkginfo + (key -> value)

            case _ =>
              pkginfo
          }
        }
        .compile
        .lastOrError
    }

  }

}
