package com.github.radium226.arguments

import cats.effect.IO
import pureconfig._
import shapeless._
import cats.implicits._
import com.google.common.base.CaseFormat
import pureconfig.generic.semiauto._
import shapeless.labelled._

/*object PureConfigAndDependentType extends App {

  trait LowPriorityInstances {

    implicit def hNilReader: ConfigReader[HNil] = {
      println("I'm here too")
      ConfigReader.fromFunction(_ => HNil.asRight)
    }


    implicit def hConsReader[K <: Symbol, H, T <: HList](implicit
      headReader: ConfigReader[H],
      tailReader: ConfigReader[T]//,
      //keyWitness: Witness.Aux[K],
    ): ConfigReader[FieldType[K, H] :: T] = {
      //val keyName = keyWitness.value.name
      val keyName = "kikoo"
      println(s"I'm here for ${keyName}")
      for {
        head <- ConfigReader.fromCursor[H]({ cursor =>
          for {
            objectCursor <- cursor.asObjectCursor
            keyCursor <- objectCursor.atKey(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, keyName))
            head <- headReader.from(keyCursor.value)
          } yield head
        })
        tail <- tailReader
      } yield field[K](head) :: tail
    }

  }

  trait Instances extends LowPriorityInstances {

    implicit def hConsWithOptionReader[K <: Symbol, H, T <: HList](implicit
      headReader: ConfigReader[H],
      tailReader: ConfigReader[T]//,
      //keyWitness: Witness.Aux[K],
    ): ConfigReader[FieldType[K, Option[H]] :: T] = {
      //val keyName = keyWitness.value.name
      val keyName = "kikoo"
      println(s"I'm here for ${keyName}")
      for {
        head <- ConfigReader.fromCursor[Option[H]]({ cursor =>
          for {
            objectCursor <- cursor.asObjectCursor
            keyCursor    = objectCursor.atKeyOrUndefined(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, keyName))
            head         <- Option(keyCursor.value).traverse(headReader.from(_))
          } yield head
        })
        tail <- tailReader
      } yield field[K](head) :: tail
    }

  }

  object Instances extends Instances

  import Instances._

  case class Settings(maxSize: Int, name: Option[String])

  val labelledGeneric = LabelledGeneric[Settings]

  val toPartial = ToPartial[IO, Settings]

  //implicit val partialReader = deriveReader[toPartial.Output]

  val settings = ConfigSource
    .string(
      """
        |toto.max-size = 2
        |toto.name = 'Toto'
        |""".stripMargin
    )
    .at("toto")
    //.load[toPartial.Output]
    //.load[labelledGeneric.Repr]

  println(settings)

}*/
