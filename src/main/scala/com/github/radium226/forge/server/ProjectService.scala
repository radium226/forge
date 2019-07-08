package com.github.radium226.forge.server

import java.nio.file.{Files, Path, Paths}

import cats._
import cats.effect._
import cats.implicits._
import fs2._
import com.github.radium226.io._
import com.google.common.io.MoreFiles

import com.github.radium226.system.execute._

import scala.concurrent.ExecutionContext

case class Job[F[_]](project: Project[F], index: Index) {

  def output(implicit F: Sync[F], contextShift: ContextShift[F]): F[Stream[F, Line]] = {
    val inputStreamF = F.delay(Files.newInputStream(outputFilePath))
    F.delay(fs2.io.readInputStream(inputStreamF, 1024, ExecutionContext.Implicits.global).through(fs2.text.utf8Decode[F]))
  }

  def outputFilePath: Path = {
    project.folderPath.resolve(s"jobs").resolve("%03d.log".format(index))
  }

}

object Job {

  def nextIndex[F[_]](project: Project[F])(implicit F: Sync[F]): F[Index] = {
    project.jobs
      .map({ jobs =>
        jobs
          .map(_.index)
          .maximumOption
          .map(_ + 1)
          .getOrElse(0)
      })
  }

}

case class Project[F[_]](folderPath: Path) {
  project =>

  def build(implicit F: Concurrent[F], contextShift: ContextShift[F]): F[Job[F]] = {
    for {
      index  <- Job.nextIndex[F](project)
      job     = Job[F](project, index)
      topic  <- Executor[F](workingFolderPath = Some(folderPath.resolve("source"))).execute("bash", "-c", "for i in $( seq 1 100 ); do sleep 1 ; echo ${i} ; done").topic
      stream  = topic
                  .subscribe(2)
                  .map({ t => println(t) ; t })
                  .collect({ case Line(_, content, _) =>
                    content
                  })
                  .through(fs2.text.utf8Encode[F])
                  .through(
                    fs2.io.writeOutputStream[F](F.delay(Files.newOutputStream(job.outputFilePath)), ExecutionContext.Implicits.global)
                  )
      _      <- F.start(stream.compile.drain)
    } yield job
  }

  def jobs(implicit F: Sync[F]): F[List[Job[F]]] = {
    println(" --> " + folderPath.resolve("jobs"))
    listFiles[F](folderPath.resolve("jobs"))
      .map({ filePaths =>
        filePaths
          .map(MoreFiles.getNameWithoutExtension(_))
          .map(_.toInt)
          .map(Job[F](project, _))
      })
  }

}

object Project {

  def byName[F[_]](name: Name)(implicit F: Sync[F]): F[Project[F]] = {
    listFolders(Paths.get("/tmp/forge"))
      .flatMap(_.collectFirst({
        case folderPath if folderPath.getFileName == Paths.get(name) =>
          F.pure(Project[F](folderPath))
      }).getOrElse(F.raiseError(new Exception(s"Project ${name} not found"))))
  }

}
