package com.github.radium226.config

import com.google.common.base.CaseFormat
import com.monovore.decline.{Command, Opts}
import shapeless._
import shapeless.labelled._

import scala.reflect.ClassTag

trait MakeSubcommand[Value] {

  def apply: Result[Opts[Value]]

}

object MakeSubcommand {

  type Aux[Value] = MakeSubcommand[Value]

  def instance[Value](f: => Result[Opts[Value]]): MakeSubcommand[Value] = new MakeSubcommand[Value] {

    override def apply: Result[Opts[Value]] = f

  }

}

trait MakeSubcommandSyntax {

  def makeSubcommand[T](implicit makeSubcommandForT: MakeSubcommand[T]): Result[Opts[T]] = makeSubcommandForT.apply

}

trait MakeSubcommandLowPriorityInstances {

  implicit def makeSubcommandForAny[T, HelpsForT <: HList](implicit
    makeOptionForT: Lazy[MakeOption[T, HelpsForT]],
    helpsForT: Annotations.Aux[help, T, HelpsForT],
    classTagForT: ClassTag[T],
    nameForT: AnnotationOption[name, T],
    helpForT: AnnotationOption[help, T]
  ): MakeSubcommand.Aux[T] = MakeSubcommand.instance({
    val defaultName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, classTagForT.runtimeClass.getSimpleName) // FIXME: We can use an annotation `@name`
    val name = nameForT().map(_.value).getOrElse(defaultName)

    val defaultHelp = s"No help for ${name}! "
    val help = helpForT().map(_.value).getOrElse(defaultHelp)

    makeOptionForT
      .value(helpsForT())
      .map({ opts =>
        Opts.subcommand[T](name = name, help = help)(opts)
      })
  })

  implicit def makeSubcommandForFieldType[K <: Symbol, T](implicit
    witnessForK: Witness.Aux[K],
    makeSubcommandForT: MakeSubcommand[T]
  ): MakeSubcommand[FieldType[K, T]] = MakeSubcommand.instance({
    makeSubcommandForT.apply.map(_.map(field[K](_)))
  })

}

trait MakeSubcommandInstances extends MakeSubcommandLowPriorityInstances {

  implicit def makeSubcommandForCNil: MakeSubcommand.Aux[CNil] = MakeSubcommand.instance(Result.success(Opts.never))

  implicit def makeSubcommandForCCons[H, T <: Coproduct](implicit
    makeSubcommandForH: MakeSubcommand.Aux[H],
    makeSubcommandForT: MakeSubcommand.Aux[T]
  ): MakeSubcommand.Aux[H :+: T] = MakeSubcommand.instance({
    for {
      subcommandForH <- makeSubcommandForH.apply
      subcommandForT <- makeSubcommandForT.apply
    } yield subcommandForH.orElse(subcommandForT).map(_.asInstanceOf[H :+: T])
  })

  implicit def makeSubcommandForGeneric[T, ReprT](implicit
    generic: Generic.Aux[T, ReprT],
    actionForT: Annotation[action, T],
    makeSubcommandForReprT: MakeSubcommand.Aux[ReprT]
  ): MakeSubcommand.Aux[T] = MakeSubcommand.instance({
    makeSubcommandForReprT.apply.map(_.map(generic.from(_)))
  })

}
