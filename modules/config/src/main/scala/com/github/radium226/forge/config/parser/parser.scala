package com.github.radium226.forge.config.parser


import com.github.radium226.forge.config.OneLastTry.{ConfigError, ConfigResult}
import shapeless._
import com.typesafe.config.{Config => TypesafeConfig, ConfigFactory => TypesafeConfigFactory}
import shapeless.labelled._

import java.nio.file.Path

import scala.util.Try

trait Parse[C] {

  def apply(parentConfig: TypesafeConfig, pathOption: Option[String]): ConfigResult[C]

}

class Entries(config: TypesafeConfig, pathOption: Option[String]) {
  self =>

  def in(path: String): Entries = {
    new Entries(config, Some(path))
  }

  def as[C](implicit parseInstance: Parse[C]): ConfigResult[C] = {
    parseInstance(config, pathOption)
  }

}

object Entries {

  def apply(content: String): ConfigResult[Entries] = {
    Try(TypesafeConfigFactory.parseString(content))
      .fold(
        { _ => Left(ConfigError) },
        { config => Right(new Entries(config, None)) }
      )
  }

}

trait LowPriorityParserInstances {

  implicit def genericParser[C, ReprC <: HList](implicit
    generic: LabelledGeneric.Aux[C, ReprC],
    reprCParser: Parse[ReprC]
  ): Parse[C] = { (parentConfig, path) =>
    reprCParser(parentConfig, path).map(generic.from(_))
  }

  implicit def hnilParser: Parse[HNil] = { (_, _) => Right(HNil) }

  implicit def hconsParser[ReprCHeadKey <: Symbol, ReprCHeadValue, ReprCTail <: HList](implicit
    reprCHeadValueParser: Parse[ReprCHeadValue],
    reprCTailParser: Parse[ReprCTail],
    reprCHeadKeyWitness: Witness.Aux[ReprCHeadKey]
  ): Parse[FieldType[ReprCHeadKey, ReprCHeadValue] :: ReprCTail] = { (parentConfig, pathOption) =>
    for {
      reprCHeadValue <- reprCHeadValueParser(parentConfig, Some(s"${pathOption.map({ path => s"${path}."}).getOrElse("")}${reprCHeadKeyWitness.value.name}"))
      reprCTail      <- reprCTailParser(parentConfig, pathOption)
    } yield field[ReprCHeadKey](reprCHeadValue) :: reprCTail
  }

}

trait ParserInstances extends LowPriorityParserInstances {

  implicit def stringParser: Parse[String] = { (config, pathOption) =>
    pathOption match {
      case Some(path) =>
        Try(config.getString(path)).fold({ _ => Left(ConfigError) }, Right(_))

      case None =>
        Left(ConfigError)
    }
  }

}