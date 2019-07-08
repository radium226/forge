package com.github.radium226.forge

import cats._
import cats.effect.concurrent._
import cats.implicits._
import com.github.radium226.forge.git._
import fs2.concurrent._


package object core {

  type StatusTopic[F[_]] = Topic[F, Option[Status[F]]]

  type Refresh[F[_]] = MVar[F, SignallingRef[F, Boolean]]

  implicit def showProject[F[_]]: Show[Project[F]] = { project =>
    show"Project(${project.repo})"
  }

}
