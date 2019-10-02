package com.github.radium226.forge.config
/*
import cats.{Applicative, Functor}
import scopt._
import shapeless._
import shapeless.ops.hlist._

object BigStuff extends App {

  sealed trait ArgumentParserError

  case object UnableToParse extends ArgumentParserError

  type ArgumentParserResult[A] = Either[ArgumentParserError, A]

  object ArgumentParserResult {

    def apply[A](a: A): ArgumentParserResult[A] = {
      Right(a)
    }

  }

  trait ArgumentParser[A] {

    def parseArguments(arguments: List[String]): ArgumentParserResult[A]

  }

  trait ArgumentParserInstances0 {

    implicit def hlistArgumentParser[G <: HList](implicit
      generic: LabelledGeneric[G]
    ): ArgumentParser[G] = new ArgumentParser[G] {

      override def parseArguments(arguments: List[String]): ArgumentParserResult[G] = {
      }

    }

  }

  implicit def parserFunctor: Functor[ArgumentParser] = new Functor[ArgumentParser] {

    override def map[A, B](fa: ArgumentParser[A])(f: A => B): ArgumentParser[B] = new ArgumentParser[B] {

      override def parseArguments(arguments: List[String]): ArgumentParserResult[B] = {
        fa.parseArguments(arguments).map(f)
      }

    }

  }

  sealed trait ConfigBuilderError

  type ConfigBuilderResult[A] = Either[ConfigBuilderError, A]

  trait ConfigBuilder[C] {

    def defaultConfig: C

    def argumentParser: ArgumentParser[C]

    def buildConfig(arguments: List[String]): ConfigBuilderResult[C] = {
      //argumentParser.parseArguments(arguments)
      ???
    }

  }

  object ConfigBuilder {

    def apply[C](implicit configBuilder: ConfigBuilder[C]) = configBuilder

  }






}
 */
