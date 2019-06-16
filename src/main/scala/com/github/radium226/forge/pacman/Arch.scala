package com.github.radium226.forge.pacman

import cats.effect._

sealed abstract class Arch(val name: String)

object Arch {

  def all: List[Arch] = List(x86_64, arm)

  def byNameF[F[_]](name: String)(implicit F: Sync[F]): F[Arch] = {
    byName(name).map(F.pure).getOrElse(F.raiseError(new Exception(s"Unknown ${name} arch")))
  }

  def byName(name: String): Option[Arch] = {
    (all :+ any).find(_.name == name)
  }

  case object x86_64 extends Arch("x86_64")

  case object arm extends Arch("arm")

  case object any extends Arch("any")

}
