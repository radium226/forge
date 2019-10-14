package com.github.radium226.config

import shapeless._
import shapeless.labelled._
import shapeless.syntax.singleton._

import cats._
import cats.implicits._

import com.monovore.decline._


class ConfigSpec extends AbstractConfigSpec with AllInstances with AllSyntax {

  case class Person(name: String, age: Int)

  it should "be able to produce partial instances" in {
    println(s"2.partial=${2.partial}")

    val hlist = 'toto ->> "toto" :: 'two ->> 2 :: HNil
    println(s"hlist.partial=${hlist.partial}")

    val adrien = Person("Adrien", 32)
    println(s"adrien.partial=${adrien.partial}")
  }

  it should "also be able to produce complete instances" in {
    println(s"2.some.complete=${2.some.complete}")

    val yes = 'toto ->> "toto".some :: 'two ->> 2.some :: HNil
    println(s"yes.complete=${yes.complete}")

    val no = 'toto ->> "toto".some :: 'none ->> none[Int] :: HNil
    println(s"no.complete=${no.complete}")
  }

  it should "be able to use complete and partial chained" in {
    val two = 2.partial.flatMap(_.complete)
    println(s"two=${two}")

    val adrien = Person("Adrien", 32)
    println(s"adrien.partial.complete=${adrien.partial.flatMap(_.complete)}")
  }

  it should "be able to parse configs" in {
    @header(
      """
        |The main goal here is to be cool!
        |""".stripMargin)
    case class Settings(
      @help("Minimum size") minSize: Int,
      maxSize: Int,
      defaultSize: Int = -1
    )

    println(Config.of[Settings].parse(List("--default-size=3"), "min-size: 2", "max-size: 4"))

    println(Config.of[Settings].parse(List.empty, "min-size: 2", "max-size: 4"))


  }

}
