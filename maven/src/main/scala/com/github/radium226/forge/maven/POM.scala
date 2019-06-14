package com.github.radium226.forge.maven

import cats._
import cats.implicits._
import cats.effect._
import java.nio.file.Path

import com.github.radium226.io
import com.lucidchart.open.xtract._
import com.lucidchart.open.xtract.XmlReader._

import scala.xml.XML


case class POM(groupID: GroupID, artifactID: ArtifactID, version: Version)

object POM {

  implicit val reader: XmlReader[POM] = (
      (__ \ "groupId").read[GroupID],
      (__ \ "artifactId").read[ArtifactID],
      (__ \ "version").read[Version]
  ).mapN(apply _)

  def read[F[_]](filePath: Path)(implicit F: Sync[F]): F[POM] = {
    F.delay(XML.loadFile(filePath)).map({ t => println(t) ; t }).map(XmlReader.of[POM].read(_)).flatMap({
      case ParseSuccess(pom) =>
        F.pure(pom)

      case ParseFailure(errors) =>
        errors.foreach(println)
        F.raiseError(new Exception(s"Unable to read ${filePath} POM"))
    })
  }

  def locateFiles[F[_]](folderPath: Path)(implicit F: Sync[F]): F[List[Path]] = {
    io.locateFiles(folderPath, "\\.pom$".r)
  }

}
