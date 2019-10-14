package com.github.radium226

import cats.implicits._

package object config {

  sealed trait Error

  case object AbsentValueError extends Error

  case object UnableToParseConfigError extends Error

  type Result[+T] = Either[Error, T]

  object Result {

    def success[T](t: T): Result[T] = {
      Right(t)
    }

    def failure(error: Error): Result[Nothing] = {
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
