package com.github.radium226.forge.config

import java.nio.file._

import cats.effect._
import cats._

import cats.implicits._


trait ConfigBuilder[F[_], C] {

  def appName: String

  def moduleName: String

  def default(implicit F: Sync[F]): F[C]

  def parseArguments(arguments: List[String])(implicit F: Sync[F]): F[C]

  def parseFile(filePath: Path)(implicit F: Sync[F]): F[C]

}

object ConfigBuilder {

  def system[F[_], C](implicit configFactory: ConfigBuilder[F, C], F: Sync[F], C: Monoid[C]) = {
    configFactory.parseFile(Paths.get(s"/etc/${configFactory.appName}/${configFactory.moduleName}.conf"))
  }

  def user[F[_], C](implicit configFactory: ConfigBuilder[F, C], F: Sync[F], C: Monoid[C]) = {
    F.delay(Paths.get(System.getProperty("user.home"))) flatMap { homeFolderPath =>
      configFactory.parseFile(homeFolderPath.resolve(".config").resolve(configFactory.appName).resolve(s"${configFactory.moduleName}.conf"))
    }
  }

  def default[F[_], C](implicit configBuilder: ConfigBuilder[F, C], F: Sync[F], C: Monoid[C]) = {
    configBuilder.default
  }

  def build[F[_], C](arguments: List[String])(implicit configBuilder: ConfigBuilder[F, C], F: Sync[F], C: Monoid[C]): F[C] = {
    List(default, system, user, configBuilder.parseArguments(arguments))
      .traverse(identity)
      .map(C.combineAll)
  }

  def resource[F[_], C](arguments: List[String])(implicit configFactory: ConfigBuilder[F, C], F: Sync[F], C: Monoid[C]): Resource[F, C] = {
    Resource.liftF[F, C](build(arguments))
  }

}