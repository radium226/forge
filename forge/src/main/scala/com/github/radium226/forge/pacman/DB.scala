package com.github.radium226.forge.pacman

import java.nio.file.Files

import cats.implicits._
import cats.effect._
import fs2._
import sys.process._

import com.github.radium226.io._

import scala.concurrent.ExecutionContext

case class DB(repo: Repo, arch: Arch)

object DB {

  def open[F[_]](repo: Repo, arch: Arch)(implicit F: Sync[F], contextShift: ContextShift[F]): Stream[F, Byte] = {
    open(DB(repo, arch))
  }

  def open[F[_]](db: DB)(implicit F: Sync[F], contextShift: ContextShift[F]): Stream[F, Byte] = {
    val dbFilePath = db.repo.folderPath.resolve("os").resolve(db.arch.name).resolve(s"${db.arch.name}.db.tar.gz")
    for {
      exists <- Stream.eval[F, Boolean](F.delay(Files.exists(dbFilePath)))
      _ = println(exists)
      _      <- if (exists)
                  Stream.emit[F, Unit](())
                else
                  Stream.eval(makeParentFolder(dbFilePath)) *> Stream.eval[F, Unit](F.delay(Seq("repo-add", s"${dbFilePath}") !))
      byte   <- io.file.readAll(dbFilePath, ExecutionContext.global, 1024)
    } yield byte
  }

}
