package com.github.radium226

import cats.implicits._

package object config {

  sealed trait Error

  case object AbsentValueError extends Error

  case object UnableToParseConfigError extends Error

  case object UnableToCreateOpsError extends Error

  case object UnableToParseArgumentsError extends Error

  case object NotImplementedError extends Error

  type Result2[+T] = Either[Error, T]

  object Result2 {

    def success[T](t: T): Result2[T] = {
      Right(t)
    }

    def failure(error: Error): Result2[Nothing] = {
      Left(error)
    }

  }

  type Partial[+T] = Option[T]

  object Partial {

    def present[T](t: T): Partial[T] = {
      Some(t)
    }

    def absent[T]: Partial[T] = none[T]

  }

}
