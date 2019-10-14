package com.github.radium226.arguments

object Arguments {

  def of[F[_], A](implicit arguments: Arguments[F, A]): Arguments[F, A] = arguments

}

trait Arguments[F[_], A] {

  def parse(arguments: List[String]): F[A]

}


