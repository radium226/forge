package com.github.radium226.config

import com.google.common.base.CaseFormat

import pureconfig.ConfigReader

import shapeless._
import shapeless.labelled._

import cats.implicits._


trait LowPriorityConfigReaderInstances {

  implicit def configReaderForHNil: ConfigReader[HNil] = {
    ConfigReader.fromCursor(_ => HNil.asRight)
  }

  implicit def configReaderForHCons[K <: Symbol, H, T <: HList](implicit
    configReaderForH: ConfigReader[H],
    configReaderForT: ConfigReader[T],
    witnessForK: Witness.Aux[K],
  ): ConfigReader[FieldType[K, H] :: T] = {
    val keyName = witnessForK.value.name
    println(s"I'm here for ${keyName}")
    for {
      head <- ConfigReader.fromCursor[H]({ cursor =>
        for {
          objectCursor <- cursor.asObjectCursor
          keyCursor    <- objectCursor.atKey(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, keyName))
          head         <- configReaderForH.from(keyCursor.value)
        } yield head
      })
      tail <- configReaderForT
    } yield field[K](head) :: tail
  }

}

trait ConfigReaderInstances extends LowPriorityConfigReaderInstances {

  implicit def configReaderForPartialHCons[K <: Symbol, H, T <: HList](implicit
    configReaderForH: ConfigReader[H],
    configReaderForT: ConfigReader[T],
    witnessForK: Witness.Aux[K],
  ): ConfigReader[FieldType[K, Partial[H]] :: T] = {
    val keyName = witnessForK.value.name
    println(s"I'm here for ${keyName} (partial)")
    for {
      head <- ConfigReader.fromCursor[Option[H]]({ cursor =>
        for {
          objectCursor <- cursor.asObjectCursor
          head         <- Option(objectCursor.atKeyOrUndefined(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, keyName)).value).traverse(configReaderForH.from(_))
        } yield head
      })
      tail <- configReaderForT
    } yield field[K](head) :: tail
  }

}
