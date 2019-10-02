package com.github.radium226.forge.config

import com.monovore.decline._
import cats.implicits._
import shapeless._
import shapeless.labelled._

import scala.reflect.ClassTag

object WhatIsDecline extends App {

  trait OptsMaker[A] {

    def makeOpts: Opts[A]

  }

  trait ProvideOpts0 {

    implicit def genericOptsMaker[T, ReprT <: HList](
      implicit labelledGeneric: LabelledGeneric.Aux[T, ReprT], reprTOptsMaker: OptsMaker[ReprT]
    ): OptsMaker[T] = new OptsMaker[T] {

      override def makeOpts: Opts[T] = {
        reprTOptsMaker
          .makeOpts
          .map({ reprT =>
            labelledGeneric.from(reprT)
          })
      }
    }

    implicit def hnilOptsMaker: OptsMaker[HNil] = new OptsMaker[HNil] {

      override def makeOpts: Opts[HNil] = Opts.unit.as(HNil)

    }

    implicit def hconsOptsMaker[HeadReprKey <: Symbol, HeadReprValue, TailRepr <: HList](
      implicit headReprKeyWitness: Witness.Aux[HeadReprKey], tailReprOptsMaker: OptsMaker[TailRepr], headReprValueArgument: Argument[HeadReprValue]
    ): OptsMaker[FieldType[HeadReprKey, HeadReprValue] :: TailRepr] = new OptsMaker[FieldType[HeadReprKey, HeadReprValue] :: TailRepr] {

      override def makeOpts: Opts[FieldType[HeadReprKey, HeadReprValue] :: TailRepr] = {
        println(s"We are here for ${headReprKeyWitness.value.name}")
        (Opts.option[HeadReprValue](headReprKeyWitness.value.name, help = headReprKeyWitness.value.name), tailReprOptsMaker.makeOpts)
            .mapN({ (headReprValue, tailReprValue) =>
              println(s"headReprValue=${headReprValue}")
              field[HeadReprKey](headReprValue) :: tailReprValue
            })
      }

    }

  }

  object OptsMaker extends ProvideOpts0 {

    implicit def actionOptsMaker[A <: Action, ReprA <: HList](
      implicit classTagA: ClassTag[A], labelledGeneric: LabelledGeneric.Aux[A, ReprA], optsMakerReprA: Lazy[OptsMaker[ReprA]]
    ): OptsMaker[A] = new OptsMaker[A] {

      override implicit def makeOpts: Opts[A] = {
        val name = classTagA.runtimeClass.getSimpleName
        println(s"Now, we are here for the ${classTagA.runtimeClass.getSimpleName} action")
        Opts.subcommand(name, help = name) {
          optsMakerReprA.value.makeOpts.map({ reprA => labelledGeneric.from(reprA) })
        }
      }

    }

    def makeOpts[T](implicit optsMakerT: OptsMaker[T]): Opts[T] = optsMakerT.makeOpts

  }

  import OptsMaker._

  sealed trait Action

  case class Create(id: Int) extends Action

  case class Delete(id: Int, force: Boolean) extends Action

  case class Config(dryRun: Boolean, action: Action)

  val dryRun = Opts.flag("dry-run", "Dry Run").map({ _ => true }).withDefault(false)

  val id = Opts.option[Int]("id", "ID")

  val force = Opts.flag("force", "Force").map({ _ => true }).withDefault(false)

  val create = Opts.subcommand[Action]("create", "Create")(id.map(Create.apply))

  val delete = Opts.subcommand[Action]("delete", "Delete")((id, force).mapN(Delete.apply))

  val config = (dryRun, create orElse delete).mapN(Config.apply)

  val command = Command("Yay", header = "Yay")(config)

  println(command.parse(List("--dry-run", "delete", "--id=12")))

  println(Generic[Action])

}
