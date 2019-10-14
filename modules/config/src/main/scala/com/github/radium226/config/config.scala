package com.github.radium226.config

import pureconfig._

import shapeless._

import cats._
import cats.kernel.Monoid
import cats.implicits._

import mouse.all._

import pureconfig.generic.auto._


trait Config[T] {

  def parse(texts: String*): Result[T]

}

object Config {

  def of[T](implicit configOfT: Config[T]): Config[T] = configOfT

}

trait ConfigInstances {

  implicit def configForAny[T, PartialForT <: HList, CompleteForPartialForT <: HList](implicit
    toPartialForT: ToPartial.Aux[T, PartialForT],
    toCompleteForPartialForT: ToComplete.Aux[PartialForT, CompleteForPartialForT],
    labelledGeneric: LabelledGeneric.Aux[T, CompleteForPartialForT],
    monoidForPartialForT: Monoid[PartialForT],
    derivationForConfigReaderForPartialT: Derivation[ConfigReader[PartialForT]]
  ): Config[T] = new Config[T] {

    def parse(texts: String*): Result[T] = {
      texts
        .toList
        .map(ConfigSource.string(_))
        .traverse({ configSourceObject =>
          val result = configSourceObject.load[PartialForT]
          println(result)
          result
        })
        .map(_.combineAll)
        .fold[Result[PartialForT]]({ _ => Result.failure(UnableToParseConfigError)}, Result.success(_))
        .flatMap(toCompleteForPartialForT(_))
        .map(labelledGeneric.from(_))
    }

  }

}
