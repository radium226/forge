package com.github.radium226.forge.config

import shapeless._
import shapeless.labelled._
import shapeless.ops.hlist._

import scala.annotation.StaticAnnotation

object WTFWithAnnotations extends App {

  type Explanation = String

  type Help = Map[Symbol, Explanation]

  object Help {

    def empty: Help = Map.empty[Symbol, Explanation]

    def apply(key: Symbol, explanation: Explanation): Help = {
      Map(key -> explanation)
    }

  }

  case class help(text: Explanation) extends StaticAnnotation

  trait GatherHelp[A] {

    def apply: Help

  }

  trait HelpGatherer[A] {

    def gatherHelp(explanations: List[Explanation]): Help

  }

  object HelpGatherer {

    def instance[A](f: List[Explanation] => Help): HelpGatherer[A] = new HelpGatherer[A] {

      override def gatherHelp(explanations: List[Explanation]): Help = {
        f(explanations)
      }

    }

  }

  trait HelpGathererLowPrioriryInstances {

    implicit def hnilHelpGatherer: HelpGatherer[HNil] = HelpGatherer.instance({ _ => Help.empty })

    implicit def hconsHelpGatherer[ReprAHeadKey <: Symbol, ReprAHeadValue, ReprATail <: HList](implicit
      reprAHeadKeyWitness: Witness.Aux[ReprAHeadKey],
      reprATailHelpGatherer: HelpGatherer[ReprATail]
    ): HelpGatherer[FieldType[ReprAHeadKey, ReprAHeadValue] :: ReprATail] = HelpGatherer.instance({ explanations =>
      Help(reprAHeadKeyWitness.value, explanations.head) ++ reprATailHelpGatherer.gatherHelp(explanations.tail)
    })

  }

  trait HelpGathererInstances extends HelpGathererLowPrioriryInstances {

  }

  object GatherHelp {

    def instance[A](f: => Help): GatherHelp[A] = new GatherHelp[A] {

      override def apply: Help = f

    }

  }

  trait GatherHelpLowPriorityInstances {

    implicit def gatherHelpLabelledGeneric[A, ReprA <: HList, HelpAnnotations <: HList](implicit
      labelledGeneric: LabelledGeneric.Aux[A, ReprA],
      helpAnnotations: Annotations.Aux[help, A, HelpAnnotations],
      helpAnnotationsToTraversable: ToTraversable.Aux[HelpAnnotations, List, Option[help]],
      helpGatherer: HelpGatherer[ReprA]
    ): GatherHelp[A] = GatherHelp.instance[A]({
      val explanations = helpAnnotations().toList.map(_.getOrElse(help("No explanation"))).map(_.text)
      println(s"explanations=${explanations}")
      helpGatherer.gatherHelp(explanations)
    })

  }

  trait GatherHelpInstances extends GatherHelpLowPriorityInstances {

  }

  object instances extends GatherHelpInstances with HelpGathererInstances

  object syntax {

    def gatherHelp[A](implicit gatherHelp: GatherHelp[A]): Help = gatherHelp.apply

  }

  import instances._
  import syntax._

  case class Drink(
    @help("Taste... ") taste: String,
    @help("Quantity... ") quantity: Int
  )

  println(gatherHelp[Drink])

}
