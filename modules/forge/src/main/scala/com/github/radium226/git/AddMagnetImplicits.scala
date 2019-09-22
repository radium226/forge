package com.github.radium226.git

import java.nio.file.{Files, Path, Paths}
import java.util.stream.Collectors

import scala.collection.JavaConverters._
import cats.effect.Sync
import com.google.common.io.MoreFiles

trait AddMagnet[F[_]] {

  def filePathsToAdd(folderPath: Path): F[List[Path]]

}

trait AddMagnetImplicits {

  implicit def filePathToAddMagnet[F[_]](filePath: Path)(implicit F: Sync[F]): AddMagnet[F] = { _ =>
    F.pure(List(filePath))
  }

  case object All

  implicit def allToAddMagnet[F[_]](all: All.type)(implicit F: Sync[F]): AddMagnet[F] = { folderPath =>
    F.delay({
      val filePaths = Files.walk(folderPath)
        .filter(Files.isRegularFile(_))
        .collect(Collectors.toList())
        .asScala
        .toList
        .filter({ filePath =>
          !filePath.startsWith(folderPath.resolve(".git"))
        })
      println(filePaths)
      filePaths
    })
  }

}
