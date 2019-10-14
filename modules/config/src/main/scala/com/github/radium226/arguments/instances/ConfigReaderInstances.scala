package com.github.radium226.arguments.instances

import com.google.common.base.CaseFormat

import pureconfig.ConfigReader

import shapeless._
import shapeless.labelled._

import cats.implicits._


trait ConfigReaderInstances {

  implicit def hNilReader: ConfigReader[HNil] = {
    println("I'm here too")
    ConfigReader.fromCursor(_ => HNil.asRight)
  }

  implicit def hConsReader[K <: Symbol, H, T <: HList](implicit
    headReader: ConfigReader[H],
    tailReader: ConfigReader[T],
    keyWitness: Witness.Aux[K],
  ): ConfigReader[FieldType[K, H] :: T] = {
    val keyName = keyWitness.value.name
    println(s"I'm here for ${keyName}")
    for {
      head <- ConfigReader.fromCursor[H]({ cursor =>
        for {
          objectCursor <- cursor.asObjectCursor
          keyCursor    <- objectCursor.atKey(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, keyName))
          head         <- headReader.from(keyCursor.value)
        } yield head
      })
      tail <- tailReader
    } yield field[K](head) :: tail
  }

}

object ConfigReaderInstances extends ConfigReaderInstances
