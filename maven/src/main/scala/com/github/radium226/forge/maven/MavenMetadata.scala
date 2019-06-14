package com.github.radium226.forge.maven

import java.nio.file.Path
import java.time.{LocalDate, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

import scala.xml._
import cats._
import cats.implicits._
import cats.effect._
import com.github.radium226.io._


case class MavenMetadata(groupID: GroupID, artifactID: ArtifactID, versions: List[Version], latestVersion: Version, releaseVersion: Version, lastUpdatedDateTime: LocalDateTime)

object MavenMetadata {

  private def inferVersion[F[_]](artifactFolderPath: Path, flag: String)(implicit F: Sync[F]): F[Version] = {
    locateFiles(artifactFolderPath, s"^${Pattern.quote(flag)}$$".r).map(_.headOption).flatMap({
      case Some(flagFilePath) =>
        F.pure(flagFilePath.getFileName.toString)

      case None =>
        F.raiseError(new Exception(s"Unable to locate ${flag} flag"))
    })
  }

  private def inferVersions[F[_]](folderPath: Path)(implicit F: Sync[F]): F[List[Version]] = {
    POM.locateFiles(folderPath).flatMap(_.traverse(POM.read(_))).map(_.map(_.version))
  }

  private def inferGroupIDAndArtifactID[F[_]](folderPath: Path)(implicit F: Sync[F]): F[(GroupID, ArtifactID)] = {
    POM.locateFiles(folderPath).map(_.headOption).flatMap({
      case Some(pomFilePath) =>
        POM.read(pomFilePath).map({ pom => (pom.groupID, pom.artifactID)})

      case None =>
        F.raiseError(new Exception(s"Unable to infer groupID and artifactID in ${folderPath}"))
    })
  }

  private def inferLastUpdatedDateTime[F[_]](folderPath: Path)(implicit F: Sync[F]): F[LocalDateTime] = {
    POM.locateFiles(folderPath)
      .flatMap(_.traverse(fileAttributes(_)))
      .map(_.map(_.creationTime()).max)
      .map(_.toInstant)
      .map(LocalDateTime.ofInstant(_, ZoneOffset.UTC))
  }

  def generate[F[_]](folderPath: Path)(implicit F: Sync[F]): F[MavenMetadata] = {
    for {
      latestVersion <- inferVersion[F](folderPath, "LATEST")
      releaseVersion <- inferVersion[F](folderPath, "RELEASE")
      groupIDAndArtifactID <- inferGroupIDAndArtifactID[F](folderPath)
      (groupID, artifactID) = groupIDAndArtifactID
      versions <- inferVersions[F](folderPath)
      lastUpdatedDateTime <- inferLastUpdatedDateTime(folderPath)
    } yield MavenMetadata(groupID, artifactID, versions, latestVersion, releaseVersion, lastUpdatedDateTime)
  }

  def write[F[_]](mavenMetadata: MavenMetadata)(implicit F: Sync[F]): F[NodeSeq] = F.delay {
    <metadata>
      <groupId>{ mavenMetadata.groupID }</groupId>
      <artifactId>{ mavenMetadata.artifactID }</artifactId>
      <versioning>
        <latest>{ mavenMetadata.latestVersion }</latest>
        <release>{ mavenMetadata.releaseVersion }</release>
        <versions>
          { mavenMetadata.versions.map({ version => <version>{ version }</version> }) }
        </versions>
        <lastUpdated>{ DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(mavenMetadata.lastUpdatedDateTime) } </lastUpdated>
      </versioning>
    </metadata>
  }

}
