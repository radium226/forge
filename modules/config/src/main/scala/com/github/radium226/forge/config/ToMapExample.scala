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
      implicit reprAHeadKeyWitness: Witness.Aux[ReprAHeadKey], describeReprAHeadValue: Describe[ReprAHeadValue], describeReprATail: Lazy[Describe[ReprATail]], reprAHeadValueClassTag: ClassTag[ReprAHeadValue]
    ): Describe[FieldType[ReprAHeadKey, ReprAHeadValue] :: ReprATail] = new Describe[FieldType[ReprAHeadKey, ReprAHeadValue] :: ReprATail] {

      override def apply: Description = {
        val name: String = reprAHeadKeyWitness.value.name
        val description = describeReprAHeadValue.apply
        describeReprATail.value.apply + (name -> description)
      }

    }

  }

  trait DescribePriority2 extends DescribePriority1 {

    implicit def describeHConsWithCoproduct[K <: Symbol, H, ReprH <: Coproduct, T <: HList](

    ): Describe[ReprH]

  }

  object describe extends DescribePriority1 {

    implicit def describeLabelledGeneric[A, ReprA <: HList](
      implicit labelledGeneric: LabelledGeneric.Aux[A, ReprA], describeReprA: Describe[ReprA]
    ): Describe[A] = new Describe[A] {

      override def apply: Description = describeReprA.apply

    }

    def apply[A](implicit describeA: Describe[A]): Description = describeA.apply

  }

  import describe._

  sealed trait Action

  case object Create extends Action

  case object Delete extends Action

  case class SubNested(float: Float)

  case class Nested(double: Double, subNested: SubNested)

  case class Config(int: Int, string: String, action: Action)

  println(describe[Config])

}
