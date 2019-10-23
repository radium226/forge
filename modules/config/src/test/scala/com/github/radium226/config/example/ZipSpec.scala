package com.github.radium226.config.example

/*import com.github.radium226.config.ConfigSpec

import shapeless._
import shapeless.ops.hlist.Tupler.Aux

class ZipSpec extends ConfigSpec {

  it should "be able to zip at type level" in {

    trait ToPair[Input1, Input2] {

      type Output

    }

    object ToPair {

      type Aux[I1, I2, O] = ToPair[I1, I2] { type Output = O }

    }

    trait ToPairInstances {

      implicit def toPairForHCons[InputH1, InputT1 <: HList, InputH2, InputT2 <: HList, OutputT <: HList](implicit
        toPairForT: ToPair.Aux[InputT1, InputT2, OutputT]
      ): ToPair.Aux[InputH1 :: InputT1, InputH2 :: InputT2, (InputH1, InputH2) :: OutputT] = new ToPair[InputH1 :: InputT1, InputH2 :: InputT2] {

        type Output = (InputH1, InputH2) :: OutputT

      }

      implicit def toPairForHNil: ToPair.Aux[HNil, HNil, HNil] = new ToPair[HNil, HNil] {

        type Output = HNil

      }

    }

  }

}
*/