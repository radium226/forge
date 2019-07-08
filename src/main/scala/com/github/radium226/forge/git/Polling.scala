package com.github.radium226.forge.git

import cats._
import cats.effect._
import cats.effect.concurrent._
import cats.implicits._
import com.github.radium226.system.execute._
import fs2._

import scala.concurrent.duration._
import java.nio.file.Path

import fs2.concurrent.Topic

class Polling[F[_]](topicIndex: CommitTopicIndex[F]) {

  def topic(folderPath: Path)(implicit F: Concurrent[F], timer: Timer[F]): F[CommitTopic[F]] = {
    for {
      topicsByPath <- topicIndex.take
      topic        <- (topicsByPath.get(folderPath) match {
        case None =>
          println(s"Creating topic for ${folderPath}")
          for {
            topic <- Topic[F, Option[Commit[F]]](None)
            _     <- F.start(poll(folderPath).map(Some(_)).through(topic.publish).compile.drain)
            _     <- topicIndex.put(topicsByPath + (folderPath -> topic))
          } yield topic
        case Some(topic) =>
          println(s"Topic already been created for ${folderPath}")
          for {
            _     <- topicIndex.put(topicsByPath)
          } yield topic
      })
    } yield topic
  }

  def subscribe(folderPath: Path)(implicit F: Concurrent[F], timer: Timer[F]): Stream[F, Commit[F]] = {
    Stream.eval(topic(folderPath)).flatMap(_.subscribe(1).unNone)
  }

  def poll(folderPath: Path)(implicit F: Concurrent[F], timer: Timer[F]): Stream[F, Commit[F]] = {
    val executor = Executor[F](workingFolderPath = Some(folderPath))
    Stream.awakeEvery[F](1000 milliseconds)
      .evalMap({ _ =>
        for {
          _ <- executor.execute("git", "fetch").foreground
          //_ <- executor.execute("git", "log", "--pretty=format:\"%h\"", s"master..origin/master").foreground
          lines <- executor.execute("git", "log", "--pretty=format:\"%h\"", s"master..origin/master").foreground(Keep.stdout).map(_.split("\n").map(_.trim).filter(!_.isBlank))
          _ <- executor.execute("git", "pull").foreground
        } yield lines
      })
      .map(_.toList)
      //.observe(_.evalMap({ lines => F.delay(println(s"lines=${lines}")) }))
      .flatMap({ lines => Stream.emits[F, SHA1](lines) })
      .map(Commit[F](_))
      .observe(_.evalMap({ commit => F.delay(println(s"commit=${commit}")) }))
      .chunkN(1)
      .flatMap(Stream.chunk(_))
  }

}

object Polling {

  def apply[F[_]](implicit F: Concurrent[F]): F[Polling[F]] = {
    CommitTopicIndex.empty.map(new Polling[F](_))
  }

}
