package com.github.radium226.config

import com.monovore.decline.Opts
import cats.implicits._
import com.monovore.decline._
import shapeless._
import shapeless.labelled._
import shapeless.syntax.singleton._

class OptionAndSubcommandSpec extends AbstractConfigSpec {

  sealed trait Action

  @help("Yay, delete! ")
  case class Delete(@help("ID") id: Int) extends Action

  @help("Yay, Show! ")
  case class Show(@help("Category") category: Option[String]) extends Action

  @help("Yay, Crate! ")
  case class Create(@help("Name") name: String) extends Action

  it should "be able to produce option from Delete, Show and Create" in {
    println(makeOption[Delete])
    println(makeOption[Show])
    println(makeOption[Create])

    val result = makeOption[Create]
    println(result
      .map({ opts =>
        Command(name = "test", header = "test")(opts)
      })
      .map({ command =>
        command.showHelp
      }))
  }

  it should "be able to produce subcommand from CNil" in {
    println(makeSubcommand[CNil])
  }

  it should "be able to produce subcommand from Action" in {
    val result = makeSubcommand[Action]
    println(s"result=${result}")

    //printHelp(result)
  }

  case class Settings(id: String, action: Action)

  //it should "not work with option" in {
    //println(makeSubcommand[Option[String]])
  //}

  it should "be able to produce option with nested Action" in {
    printHelp(makeOption[Settings])
  }

  case class Fucker(name: Option[String])

  it should "not be fucked by Option[String]" in {
    //println(makeOption[Fucker])
    //printHelp(makeSubcommand[Fucker])
  }

  it should "do... Well IDK, with Option" in {
    //println(makeSubcommand[Option[String]]("test"))

  }

  def printHelp(result: Result[Opts[_]]): Unit = {
    println(result
        .map({ opts =>
          Command(name = "test", header = "test")(opts)
        })
        .map({ command =>
          command.showHelp
        }))
  }

}
