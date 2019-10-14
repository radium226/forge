package com.github.radium226.forge.config

import com.monovore.decline._
import shapeless._
import cats.implicits._
import shapeless.labelled._
import shapeless.ops.tuple.Unifier

import scala.reflect.ClassTag

object DeclinePart2 extends App {

  // Subcommands
  trait MakeSubcommand[A] {

    def apply: Opts[A]

  }

  trait MakeSubcommandPriority0 {

    implicit def generic[A, ReprA <: Coproduct](
      implicit generic: Generic.Aux[A, ReprA], ReprA: MakeSubcommand[ReprA]
    ): MakeSubcommand[A] = new MakeSubcommand[A] {

      override def apply: Opts[A] = ReprA.apply.map(generic.from(_))

    }

  }

  trait MakeSubcommandPriority1 extends MakeSubcommandPriority0 {

    implicit def cnil: MakeSubcommand[CNil] = new MakeSubcommand[CNil] {

      override def apply: Opts[CNil] = Opts.never

    }

    implicit def ccons[ReprAHead, ReprATail <: Coproduct](
      implicit reprAHeadClassTag: ClassTag[ReprAHead], makeSubcommandRepATail: MakeSubcommand[ReprATail], makeOptionReprAHead: MakeOption[ReprAHead]
    ): MakeSubcommand[ReprAHead :+: ReprATail] = new MakeSubcommand[ReprAHead :+: ReprATail] {
      override def apply: Opts[ReprAHead :+: ReprATail] = {
        val name = reprAHeadClassTag.runtimeClass.getSimpleName
        println(name)
        Opts.subcommand[ReprAHead :+: ReprATail](name = name, help = name)(makeOptionReprAHead.apply.map(Coproduct(_)))
          .orElse(makeSubcommandRepATail.apply)
          .map(_.asInstanceOf[ReprAHead :+: ReprATail]) // FIXME: Unify
      }

    }

  }

  trait MakeSubcommandPriority2 extends MakeSubcommandPriority1 {



  }

  trait MakeSubcommandImplicits extends MakeSubcommandPriority2

  object makeSubcommand  {

    def apply[A](implicit A: MakeSubcommand[A]): Opts[A] = A.apply

  }

  // Options
  trait MakeOption[A] {

    def apply: Opts[A]

  }

  object MakeOption {



    def instance[A](block: => Opts[A]): MakeOption[A] = new MakeOption[A] {

      override def apply: Opts[A] = block

    }

  }

  trait MakeOptionPriority0 {

    implicit def makeOptionLabelledGeneric[A, ReprA](
      implicit labelledGeneric: LabelledGeneric.Aux[A, ReprA], makeReprAOption: MakeOption[ReprA]
    ): MakeOption[A] = MakeOption.instance(makeReprAOption.apply.map(labelledGeneric.from(_)))

  }

  trait MakeOptionPriority1 extends MakeOptionPriority0 {

    implicit def makeOptionHNil: MakeOption[HNil] = MakeOption.instance(Opts.unit.map({ _ => HNil }))

    implicit def makeOptionHConsOptionCase[ReprAHeadKey <: Symbol, ReprAHeadValue, ReprATail <: HList](
      implicit makeOptionReprATail: MakeOption[ReprATail], reprAHeadKeyWitness: Witness.Aux[ReprAHeadKey], reprAHeadValueArgument: Argument[ReprAHeadValue]
    ): MakeOption[FieldType[ReprAHeadKey, ReprAHeadValue] :: ReprATail] = MakeOption.instance({
      val name = reprAHeadKeyWitness.value.name
      (Opts.option[ReprAHeadValue](name, name), makeOptionReprATail.apply)
          .mapN({ (reprAHead, reprATail) =>
            field[ReprAHeadKey](reprAHead) :: reprATail
          })
    })

    implicit def makeOptionHConsSubcommandCase[ReprAHeadKey <: Symbol, ReprAHeadValue, ReprATail <: HList](implicit
      makeOptionReprATail: MakeOption[ReprATail],
      reprAHeadKeyWitness: Witness.Aux[ReprAHeadKey],
      makeSubcommandReprAHeadValue: MakeSubcommand[ReprAHeadValue]
    ): MakeOption[FieldType[ReprAHeadKey, ReprAHeadValue] :: ReprATail] = MakeOption.instance({
      val name = reprAHeadKeyWitness.value.name
      (makeSubcommandReprAHeadValue.apply, makeOptionReprATail.apply)
          .mapN({ (reprAHead, reprATail) =>
            field[ReprAHeadKey](reprAHead) :: reprATail
          })
    })

  }

  trait MakeOptionPriority2 extends MakeOptionPriority1 {

  }

  trait MakeOptionImplicits extends MakeOptionPriority2

  object makeOption  {

    def apply[A](implicit A: MakeOption[A]): Opts[A] = A.apply

  }

  // Go!
  object implicits extends MakeOptionImplicits with MakeSubcommandImplicits

  import implicits._


  sealed trait Action

  case class Create(id: Int, name: String) extends Action

  case class Delete(id: Int) extends Action

  case class Config(maxSize: Int, action: Action)

  val opts = makeOption[Config]

  val command = Command(name = "This is a test", header = "This is a test")(opts)


  println(command.parse(List("--maxSize", "2", "Create", "--id=2", "--name=toto")))

}
