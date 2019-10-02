package com.github.radium226.forge.config

import shapeless._

import scala.reflect.ClassTag

object CoproductTry extends App {


  trait GatherCoproduct[A] {

    def apply: List[String]

  }

  object GatherCoproduct {

    implicit def genericGatherCoproduct[A, ReprA <: Coproduct](
      implicit generic: Generic.Aux[A, ReprA], gatherCoproductReprA: GatherCoproduct[ReprA]
    ): GatherCoproduct[A] = new GatherCoproduct[A] {

      override def apply: List[String] = gatherCoproductReprA.apply

    }

    implicit def cnilGatherCoproduct[A]: GatherCoproduct[CNil] = new GatherCoproduct[CNil] {

      override def apply: List[String] = List.empty

    }

    implicit def consGatherCoproduct[A, ReprAHead <: A, ReprATail <: Coproduct](
      implicit gatherCoproductReprATail: GatherCoproduct[ReprATail], classTagA: ClassTag[A]
    ): GatherCoproduct[ReprAHead :+: ReprATail] = new GatherCoproduct[ReprAHead :+: ReprATail] {

      override def apply: List[String] = {
        classTagA.runtimeClass.getSimpleName +: gatherCoproductReprATail.apply
      }

    }

    def apply[A](implicit gatherCoproductA: GatherCoproduct[A]): List[String] = gatherCoproductA.apply

  }

  sealed trait Direction

  case object West extends Direction

  case class Custom(value: String) extends Direction

  import GatherCoproduct._

  println(GatherCoproduct[Direction])

}
