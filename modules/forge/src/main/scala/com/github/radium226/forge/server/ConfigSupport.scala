package com.github.radium226.forge.server

import java.nio.file.{Path, Paths}

import cats.effect.Sync
import cats.kernel.Monoid
import com.github.radium226.forge.config.ConfigBuilder
import com.typesafe.config.{Config => TypeSafeConfig, ConfigFactory => TypeSafeConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader
import scopt._
import com.github.radium226.scopt.{implicits => ScoptImplicits}
import com.github.radium226.ficus.{implicits => FicusImplicits}
import cats.implicits._

trait ConfigSupport extends ScoptImplicits with FicusImplicits {

  implicit def catsConfigMonoid[F[_]]: Monoid[Config[F]] = new Monoid[Config[F]] {

    override def empty: Config[F] = {
      Config.empty[F]
    }

    override def combine(fallbackConfig: Config[F], config: Config[F]): Config[F] = {
      Config[F](
        port = config.port.orElse(fallbackConfig.port),
        baseFolderPath = config.baseFolderPath.orElse(fallbackConfig.baseFolderPath),
        scriptFolderPath = config.scriptFolderPath.orElse(fallbackConfig.scriptFolderPath)
      )
    }
  }

  implicit def ficusConfigValueReader[F[_]]: ValueReader[Config[F]] = new ValueReader[Config[F]] {

    def read(typeSafeConfig: TypeSafeConfig, path: String): Config[F] = {
      val fixedTypeSafeConfig = if (path == ".") typeSafeConfig else typeSafeConfig.getConfig(path)
      val port = fixedTypeSafeConfig.as[Option[Int]](s"port")
      val baseFolderPath = fixedTypeSafeConfig.as[Option[Path]]("base-folder-path")
      val scriptFolderPath = fixedTypeSafeConfig.as[Option[Path]]("script-folder-path")
      Config[F](
        port = port,
        baseFolderPath = baseFolderPath,
        scriptFolderPath = scriptFolderPath
      )
    }

  }

  implicit def configBuilder[F[_]]: ConfigBuilder[F, Config[F]] = new ConfigBuilder[F, Config[F]] {

    def appName: String = "forge"

    def moduleName: String = "server"

    def default(implicit F: Sync[F]) : F[Config[F]] = {
      F.delay({
        TypeSafeConfigFactory.defaultReference().as[Config[F]]("forge.server")
      })
    }

    def parseArguments(arguments: List[String])(implicit F: Sync[F]): F[Config[F]] = {
      val builder = OParser.builder[Config[F]]
      import builder._

      val parser = OParser.sequence(
        opt[Int]("port")
          .action({ (port, config) =>
            config.copy(port = Some(port))
          })
          .optional(),
        opt[Path]("base-folder-path")
          .action({ (baseFolderPath, config) =>
            config.copy(baseFolderPath = Some(baseFolderPath))
          })
          .optional()
      )

      F.delay(OParser.parse(parser, arguments, Config.empty[F])).flatMap(_.liftTo[F](new Exception("Unable to parse")))
    }

    def parseFile(filePath: Path)(implicit F: Sync[F]): F[Config[F]] = {
      F.delay(TypeSafeConfigFactory.parseFile(filePath.toFile).as[Config[F]])
    }

  }

}
