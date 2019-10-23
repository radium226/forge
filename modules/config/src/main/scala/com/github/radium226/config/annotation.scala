package com.github.radium226.config

import scala.annotation.StaticAnnotation

case class help(value: String) extends StaticAnnotation

case class header(value: String) extends StaticAnnotation

case class subcommand(value: String) extends StaticAnnotation

case class name(value: String) extends StaticAnnotation

case class action() extends StaticAnnotation
