package com.github.radium226.forge.run

import java.nio.file.{Files, Path}
import java.util.stream.Collectors

import cats.effect._

import scala.util.matching.Regex

import scala.collection.JavaConverters._

import cats.implicits._

trait Kind {

  def fileNameRegex: Regex

  def run[F[_]](folderPath: Path)(implicit F: Concurrent[F]): Run[F]

}

object Kind {

  def enumerate[F[_]](implicit F: Sync[F]): F[List[Kind]] = {
    F.pure(List[Kind](Make, Sbt))
  }

  def detect[F[_]](folderPath: Path)(implicit F: Sync[F]): F[List[Kind]] = {
    enumerate[F].flatMap({ kinds =>
      kinds.filterA({ kind =>
        F.delay(Files.list(folderPath).collect(Collectors.toList()).asScala.toList.filter(Files.isRegularFile(_)))
          .map({ filePaths =>
            filePaths
              .map({ filePath =>
                kind.fileNameRegex.findFirstIn(filePath.getFileName.toString)
              })
              .find(_.isDefined)
              .flatten
              .isDefined
          })
      })
    })
  }

}
