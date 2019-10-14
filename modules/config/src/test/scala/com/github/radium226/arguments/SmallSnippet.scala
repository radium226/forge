package com.github.radium226.arguments

import shapeless._
import shapeless.labelled._

object SmallSnippet extends App {

  trait Alter[Input] {

    type Output

    def apply(input: Input): Output

  }

  object Alter {

    type Aux[Input, Output0] = Alter[Input] { type Output = Output0}

    def apply[Input](alterInstance: Alter[Input]) = alterInstance

  }

  trait LowPriorityAlterInstances {



  }

  trait AlterInstances extends LowPriorityAlterInstances {

    implicit def forInt: Alter.Aux[Int, String] = new Alter[Int] {

      type Output = String

      def apply(int: Int) = "Forty Two"

    }

    implicit def forString: Alter.Aux[String, Int] = new Alter[String] {

      type Output = Int

      def apply(string: String) = 42

    }

  }

  trait AlterSyntax {

    implicit class AlterOps[Input](input: Input) {

      def alter(implicit alterInstance: Alter[Input]): alterInstance.Output = alterInstance.apply(input)

    }

  }

  trait Measure[Input] {

    type Output

    def apply(input: Input): Output

  }

  object Measure {

    type Aux[Input0, Output0] = Measure[Input0] { type Output = Output0 }

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

  object instances extends AlterInstances with MeasureInstances
  object syntax extends AlterSyntax with MeasureSyntax

  import instances._
  import syntax._

  val t1 = 2.alter.measure.alter
  println(t1)

  val t2 = t1.alter
  println(t2)

}
