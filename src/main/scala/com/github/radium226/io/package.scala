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

import com.google.common.io.{MoreFiles, RecursiveDeleteOption}
import fs2._
import org.http4s._

import scala.concurrent.ExecutionContext


package object io {

  object folders {

    def create[F[_]](folderPath: Path)(implicit F: Sync[F]): F[Path] = {
      F.delay(Files.createDirectories(folderPath)).as(folderPath)
    }

    def list[F[_]](folderPath: Path, recursive: Boolean)(implicit F: Sync[F]): F[List[Path]] = {
      F.delay {
        Files.list(folderPath)
            .collect(Collectors.toList())
            .asScala
            .toList
            .partition(Files.isDirectory(_))
      } flatMap { case (folderPaths, _) =>
        for {
          deeperFolderPaths <- if (recursive) folderPaths.flatTraverse(listFolders[F](_)) else F.pure(List.empty[Path])
        } yield folderPaths ++ deeperFolderPaths
      }
    }

    def deleteContent[F[_]](folderPath: Path)(implicit F: Sync[F]): F[Unit] = {
      F.delay(MoreFiles.deleteDirectoryContents(folderPath))
    }

  }

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

  def move[F[_]](originFolderPath: Path, targetFolderPath: Path)(implicit F: Sync[F]): F[Path] = {
    F.delay(Files.move(originFolderPath, targetFolderPath.resolve(originFolderPath.getFileName)))
  }

  def listFolders[F[_]](folderPath: Path)(implicit F: Sync[F]): F[List[Path]] = {
    F.delay {
      Files.list(folderPath)
        .collect(Collectors.toList())
        .asScala
        .toList
        .partition(Files.isDirectory(_))
    } flatMap { case (folderPaths, _) =>
      for {
        deeperFolderPaths <- folderPaths.flatTraverse(listFolders[F](_))
      } yield folderPaths ++ deeperFolderPaths
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
    F.delay(Files.createDirectories(filePath.getParent))
  }

  def makeFolder[F[_]](folderPath: Path)(implicit F: Sync[F]): F[Unit] = {
    F.delay(Files.createDirectories(folderPath))
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

  def upload[F[_]](filePath: Path)(implicit F: Sync[F], contextShift: ContextShift[F]): Pipe[F, Byte, Unit] = { stream =>
    for {
      _ <- Stream.eval[F, Unit](makeParentFolder[F](filePath))
      _ <- stream.through(fs2.io.file.writeAll(filePath, ExecutionContext.global))
    } yield ()
  }

  def upload[F[_]](entityBody: EntityBody[F], filePath: Path)(implicit F: Sync[F], contextShift: ContextShift[F]): F[Unit] = {
    entityBody.through(upload[F](filePath)).compile.drain
  }

  def upload[F[_]](request: Request[F], filePath: Path)(implicit F: Sync[F], contextShift: ContextShift[F]): F[Unit] = {
    upload(request.body, filePath)
  }

  def upload[F[_]](request: Request[F], suffix: String = "")(implicit F: Sync[F], contextShift: ContextShift[F]): F[Path] = {
    for {
      filePath <- F.delay(Files.createTempFile("", suffix))
      _        <- upload(request, filePath)
    } yield filePath
  }

}
