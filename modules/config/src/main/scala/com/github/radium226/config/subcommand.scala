package com.github.radium226.config

import com.google.common.base.CaseFormat
import com.monovore.decline._
import shapeless._
import shapeless.labelled._

import scala.reflect.ClassTag


trait MakeSubcommand[Value] {

  def apply: Result2[Opts[Value]]

}

object MakeSubcommand {

  type Aux[Value] = MakeSubcommand[Value]

  def instance[Value](f: => Result2[Opts[Value]]): MakeSubcommand.Aux[Value] = new MakeSubcommand[Value] {

    override def apply: Result2[Opts[Value]] = f

  }

}

trait MakeSubcommandSyntax {

  def makeSubcommand[T](implicit makeSubcommandForT: MakeSubcommand[T]): Result2[Opts[T]] = makeSubcommandForT.apply

}

trait MakeSubcommandLowPriorityInstances {

  implicit def makeSubcommandForCNil: MakeSubcommand.Aux[CNil] = MakeSubcommand.instance(Result2.success(Opts.never))

  implicit def makeSubcommandForCCons[H, T <: Coproduct](implicit
    makeSubcommandForH: MakeSubcommand.Aux[H],
    makeSubcommandForT: MakeSubcommand.Aux[T]
  ): MakeSubcommand.Aux[H :+: T] = MakeSubcommand.instance({
    println("We are in makeSubcommandForCCons")
    for {
      subcommandForH <- makeSubcommandForH.apply
      subcommandForT <- makeSubcommandForT.apply
    } yield subcommandForH.orElse(subcommandForT).map(_.asInstanceOf[H :+: T])
  })

  implicit def makeSubcommandForCoproductButNotOption[T, ReprT <: Coproduct](implicit
    generic: Generic.Aux[T, ReprT],
    makeSubcommandForReprT: MakeSubcommand.Aux[ReprT],
    classTagForT: ClassTag[T]
  ): MakeSubcommand.Aux[T] = MakeSubcommand.instance({
    println(s"We are in makeSubcommandForCoproductButNotOption for ${classTagForT.runtimeClass.getSimpleName}")
    makeSubcommandForReprT.apply.map(_.map(generic.from(_)))
  })

}

trait MakeSubcommandInstances extends MakeSubcommandLowPriorityInstances {

  implicit def makeSubcommandForAny[T, HelpsForT <: HList](implicit
    helpsForT: Annotations.Aux[help, T, HelpsForT],
    makeOptionForT: MakeOption.Aux[T, HelpsForT],
    classTagForT: ClassTag[T],
    nameForT: AnnotationOption[name, T],
    helpForT: AnnotationOption[help, T]
  ): MakeSubcommand.Aux[T] = MakeSubcommand.instance({
    val defaultName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, classTagForT.runtimeClass.getSimpleName) // FIXME: We can use an annotation `@name`
    val name = nameForT().map(_.value).getOrElse(defaultName)

    val defaultHelp = s"No help for ${name}! "
    val help = helpForT().map(_.value).getOrElse(defaultHelp)

    makeOptionForT(helpsForT())
        .map({ opts =>
          Opts.subcommand[T](name = name, help = help)(opts)
        })
  })

}
