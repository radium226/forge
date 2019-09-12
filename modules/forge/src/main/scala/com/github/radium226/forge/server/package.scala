package com.github.radium226.forge

import cats.data.ReaderT

package object server {

  type Action[F[_], A] = ReaderT[F, Config[F], A]

}
