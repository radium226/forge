package com.github.radium226.forge

object Scenarios {

  def apply[F[_]]: Scenarios[F] = {
    new Scenarios[F]
  }

}

case class Scenarios[F[_]]() {



}
