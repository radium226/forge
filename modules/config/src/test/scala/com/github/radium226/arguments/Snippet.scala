package com.github.radium226.arguments

import shapeless._
import shapeless.labelled._

object Snippet extends App {

  trait Measure[Input] {

    type Output

    def apply(input: Input): Output

  }

  object Measure {

    type Aux[Input, Output0] = Measure[Input] { type Output = Output0  }

    def apply[A](implicit measureInstance: Measure[A]): Measure[A] = measureInstance

  }

  trait LowPriorityMeasureInstances {

    implicit def measureGeneric[A, ReprA <: HList, MeasureReprA <: HList](implicit
      generic: Generic.Aux[A, ReprA],
      measureReprA: Measure.Aux[ReprA, MeasureReprA]
    ): Measure.Aux[A, MeasureReprA] = new Measure[A] {

      type Output = MeasureReprA

      def apply(a: A): Output = {
        measureReprA(generic.to(a))
      }

    }

    implicit def measureHCons[H, T <: HList, MeasureH, MeasureT <: HList](implicit
      measureH: Measure.Aux[H, MeasureH],
      measureT: Lazy[Measure.Aux[T, MeasureT]]
    ): Measure.Aux[H :: T, MeasureH :: MeasureT] = new Measure[H :: T] {

      type Output = MeasureH :: MeasureT

      override def apply(input: H :: T): Output = {
        measureH(input.head) :: measureT.value(input.tail)
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

  import instances._
  import syntax._


  case class Person(firstName: String, lastName: String, age: Int)

  val adrien = Person("Adrien", "Besnard", 32)
  println(adrien.measure.measure) // Works
  val personMeasureInstance = implicitly[Measure[Person]]
  val intIntIntMeasureInstance = implicitly[Measure[Int :: Int :: Int :: HNil]] // Work
  //val measureOfPersonMeasureInstance = implicitly[Measure[personMeasureInstance.Output]] // Does not work




  //println(('toto ->> "Toto" :: 'two -> 2 :: HNil).measure)

}
