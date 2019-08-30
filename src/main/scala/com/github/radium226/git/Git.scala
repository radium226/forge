package com.github.radium226.git

import java.nio.file.Path

class Repo[F[_]](localFolderPath: Path, remoteURI: URI)

object Repo {

  // Existing repository
  def apply[F[_]](localFolderPath: Path): F[Repo[F]] = ???

  def init[F[_]](localFolderPath: Path): F[Repo[F]] = ???

  def clone[F[_]](localFolderPath: Path, remoteURI: URI): F[Repo[F]] = ???

}

