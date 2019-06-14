package com.github.radium226

import cats._
import cats.implicits._
import cats.effect._
import java.nio.file.{Files, Path}
import java.time.{Instant, LocalDateTime}
import java.util.stream.Collectors

import scala.util.matching.Regex
import scala.collection.JavaConverters._
import java.nio.file.Files
import java.nio.file.attribute.{BasicFileAttributes, FileTime}
import java.io.FileOutputStream


package object io {

  def listFiles[F[_]](folderPath: Path)(implicit F: Sync[F]): F[List[Path]] = {
    F.delay {
      Files.list(folderPath)
        .collect(Collectors.toList())
        .asScala
        .toList
        .partition(Files.isDirectory(_))
    } flatMap { case (folderPaths, filePaths) =>
      for {
        filePaths       <- F.pure(filePaths)
        deeperFilePaths <- folderPaths.flatTraverse(listFiles[F](_))
      } yield filePaths ++ deeperFilePaths
    }
  }

  def locateFiles[F[_]](folderPath: Path, regex: Regex)(implicit F: Sync[F]): F[List[Path]] = {
    listFiles(folderPath)
      .map({ t =>
        println(s"t=${t}")
        t
      })
      .map(_.filter({ filePath =>
        regex.findFirstMatchIn(filePath.getFileName.toString).isDefined
      }))
  }

  def makeParentFolder[F[_]](filePath: Path)(implicit F: Sync[F]): F[Unit] = {
    F.delay({
      println(s"filePath.getParent=${filePath.getParent}")
      Files.createDirectories(filePath.getParent)
    })
  }

  def fileAttributes[F[_]](filePath: Path)(implicit F: Sync[F]): F[BasicFileAttributes] = {
    F.delay(Files.readAttributes(filePath, classOf[BasicFileAttributes]))
  }

  def touch[F[_]](filePath: Path)(implicit F: Sync[F]): F[Unit] = F.delay({
    if (!Files.exists(filePath)) {
      Files.newOutputStream(filePath).close()
    } else {
      Files.setLastModifiedTime(filePath, FileTime.from(Instant.now()))
    }
  })

}
