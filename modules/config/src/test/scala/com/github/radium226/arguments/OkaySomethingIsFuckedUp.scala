package com.github.radium226.arguments

import pureconfig._
import pureconfig.generic.auto._

import shapeless._
import shapeless.labelled._
import shapeless.syntax.singleton._

/*object OkaySomethingIsFuckedUp extends App {

  trait Measure[Input] {

    type Output

    def apply(input: Input): Output

  }

  object Measure {

    type Aux[Input0, Output0 <: HList] = Measure[Input0] { type Output = Output0 }

  }

  trait LowPriorityMeasureInstances {

    implicit def measureGeneric[A, ReprA <: HList, MeasureReprA <: HList](implicit
      labelledGeneric: LabelledGeneric.Aux[A, ReprA],
      measureReprA: Measure.Aux[ReprA, MeasureReprA]
    ): Measure.Aux[A, MeasureReprA] = new Measure[A] {

      type Output = MeasureReprA

      def apply(a: A): Output = {
        measureReprA(labelledGeneric.to(a))
      }

    }

    implicit def measureHCons[K <: Symbol, H, T <: HList, MeasureH, MeasureT <: HList](implicit
      measureH: Measure.Aux[H, MeasureH],
      measureT: Lazy[Measure.Aux[T, MeasureT]],
      witness: Witness.Aux[K]
    ): Measure.Aux[FieldType[K, H] :: T, FieldType[K, MeasureH] :: MeasureT] = new Measure[FieldType[K, H] :: T] {

      type Output = FieldType[K, MeasureH] :: MeasureT

      override def apply(input: FieldType[K, H] :: T): Output = {
        field[K](measureH(input.head)) :: measureT.value(input.tail)
      }

    }

    implicit def measureHNil: Measure.Aux[HNil, HNil] = new Measure[HNil] {

      type Output = HNil

      def apply(hNil: HNil): HNil = {
        HNil
      }

    }

  }

  trait MeasureInstances extends LowPriorityMeasureInstances {

    implicit def measureString: Measure.Aux[String, Int] = new Measure[String] {

      type Output = Int

      def apply(string: String): Int = {
        string.length
      }

    }

    implicit def measureInt: Measure.Aux[Int, Int] = new Measure[Int] {

      type Output = Int

      def apply(int: Int): Int = {
        int
      }

    }

  }

  trait MeasureSyntax {

    import scala.language.implicitConversions

    implicit class MeasureOps[A](a: A) {

      def measure(implicit measureInstance: Measure[A]): measureInstance.Output = {
        measureInstance(a)
      }

    }

  }

  object instances extends MeasureInstances
  object syntax extends MeasureSyntax

  import instances._, syntax._


  case class Person(firstName: String, lastName: String, age: Int)

  val adrien = Person("Adrien", "Besnard", 32)
  println(adrien.measure)

  val aude = ConfigSource
    .string(
      s"""
        |first-name: 'Aude'
        |last-name: 'Besnard
        |age: ${32 - 5 + 1}
        |""".stripMargin)
    .load[Person]
  println(aude)

  val personMeasureInstance = implicitly[Measure[Person]]

  println()

  //val measureOfPersonMeasureInstance = implicitly[personMeasureInstance.Output]




  //println(('toto ->> "Toto" :: 'two -> 2 :: HNil).measure)

}*/
