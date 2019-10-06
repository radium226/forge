package com.github.radium226.forge.config.parser

object ParserApp extends App with ParserInstances {

  case class Toto(tata: String, titi: Option[String])

  case class Titou(papo: String)

  val result = for {
    entries       <- Entries(
      """
        |tata = 'Coucou'
        |toto.tata = 'Pipou'
        |""".stripMargin)

    rootToto      <- entries.as[Toto]
    t             <- entries.as[Option[Titou]]
    subToto       <- entries.at("toto").as[Toto]
  } yield (rootToto, subToto, t)

  println(result)

}
