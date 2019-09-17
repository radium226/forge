package com.github.radium226.forge

package object run {

  type Run[F[_]] = PartialFunction[Phase, F[Unit]]

}
