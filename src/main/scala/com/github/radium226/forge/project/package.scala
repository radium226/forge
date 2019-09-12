package com.github.radium226.forge

import cats.Applicative
import cats.data.{Kleisli, ReaderT}
import io.chrisdavenport.vault._

package object project {

  type Name = String

  type Config[F[_]] = Vault

  type Action[F[_], A] = ReaderT[F, Config[F], A]

  object Action {

    def apply[F[_], A](f: Config[F] => F[A]): Action[F, A] = {
      Kleisli(f)
    }

    def liftF[F[_], A](fa: F[A]): Action[F, A] = {
      Kleisli.liftF[F, Config[F], A](fa)
    }

    def pure[F[_], A](a: A)(implicit F: Applicative[F]): Action[F, A] = {
      Kleisli.pure[F, Config[F], A](a)
    }

  }

}
