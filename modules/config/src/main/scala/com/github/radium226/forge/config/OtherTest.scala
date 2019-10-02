package com.github.radium226.forge.config

import shapeless._
import shapeless.labelled._

object OtherTest extends App {

  trait FieldNames[A] {

    def fieldNames(a: A): List[String]

  }

  trait FieldNamesInstance0 {

    implicit def genericRender[A, G <: HList](implicit
      generic: LabelledGeneric.Aux[A, G],
      fieldNamesG: Lazy[FieldNames[G]]
    ): FieldNames[A] = new FieldNames[A] {

      override def fieldNames(a: A): List[String] = fieldNamesG.value.fieldNames(generic.to(a))

    }

    implicit def hnilRender: FieldNames[HNil] = new FieldNames[HNil] {

      override def fieldNames(a: HNil): List[String] = List.empty[String]

    }

    implicit def hconsRender[HK <: Symbol, H, T <: HList](implicit
      fieldNamesT: Lazy[FieldNames[T]],
      witnessHK: Witness.Aux[HK],
    ): FieldNames[FieldType[HK, H] :: T] = new FieldNames[FieldType[HK, H] :: T] {

      override def fieldNames(ht: FieldType[HK, H] :: T): List[String] = {
        List(witnessHK.value.name) ++ fieldNamesT.value.fieldNames(ht.tail)
      }

    }

  }

  trait FieldNamesInstance1 {

    implicit def hconsRender2[HK <: Symbol, H, T <: HList](implicit
      fieldNamesT: Lazy[FieldNames[T]],
      witnessHK: Witness.Aux[HK],
      fieldNamesFieldType: FieldNames[FieldType[HK, H]],
    ): FieldNames[FieldType[HK, H] :: T] = new FieldNames[FieldType[HK, H] :: T] {

      override def fieldNames(ht: FieldType[HK, H] :: T): List[String] = {
        List(witnessHK.value.name) ++ fieldNamesFieldType.fieldNames(ht.head) ++ fieldNamesT.value.fieldNames(ht.tail)
      }

    }

  }

  object FieldNames extends FieldNamesInstance0 with FieldNamesInstance1 {

    def apply[A](implicit fieldNames: FieldNames[A]): FieldNames[A] = fieldNames

  }

  case class Nested(
    someNestedField: Double
  )

  case class SomeConfig(
    someField: String,
    someOtherField: Int,
    nested: Nested
  )



  val config = SomeConfig("config", 2, Nested(2.3))
  println(FieldNames[SomeConfig].fieldNames(config))

}
