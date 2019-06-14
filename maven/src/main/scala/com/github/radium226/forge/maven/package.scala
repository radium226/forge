package com.github.radium226.forge

import java.nio.file.{Path, Paths}
import org.http4s.dsl.io.{Path => Http4sPath}

import scala.language.implicitConversions

import scopt._

package object maven {

  type Argument = String

  type Port = Int

  implicit def readPath: Read[Path] = Read.reads(Paths.get(_))

  type GroupID = String

  type ArtifactID = String

  type Version = String

  type MavenCoordinates = (GroupID, ArtifactID, Version)

  implicit def pathToFile(path: Path) = {
    path.toFile()
  }

  implicit class PimpedHttp4sPath(http4sPath: Http4sPath) {

    def toPathOption: Option[Path] = {
      http4sPath.toList.toPathOption
    }

  }

  implicit class PimpedListOfString(listOfString: List[String]) {

    def toPathOption: Option[Path] = {
      listOfString match {
        case firstSegment :: otherSegments =>
          Some(otherSegments.foldLeft(Paths.get(firstSegment)) {
            _.resolve(_)
          })

        case _ =>
          None
      }
    }

  }

}
