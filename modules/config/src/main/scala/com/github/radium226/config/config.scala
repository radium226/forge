package com.github.radium226.config

import pureconfig._
import shapeless._
import shapeless.ops.hlist._
import cats._
import cats.kernel.Monoid
import cats.implicits._
import com.monovore.decline.Command
import mouse.all._
import pureconfig.generic.auto._


trait Config[T] {

  def parse(arguments: List[String], texts: String*): Result2[T]

}

object Config {

  def of[T](implicit configOfT: Config[T]): Config[T] = configOfT

}

trait ConfigInstances {

  implicit def configForAny[T, PartialForT <: HList, CompleteForPartialForT <: HList, DefaultForT <: HList, HelpForT <: HList](implicit
    // Partial <-> Complete
    toPartialForT: ToPartial.Aux[T, PartialForT],
    toCompleteForPartialForT: ToComplete.Aux[PartialForT, CompleteForPartialForT],
    labelledGeneric: LabelledGeneric.Aux[T, CompleteForPartialForT],
    // Monoid
    monoidForPartialForT: Monoid[PartialForT],
    // PureConfig
    derivationForConfigReaderForPartialT: Derivation[ConfigReader[PartialForT]],
    // Defaults + Annotations
    defaultForT: Default.AsOptions.Aux[T, DefaultForT],
    helpForT: Annotations.Aux[help, T, HelpForT],
    // Options
    makeOptionForPartialForT: MakeOption.Aux[PartialForT, HelpForT],
    // Header
    headerForT: AnnotationOption[header, T]
  ): Config[T] = new Config[T] {

    def parseFiles(texts: String*): Result2[List[PartialForT]] = {
      texts
        .toList
        .map(ConfigSource.string(_))
        .traverse({ configSourceObject =>
          val Result2 = configSourceObject.load[PartialForT]
          println(Result2)
          Result2
        })
        .fold({ _ => Result2.failure(UnableToParseConfigError)}, Result2.success(_))
    }

    def parseArguments(arguments: List[String]): Result2[PartialForT] = {
      println(s"defaultForT=${defaultForT()}")
      makeOptionForPartialForT(helpForT())
        .flatMap({ opts =>
          val command = Command[PartialForT](name = "Name", header = "Header")(opts)
          println(command.showHelp)
          command
            .parse(arguments)
            .fold[Result2[PartialForT]]({ help => println(help) ; Result2.failure(UnableToParseArgumentsError) }, { partialForT => Result2.success(partialForT) })
        })
    }

    def parse(arguments: List[String], texts: String*): Result2[T] = {
      for {
        partialsForTFromFiles    <- parseFiles(texts: _*)
        partialForTFromArguments <- parseArguments(arguments)
        partialsForT              =  List(defaultForT().asInstanceOf[PartialForT]) /* FIXME: We should not use asInstanceOf */ ++ partialsForTFromFiles ++ List(partialForTFromArguments)
        _                         = println(" ... ")
        _                         = partialsForT.foreach(println)
        _                         = println(" ... ")
        partialForT               = partialsForT.combineAll
        completeForT             <- toCompleteForPartialForT(partialForT)
        t                         = labelledGeneric.from(completeForT)
      } yield t
    }

  }

}
