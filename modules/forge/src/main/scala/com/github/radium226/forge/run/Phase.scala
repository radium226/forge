package com.github.radium226.forge.run

sealed trait Phase

object Phase {

  case object Clean extends Phase

  case object Test extends Phase

  case object Package extends Phase

  case object Publish extends Phase

}