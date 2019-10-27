package com.github.radium226.config.tests

import com.google.common.base.CaseFormat
import com.monovore.decline._

import shapeless._
import shapeless.labelled._
import shapeless.syntax.singleton._

import cats.implicits._

import scala.reflect._


trait Service {

  def inferOptionName(key: Symbol): String = {
    CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, key.name)
  }

  def inferSubcommandName(runtimeClass: Class[_]): String = {
    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, runtimeClass.getSimpleName)
  }

}

trait ServiceInstances {

  implicit def default: Service = new Service {}

}

object Service extends ServiceInstances

trait MakeOption[A] {

  def apply(): Opts[A]

}

trait MakeOptionLowestPriorityInstances {

  implicit def makeOptionForSubcommandOfAny[K <: Symbol, A](implicit
    makeSubcommandForA: MakeSubcommand.Aux[A],
    witnessForK: Witness.Aux[K],
    service: Service,
    classTagForA: ClassTag[A]
  ): MakeOption.Aux[FieldType[K, A]] = MakeOption.instance({
    debug(s"makeOptionForSubcommandOfAny[${witnessForK.value}, ${classTagForA.runtimeClass.getSimpleName}]")
    makeSubcommandForA().map(field[K](_))
  })

}

trait MakeOptionLowPriorityInstances extends MakeOptionLowestPriorityInstances {

  implicit def makeOptionForAny[K <: Symbol, A](implicit
    argumentForA: Argument[A],
    witnessForK: Witness.Aux[K],
    service: Service,
    classTagForA: ClassTag[A]
  ): MakeOption.Aux[FieldType[K, A]] = MakeOption.instance({
    val name = service.inferOptionName(witnessForK.value)
    debug(s"makeOptionForAny[${witnessForK.value}, ${classTagForA.runtimeClass.getSimpleName}]")
    Opts.option[A](name, "No help! ").map(field[K](_))
  })

}

trait MakeOptionInstances extends MakeOptionLowPriorityInstances {

  implicit def makeOptionForHNil: MakeOption.Aux[HNil] = MakeOption.constant(Opts.unit.as(HNil))

  implicit def makeOptionForHCons[K <: Symbol, H, T <: HList](implicit
    makeOptionForH: MakeOption.Aux[H],
    makeOptionForT: MakeOption.Aux[T]
  ): MakeOption.Aux[H :: T] = MakeOption.instance({
    (makeOptionForH(), makeOptionForT()).mapN(_ :: _)
  })

  implicit def makeOptionForLabelledGeneric[A, ReprOfA <: HList](implicit
    labelledGeneric: LabelledGeneric.Aux[A, ReprOfA],
    makeOptionForReprOfA: MakeOption.Aux[ReprOfA]
  ): MakeOption.Aux[A] = MakeOption.instance({
    makeOptionForReprOfA().map(labelledGeneric.from(_))
  })

  implicit def makeOptionForOption[K <: Symbol, A](implicit
    makeOptionForA: MakeOption.Aux[FieldType[K, A]],
    witnessForK: Witness.Aux[K]
  ): MakeOption.Aux[FieldType[K, Option[A]]] = MakeOption.instance({
    makeOptionForA().orNone.map(field[K](_))
  })

}

trait MakeOptionSyntax {

  def makeOption[A](implicit makeOptionForA: MakeOption[A]): Opts[A] = {
    makeOptionForA.apply()
  }

}

object MakeOption {

  type Aux[A] = MakeOption[A]

  def instance[A](f: => Opts[A]): MakeOption.Aux[A] = new MakeOption[A] {

    def apply() = f

  }

  def constant[A](a: Opts[A]): MakeOption.Aux[A] = MakeOption.instance(a)

}

trait MakeSubcommand[A] {

  def apply(): Opts[A]

  def instance[A](f: => Opts[A]): MakeSubcommand.Aux[A] = new MakeSubcommand[A] {

    def apply() = f

  }

}

object MakeSubcommand {

  type Aux[A] = MakeSubcommand[A]

  def instance[A](f: => Opts[A]): MakeSubcommand.Aux[A] = new MakeSubcommand[A] {

    def apply(): Opts[A] = f

  }

  def constant[A](a: Opts[A]): MakeSubcommand.Aux[A] = MakeSubcommand.instance(a)

}

trait MakeSubcommandLowPriorityInstances {

  implicit def makeSubcommandForAny[A](implicit
    makeOptionForA: Lazy[MakeOption[A]],
    classTagForA: ClassTag[A],
    service: Service
  ): MakeSubcommand.Aux[A] = MakeSubcommand.instance({
    val name = service.inferSubcommandName(classTagForA.runtimeClass)
    val opts = makeOptionForA.value()
    Opts.subcommand[A](name, "No help! ")(opts)
  })

}

trait MakeSubcommandInstances extends MakeSubcommandLowPriorityInstances {

  implicit def makeSubcommandForCNil[K <: Symbol]: MakeSubcommand.Aux[CNil] = MakeSubcommand.constant(Opts.never)

  implicit def makeSubcommandForCCons[H, T <: Coproduct](implicit
    makeSubcommandForH: MakeSubcommand.Aux[H],
    makeSubcommandForT: MakeSubcommand.Aux[T],
    classTagForH: ClassTag[H]
  ): MakeSubcommand.Aux[H :+: T] = MakeSubcommand.instance({
    debug(s"makeSubcommandForCCons[${classTagForH.runtimeClass.getSimpleName}, ...]")
    val optsForH = makeSubcommandForH()
    val optsForT = makeSubcommandForT()
    optsForH.orElse(optsForT)
        .map(_.asInstanceOf[H :+: T])
  })

  implicit def makeSubcommandForGeneric[A, ReprOfA <: Coproduct](implicit
    generic: Generic.Aux[A, ReprOfA],
    makeSubcommandForReprOfA: MakeSubcommand[ReprOfA],
    classTagForA: ClassTag[A]
  ): MakeSubcommand.Aux[A] = MakeSubcommand.instance({
    debug(s"makeSubcommandForGeneric[${classTagForA.runtimeClass.getSimpleName}, ...]")
    makeSubcommandForReprOfA()
        .map(generic.from(_))
  })

}

trait MakeSubcommandSyntax {

  def makeSubcommand[A](implicit makeSubcommandForA: MakeSubcommand.Aux[A]): Opts[A] = makeSubcommandForA()

}

object ArgumentsTests extends App with MakeOptionInstances with MakeSubcommandInstances with MakeOptionSyntax with MakeSubcommandSyntax {

  sealed trait Action
  case class Create(name: String, tag: Option[String]) extends Action
  case class Delete(id: Int) extends Action

  case class SettingsWithoutAction(minSize: Int, maxSize: Option[Int])

  case class Settings(minSize: Int, maxSize: Option[Int], action: Action)

  val opts  = makeOption[Settings]
  val command = Command("my-program", "My program! ")(opts)

  println(" ~~~~~ ")
  println(command.parse(List("--help")))
  println(" ~~~~~ ")
  println(command.parse(List("create", "--help")))
  println(" ~~~~~ ")

}





