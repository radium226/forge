package com.github.radium226.arguments.instances

import com.github.radium226.arguments._

import cats._
import cats.implicits._

import shapeless._

import pureconfig._


trait ArgumentsInstances {

  import ConfigReaderInstances._

  implicit def defaultArguments[F[_], Complete, Partial <: HList](implicit
    toPartial: ToPartial.Aux[F, Complete, Partial],
    toComplete: ToComplete.Aux[F, Partial, Complete],
    F: MonadError[F, Throwable],
    Partial: Monoid[Partial]
  ): Arguments[F, Complete] = new Arguments[F, Complete] {

    override def parse(arguments: List[String]): F[Complete] = {
      /*ConfigSource
        .string(
          """
            |toto.maxSize = 2
            |toto.name = 'Pipou'
            |""".stripMargin
        )
        .at("toto")
        .load[Partial]
        .fold[F[Partial]](
          { _ => F.raiseError(new Exception("Wut?!")) },
          { partial => F.pure(partial) }
        )
        .flatMap(_.toComplete[F])*/
      Partial.empty.toComplete[F]
    }

  }

}
