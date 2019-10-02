package com.github.radium226.forge.config

import shapeless._
import cats.implicits._
import scopt._
import shapeless.labelled._
import shapeless.record._
import shapeless.ops.record._
import shapeless.ops.hlist._

import scala.reflect.ClassTag

object LetsGiveAnotherTry extends App {

  trait Default[C] {

    def default: C

  }

  trait ArgumentParser[C] {

    def parseArguments(arguments: List[String]): Option[C]

  }

  trait OParserProvider[A, C] {

    def provideOParsers(builder: OParserBuilder[C]): List[OParser[_, C]]

  }

  trait OParserProvider0 {

    implicit def hnilOParserProvider[C]: OParserProvider[HNil, C] = { _: OParserBuilder[C] =>
      println("We are heeeere ! ")
      List.empty[OParser[HNil, C]]
    }

    implicit def hconsOParserProvider[K <: Symbol, V, T <: HList, C <: HList](
      implicit witnessK: Witness.Aux[K], parserProviderT: Lazy[OParserProvider[T, C]], readV: Read[V], classTagV: ClassTag[V], updater: Updater.Aux[C, FieldType[K, V], C]
    ): OParserProvider[FieldType[K, V]:: T, C] = { builder: OParserBuilder[C] =>
      println(s"We may be heeeere for ${witnessK.value} of ${classTagV.runtimeClass}! ")
      val opt = builder.opt[V](witnessK.value.name).action({ (v, c) =>
        c.updated(witnessK, v)
      })

      opt +: parserProviderT.value.provideOParsers(builder)
    }

  }

  object OParserProvider extends OParserProvider0 {

    implicit def actionOParserProvider[K <: Symbol, V, VG <: HList, T <: HList, C <: HList](
      implicit witnessK: Witness.Aux[K], parserProviderT: Lazy[OParserProvider[T, C]], generic: LabelledGeneric.Aux[V, VG], classTagV: ClassTag[V]
    ): OParserProvider[FieldType[K, V]:: T, C] = { builder: OParserBuilder[C] =>
      println(s"Yay, we are here for ${classTagV.runtimeClass.getSimpleName}! ")
      /*builder.cmd(classTagV.runtimeClass.getSimpleName).children(
        parserProviderV.value.provideOParsers(builder): _*
      ) +: */parserProviderT.value.provideOParsers(builder)
    }

    def provideOParsers[C](implicit parserProviderC: OParserProvider[C, C]): OParser[_, C] = {
      val builder = OParser.builder[C]
      val parsers = parserProviderC.provideOParsers(builder)
      OParser.sequence(parsers.head, parsers.tail: _*)
    }

  }

  import Default._

  object ArgumentParser {

    implicit def labelledGenericFoo[A, G](
      implicit labelledGeneric: LabelledGeneric.Aux[A, G], oparserProviderG: OParserProvider[G, G], G: Default[G]
    ): ArgumentParser[A] = new ArgumentParser[A] {

      override def parseArguments(arguments: List[String]): Option[A] = {
        val parser: OParser[_, G] = OParserProvider.provideOParsers[G]
        OParser.parse(parser, arguments, G.default).map(labelledGeneric.from(_))
      }

    }

    def apply[A](implicit A: ArgumentParser[A]): ArgumentParser[A] = A

  }

  trait Default0 {

    implicit def genericDefault[A, G <: HList](
      implicit generic: LabelledGeneric.Aux[A, G], G: Default[G]
    ): Default[A] = new Default[A] {

      override def default: A = generic.from(G.default)

    }

  }

  object Default extends Default0 {

    implicit def hnilDefault: Default[HNil] = new Default[HNil] {

      override def default: HNil = HNil

    }

    implicit def hconsDefault[K <: Symbol, V, T <: HList](
      implicit V: Default[V], T: Lazy[Default[T]]
    ): Default[FieldType[K, V] :: T] = new Default[FieldType[K, V] :: T] {

      override def default: FieldType[K, V] :: T = {
        field[K](V.default) :: T.value.default
      }

    }

    implicit def optionDefault[A]: Default[Option[A]] = new Default[Option[A]] {

      override def default: Option[A] = None

    }

    implicit def intDefault: Default[Int] = new Default[Int] {

      override def default: Int = 0

    }

    def apply[A](implicit A: Default[A]): A = A.default

  }

  import Default._
  import ArgumentParser._
  import OParserProvider._

  trait Action

  case class Nested(kikoo: Option[String]) extends Action

  case class Dunno(wtf: Option[String], still: Int, nested: Nested)

  val foo = ArgumentParser[Dunno]
  println(foo.parseArguments(List("--still=2", "--wtf=caca")))


}
