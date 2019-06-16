package com.github.radium226.forge.pacman

import java.nio.file.{Path, Paths}

import cats.implicits._
import cats.effect._

import scala.concurrent.ExecutionContext
import com.github.radium226.forge.pacman._

case class Pkg(name: Name, version: Version, arch: Arch) {

  def fileName: Path = {
    Paths.get(s"${name}-${version}-${arch.name}.pkg.tar.xz")
  }

}

object Pkg {

  def read[F[_]](filePath: Path)(implicit F: Async[F], contextShift: ContextShift[F]): F[Pkg] = {
    for {
      pkgInfo  <- PkgInfo.read[F](filePath)
      name     <- pkgInfo.read[F]("pkgname")
      version  <- pkgInfo.read[F]("pkgver")
      archName <- pkgInfo.read[F]("arch")
      _         = println(s"archName=${archName}")
      arch     <- Arch.byNameF(name)
    } yield Pkg(name, version, arch)
  }

}
