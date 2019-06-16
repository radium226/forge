package com.github.radium226.forge.maven

import cats._
import cats.implicits._
import cats.effect._
import java.nio.file.{Files, Path}

import com.github.radium226.forge.{AbsoluteFilePathVar, User}
import org.http4s._
import org.http4s.dsl.io._
import com.github.radium226.io._

import scala.concurrent.ExecutionContext

object Maven {

  val chunkSize = 1024

  def makeRoutes[F[_]](folderPath: Path)(implicit F: Sync[F], contextShift: ContextShift[F]): AuthedRoutes[User, F] = {
    val filePathVar = AbsoluteFilePathVar(folderPath)

    AuthedRoutes.of[User, F] {
      case (request @ PUT -> filePathVar(filePath)) as _ =>
        for {
          _        <- upload[F](request, filePath)
          response  = Response[F](Ok)
        } yield response

      case GET -> Root as user =>
        F.pure(Response[F](Ok).withEntity(s"Hello ${user}! "))

      case GET -> filePathVar(filePath) as _ =>
        F.delay(Files.exists(filePath)).map({
          case true =>
            Response[F](status = Ok, body = fs2.io.file.readAll[F](filePath, ExecutionContext.global, chunkSize))

          case false =>
            Response[F](NotFound)
        })
    }
  }

}
