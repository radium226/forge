package com.github.radium226.forge.config

import shapeless._
import shapeless.labelled.FieldType
import shapeless.ops.coproduct.Mapper

import scala.reflect.ClassTag

object ToMapExample extends App {

  type Description = Map[String, Any]

  trait Describe[A] {

    def apply: Description

  }

  trait DescribePriority0 {

    implicit def describeLabelledGeneric[A, ReprA <: HList](
      implicit labelledGeneric: LabelledGeneric.Aux[A, ReprA], describeReprA: Describe[ReprA]
    ): Describe[A] = new Describe[A] {

      override def apply: Description = describeReprA.apply

    }

    implicit def describeHNil: Describe[HNil] = new Describe[HNil] {

      override def apply: Description = Map.empty

    }

    implicit def describeHCons[ReprAHeadKey <: Symbol, ReprAHeadValue, ReprATail <: HList](
      implicit reprAHeadKeyWitness: Witness.Aux[ReprAHeadKey], describeReprATail: Describe[ReprATail], reprAHeadValueClassTag: ClassTag[ReprAHeadValue]
    ): Describe[FieldType[ReprAHeadKey, ReprAHeadValue] :: ReprATail] = new Describe[FieldType[ReprAHeadKey, ReprAHeadValue] :: ReprATail] {

      override def apply: Description = {
        val name: String = reprAHeadKeyWitness.value.name
        val description = reprAHeadValueClassTag.runtimeClass.getSimpleName.asInstanceOf[Any]
        describeReprATail.apply + (name -> description)
      }

    }

  }

  trait DescribePriority1 extends DescribePriority0 {

    implicit def describeHConsWithDescribeInHead[ReprAHeadKey <: Symbol, ReprAHeadValue, ReprATail <: HList](
      implicit reprAHeadKeyWitness: Witness.Aux[ReprAHeadKey], describeReprAHeadValue: Describe[ReprAHeadValue], describeReprATail: Describe[ReprATail], reprAHeadValueClassTag: ClassTag[ReprAHeadValue]
    ): Describe[FieldType[ReprAHeadKey, ReprAHeadValue] :: ReprATail] = new Describe[FieldType[ReprAHeadKey, ReprAHeadValue] :: ReprATail] {

      override def apply: Description = {
        val name: String = reprAHeadKeyWitness.value.name
        val description = describeReprAHeadValue.apply
        describeReprATail.apply + (name -> description)
      }

    }

  }

  object describe extends DescribePriority1 {

    def apply[A](implicit describeA: Describe[A]): Description = describeA.apply

  }

  import describe._

  case class Nested(double: Double)

  case class Config(int: Int, string: String, nested: Nested)

  println(describe[Config])

}
