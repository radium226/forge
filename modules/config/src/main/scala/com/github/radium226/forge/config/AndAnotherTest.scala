package com.github.radium226.forge.config

import shapeless._
import shapeless.ops.record._
import shapeless.ops.hlist._
import scopt._
import shapeless.labelled.FieldType


object AndAnotherTest extends App {

  /*trait ParserProvider[C] {

    def provideParsers(builder: OParserBuilder[C]): List[OParser[_, C]]

  }

  object ParserProvider {

    def provideParser[C](implicit parserProvider: ParserProvider[C]): OParser[_, C] = {
      val builder = OParser.builder[C]
      val parsers = parserProvider.provideParsers(builder)
      val parser = OParser.sequence(parsers.head, parsers.tail: _*)
      parser
    }

  }

  trait ParserProviderInstances0 {

    implicit def genericParser[G <: HList](implicit
      generic: LabelledGeneric.Aux[C, G],
      parserProviderG: Lazy[ParserProvider[G]]
    ): ParserProvider[C] = {

    }

    implicit def hconsParserProvider[C, K <: Symbol, V, T <: HList](implicit
      generic: LabelledGeneric[C],
      witnessK: Witness.Aux[K],
      parserProviderT: Lazy[ParserProvider[C]],
      readV: Read[V]
    ): ParserProvider[C] = new ParserProvider[C] {

      override def provideParsers(builder: OParserBuilder[C]): List[OParser[_, C]] = {
        builder.opt[V](witnessK.value.name) +: parserProviderT.value.provideParsers(builder)
      }

    }

  }

  object implicits extends ParserProviderInstances0

  import implicits._

  case class Person(firstName: String, lastName: String)

  val parser = ParserProvider.provideParser[Person]

  println(OParser.usage(parser))*/


}
