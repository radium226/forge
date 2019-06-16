package com.github.radium226.forge.pacman

import org.http4s.dsl.io.{Path => Http4sPath}

object ArchVar {

  def unapply(segment: String): Option[Arch] = {
    println(s"segment=${segment}")
    Arch.byName(segment)

  }

}
