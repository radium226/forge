package com.github.radium226.forge

import java.nio.file.Path

import cats._
import cats.effect._
import cats.effect.concurrent._
import cats.implicits._
import fs2.concurrent._

package object git {

  type URI = String

  type SHA1 = String

  type CommitTopic[F[_]] = Topic[F, Option[Commit[F]]]

  type CommitTopicIndex[F[_]] = MVar[F, Map[Path, CommitTopic[F]]]

  object CommitTopicIndex {

    def empty[F[_]](implicit F: Concurrent[F]): F[CommitTopicIndex[F]] = {
      MVar.of[F, Map[Path, CommitTopic[F]]](Map.empty[Path, CommitTopic[F]])
    }

  }

  implicit val showPath: Show[Path] = Show.fromToString

  implicit def showRepo[F[_]]: Show[Repo[F]] = _.folderPath.show

}
