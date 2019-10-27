package com.github.radium226.config

import com.monovore.decline._
import shapeless._
import shapeless.labelled._
import cats.implicits._
import com.google.common.base.CaseFormat
import pureconfig._

trait MakeOption[Value, Help] {

  def apply(help: Help): Result2[Opts[Value]]

}

object MakeOption {

  type Aux[Value, Help] = MakeOption[Value, Help]

  def instance[Value, Help](f: Help => Result2[Opts[Value]]): MakeOption[Value, Help] = new MakeOption[Value, Help] {

    override def apply(help:  Help): Result2[Opts[Value]] = f(help)

  }

}

trait MakeOptionPartiallyApplied[T] {

  def apply: Result2[Opts[T]]

}

trait MakeOptionPartiallyAppliedInstances {

  implicit def makeOptionPartiallyAppliedForAny[T, HelpsForT <: HList](implicit
    helpsForT: Annotations.Aux[help, T, HelpsForT],
    makeOptionForT: MakeOption[T, HelpsForT]
  ): MakeOptionPartiallyApplied[T]  = new MakeOptionPartiallyApplied[T] {

    override def apply: Result2[Opts[T]] = makeOptionForT(helpsForT())

  }

}

trait MakeOptionSyntax {

  def makeOption[T](implicit makeOptionPartiallyAppliedForT: MakeOptionPartiallyApplied[T]): Result2[Opts[T]] = makeOptionPartiallyAppliedForT.apply

}

trait MakeOptionLowPriorityInstances {

  /*implicit def makeOptionForSubcommand[K <: Symbol, A, HelpForA <: Option[help]](implicit
    witnessForK: Witness.Aux[K],
    makeSubcommandForA: Lazy[MakeSubcommand.Aux[A]]
  ): MakeOption.Aux[FieldType[K, A], HelpForA] = MakeOption.instance({ _ =>
    makeSubcommandForA.value.apply.map(_.map(field[K](_)))
  })*/

  implicit def makeOptionForFieldType[K <: Symbol, A, HelpForA <: Option[help]](implicit
    //argumentForA: Argument[A],
    argumentOrMakeSubcommandForA: Argument[A] OrElse Lazy[MakeSubcommand.Aux[A]],
    witnessForK: Witness.Aux[K]
  ): MakeOption.Aux[FieldType[K, A], HelpForA] = MakeOption.instance({ help =>
    val name = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, witnessForK.value.name)
    /*Result2.success(Opts.option[A](name, help.map(_.value).getOrElse("No help! ")).map(field[K](_)))*/

    argumentOrMakeSubcommandForA.fold(
      { argumentForA => Result2.success(Opts.option[A](name, help.map(_.value).getOrElse("No help! "))(argumentForA)) },
      { _.value.apply }
    ).map(_.map(field[K](_)))
  })

}

trait MakeOptionMiddlePriorityInstances extends MakeOptionLowPriorityInstances {

  implicit def makeOptionForLabelledGeneric[T, ReprT <: HList, HelpsForT <: HList](implicit
    labelledGeneric: LabelledGeneric.Aux[T, ReprT],
    makeOptionForReprT: MakeOption[ReprT, HelpsForT],
  ): MakeOption.Aux[T, HelpsForT] = MakeOption.instance({ helps =>
    makeOptionForReprT(helps).map(_.map(labelledGeneric.from(_)))
  })


  implicit def makeOptionForHNil: MakeOption.Aux[HNil, HNil] = MakeOption.instance({ _ =>
    Result2.success(Opts.unit.map({ _ => HNil}))
  })

  implicit def makeOptionForHCons[K <: Symbol, H, T <: HList, HelpForH <: Option[help], HelpsForT <: HList](implicit
    //makeOptionOrSubcommandForH: MakeSubcommand.Aux[H] OrElse MakeOption.Aux[FieldType[K, H], HelpForH],
    makeOptionForH: MakeOption.Aux[FieldType[K, H], HelpForH],
    makeOptionForT: MakeOption.Aux[T, HelpsForT],
    witnessForK: Witness.Aux[K]
  ): MakeOption.Aux[FieldType[K, H] :: T, HelpForH :: HelpsForT] = MakeOption.instance({ help =>
    for {
      //optsForH <- makeOptionOrSubcommandForH.value.fold(_.apply("Dunno").map(_.map(field[K](_))), _.apply(help.head)) //FIXME: ???
      optsForH <- makeOptionForH(help.head)
      optsForT <- makeOptionForT(help.tail)
    } yield (optsForH, optsForT).mapN(_ :: _)
  })

}

trait MakeOptionInstances extends MakeOptionMiddlePriorityInstances with MakeOptionPartiallyAppliedInstances {

  /*implicit def makeOptionForFieldTypeOfOption[K <: Symbol, T, HelpForT <: Option[help]](implicit
    makeOptionForT: MakeOption.Aux[FieldType[K, T], HelpForT]
  ): MakeOption.Aux[FieldType[K, Option[T]], HelpForT] = MakeOption.instance({ helpForT =>
    makeOptionForT(helpForT).map({ optsForT =>
      optsForT.map(_.asInstanceOf[T]).orNone.map(field[K](_))
    })
  })*/

  /*implicit def makeOptionForOption[T, HelpForT <: Option[help]](implicit
    makeOptionForT: MakeOption.Aux[T, HelpForT]
  ): MakeOption.Aux[Option[T], HelpForT] = MakeOption.instance({ helpForT =>
    makeOptionForT(helpForT).map(_.orNone)
  })*/

}
