package com.github.radium226.forge.config.parser

object ParserApp extends App with ParserInstances {

  case class Toto(tata: String)

  val result = for {
    entries       <- Entries(
      """
        |tata = 'Coucou'
        |toto.tata = 'Pipou'
        |""".stripMargin)

    rootToto      <- entries.as[Toto]
    subToto       <- entries.in("toto").as[Toto]
  } yield (rootToto, subToto)

  println(result)

}
