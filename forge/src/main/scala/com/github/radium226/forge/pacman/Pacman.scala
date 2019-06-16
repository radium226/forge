package com.github.radium226.forge.pacman

import java.nio.file.Path

import cats._
import cats.implicits._
import cats.effect._
import com.github.radium226
import com.github.radium226.forge.{AbsoluteFilePathVar, User}
import org.http4s.AuthedRoutes
import org.http4s._
import org.http4s.dsl._
import org.http4s.dsl.io._
import radium226.io._

import scala.concurrent.ExecutionContext

object Pacman {

  def makeRoutes[F[_]](storeFolderPath: Path)(implicit F: Async[F], contextShift: ContextShift[F]): AuthedRoutes[User, F] = {
    val filePathVar = AbsoluteFilePathVar(storeFolderPath)
    val repoVar = RepoVar(storeFolderPath)
    AuthedRoutes.of[User, F] {
      case (request @ PUT -> Root / repoVar(repo)) as _ =>
        for {
          pkgFilePath <- upload[F](request, ".pkg.tar.xz")
          _           <- repo.addPkg[F](pkgFilePath)
        } yield Response[F](status = Ok)

      case GET -> Root / repoVar(repo) / "os" / ArchVar(arch) / fileName as _ if fileName endsWith ".db" =>
        println(s"repo=${repo}")
        println(s"arch=${arch}")
        F.pure(Response[F](status = Ok, body = DB.open(repo, arch)))

      case GET -> filePathVar(filePath) as _ =>
        println(s"filePath=${filePath}")
        F.pure(Response[F](status = Ok, body = fs2.io.file.readAll[F](filePath, ExecutionContext.global, 1024)))
    }
  }

}
