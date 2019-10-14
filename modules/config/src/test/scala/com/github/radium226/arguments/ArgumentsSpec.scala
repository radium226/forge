package com.github.radium226.arguments

import com.github.radium226.arguments._

import cats.effect._
import cats.implicits._

import shapeless._
import shapeless.syntax.singleton._

object ArgumentsSpec extends App {

  //case class Settings(maxSize: Int, name: String)

 // val settings = Settings(2, "settings")
 // val partialSettings = settings.toPartial[IO].unsafeRunSync()
 // println(partialSettings)

//  println((('toto ->> Option("toto")) :: ('tata ->> Option("tata")) :: HNil).toComplete[IO].unsafeRunSync())

  //println(('toto -> Option("toto") :: HNil).toComplete[IO])

  //val arguments = Arguments.of[IO, Settings].parse(List("--max-size=2", "--name=toto")).unsafeRunSync()
  //println(arguments)



}
