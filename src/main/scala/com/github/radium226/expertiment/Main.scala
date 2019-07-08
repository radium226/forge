package com.github.radium226.expertiment

import java.rmi.registry.Registry

import cats.effect._
import cats.effect.concurrent.MVar
import cats.implicits._
import fs2._
import fs2.concurrent._

import scala.concurrent.duration._

object Main extends IOApp {

  type Letter = String

  type LetterTopic = Topic[IO, Option[Letter]]

  type LetterStream = Stream[IO, Letter]

  type Registry = MVar[IO, List[LetterTopic]]

  object Registry {

    def empty: IO[Registry] = {
      MVar.of[IO, List[LetterTopic]](List.empty)
    }

    def of(letterTopics: LetterTopic *): IO[Registry] = {
      MVar.of[IO, List[LetterTopic]](letterTopics.toList)
    }

  }

  def makeLetterTopic(letter: Letter, period: FiniteDuration): IO[LetterTopic] = {
    for {
      letterTopic <- Topic[IO, Option[Letter]](None)
      _           <- (IO(println(s"Starting ${letter} letter! ")) *> Stream.awakeEvery[IO](period).map({ _ => letter }).map(Some(_)).through(letterTopic.publish).compile.drain).start
    } yield letterTopic
  }

  def mergeLetters(registry: Registry)(implicit timer: Timer[IO]): IO[LetterStream] = {
    def go(letterQueue: Queue[IO, Letter]): IO[Unit] = {
      for {
        letterTopics <- registry.read
        letterStream  = letterTopics.map(_.subscribe(1)).reduce(_ merge _).interruptAfter(5 seconds).unNone
        _            <- letterStream.through(letterQueue.enqueue).compile.drain
        _            <- go(letterQueue)
      } yield ()
    }

    for {
      letterQueue <- Queue.unbounded[IO, Letter]
      _           <- go(letterQueue).start
    } yield letterQueue.dequeue
  }

  def updateRegistry(registry: Registry): IO[Unit] = {
    for {
      letterTopics <- registry.take
      letterTopic  <- makeLetterTopic("D", 100 milliseconds)
      _            <- registry.put(letterTopics :+ letterTopic)
    } yield ()
  }

  def peek: Pipe[IO, Letter, Unit] = {
    _.evalTap({ letter => IO(println(s"${letter}")) }).void
  }

  override def run(arguments: List[String]): IO[ExitCode] = {
    for {
      a            <- makeLetterTopic("A", 1 second)
      b            <- makeLetterTopic("B", 2 second)
      c            <- makeLetterTopic("C", 3 second)
      registry     <- Registry.of(a, b, c)
      fiber        <- (IO.sleep(10 seconds) *> updateRegistry(registry)).start
      letterStream <- mergeLetters(registry)
      letters      <- letterStream.interruptAfter(1 minute).observe(peek).compile.toList
      _             = println(s"letters=${letters}")
      _            <- fiber.join
    } yield ExitCode.Success
  }

}
