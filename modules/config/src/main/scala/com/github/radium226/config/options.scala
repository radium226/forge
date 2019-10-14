package com.github.radium226.config

import com.monovore.decline._
import shapeless._
import shapeless.labelled._
import cats.implicits._
import com.google.common.base.CaseFormat
import pureconfig._

trait MakeOption[Value, Help] {

  def apply(help: Help): Result[Opts[Value]]

}

object MakeOption {

  type Aux[Value, Help] = MakeOption[Value, Help]

  def instance[Value, Help](f: Help => Result[Opts[Value]]): MakeOption[Value, Help] = new MakeOption[Value, Help] {

    override def apply(help:  Help): Result[Opts[Value]] = f(help)

  }

}

trait MakeOptionInstances {

  implicit def makeOptionForHNil: MakeOption.Aux[HNil, HNil] = MakeOption.instance({ _ =>
    Result.success(Opts.unit.map({ _ => HNil}))
  })

  implicit def makeOptionForHCons[K <: Symbol, ValueH, ValueT <: HList, HelpH <: Option[help], HelpT <: HList](implicit
    makeOptionForT: MakeOption.Aux[ValueT, HelpT],
    witnessForK: Witness.Aux[K],
    argumentForValueH: Argument[ValueH]
  ): MakeOption.Aux[FieldType[K, Partial[ValueH]] :: ValueT, HelpH :: HelpT] = MakeOption.instance({ help =>
    val name = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, witnessForK.value.name)
    val optsForH = Opts.option[ValueH](name, help.head.map(_.value).getOrElse("Unknown")).orNone
    makeOptionForT(help.tail)
      .map({ optsForT =>
        (optsForH, optsForT)
          .mapN({ (h, t) =>
            field[K](h) :: t
          })
      })
  })

}
