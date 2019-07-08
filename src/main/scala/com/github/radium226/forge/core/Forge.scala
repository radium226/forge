package com.github.radium226.forge.core

import java.nio.file._

import fs2._
import fs2.concurrent._

import cats._
import cats.effect._
import cats.implicits._

import com.github.radium226.forge.git._

import com.github.radium226.io._


case class Build[F[_]]()

trait Status[F[_]]

case class Succeed[F[_]]() extends Status[F]

case class Failed[F[_]]() extends Status[F]


case class Project[F[_]](folderPath: Path, repo: Repo[F]) {

  def build[F[_]](commit: Commit[F])(implicit F: Sync[F]): F[Status[F]] = {
    F.delay(println(s"build(commit=${commit})")).as(Succeed[F]())
  }

}

sealed trait Strategy

case object Init extends Strategy

case object Clone extends Strategy

class ProjectService[F[_]](baseFolderPath: Path, polling: Polling[F], refreshQueue: Queue[F, Boolean]) {

  def trashFolderPath: Path = baseFolderPath.resolve(".trash")

  def clone(name: String, uri: URI)(implicit F: Concurrent[F], timer: Timer[F]): F[Project[F]] = {
    for {
      folderPath <- folders.create[F](baseFolderPath.resolve(name))
      repo       <- Git[F](folderPath, polling).cloneRepo(uri)
      _          <- refreshQueue.enqueue1(true)
    } yield Project[F](folderPath, repo)
  }

  def trash(project: Project[F])(implicit F: Sync[F]): F[Unit] = {
    folders.create(trashFolderPath) *> move[F](project.folderPath, trashFolderPath).void
  }

  def enumerate(implicit F: Concurrent[F], timer: Timer[F]): F[List[Project[F]]] = {
    folders.list[F](baseFolderPath, recursive = false)
      .map(_.filter(_ != trashFolderPath))
      .flatMap(_.traverse({ folderPath =>
        Git[F](folderPath, polling).useRepo.map({ repo =>
          Project[F](folderPath, repo)
        })
      }))
  }

  def emptyTrash(implicit F: Sync[F]): F[Unit] = {
    folders.deleteContent[F](trashFolderPath)
  }

  def triggers(implicit F: Concurrent[F], timer: Timer[F]): Stream[F, Trigger[F]] = {
    Stream.eval(enumerate).flatMap({ projects =>
      println(s"projects=${projects}")
      projects.map({ project =>
        project.repo.commits.map({ commit =>
          val trigger = Trigger[F](project, commit)
          println(s"trigger=${trigger}")
          trigger
        })
      })
      .foldLeft[Stream[F, Trigger[F]]](Stream.empty)(_ merge _)
    })
  }

}

object Project {

  def inFolder[F[_]](folderPath: Path, polling: Polling[F])(implicit F: Concurrent[F], timer: Timer[F]): F[Project[F]] = {
    Git[F](folderPath, polling).useRepo.map({ repo => Project[F](folderPath, repo) })
  }

  def service[F[_]](baseFolderPath: Path, polling: Polling[F], refreshQueue: Queue[F, Boolean])(implicit F: Sync[F]): Resource[F, ProjectService[F]] = {
    Resource.liftF(F.pure(new ProjectService[F](baseFolderPath, polling, refreshQueue)))
  }

}

case class Trigger[F[_]](project: Project[F], commit: Commit[F])

object Build {

  def service[F[_]](implicit F: Sync[F]): Resource[F, BuildService[F]] = {
    Resource.liftF[F, BuildService[F]](F.pure(new BuildService[F]))
  }

}

class BuildService[F[_]] {

  def build(implicit F: Sync[F]): Pipe[F, Trigger[F], Status[F]] = { triggers =>
    triggers.evalMap({ trigger =>
      println("BuildService.build")
      trigger.project.build(trigger.commit)
    })
  }

}

case class ForgeBuilder[F[_]](baseFolderPath: Path) {

  def resource(implicit F: Concurrent[F], timer: Timer[F], contextShift: ContextShift[F]): Resource[F, Forge[F]] = {
    Resource.make[F, Forge[F]](start)(_.stop)
  }

  def start(implicit F: Concurrent[F], timer: Timer[F], contextShift: ContextShift[F]): F[Forge[F]] = {
    (for {
      polling        <- Resource.liftF(Polling[F])
      refreshQueue   <- Resource.liftF(Queue.unbounded[F, Boolean])
      projectService <- Project.service[F](baseFolderPath, polling, refreshQueue)
      buildService   <- Build.service[F]
    } yield (projectService, buildService, refreshQueue)).use({ case (projectService, buildService, refreshQueue) =>
      for {
        manualTriggerQueue <- Queue.unbounded[F, Trigger[F]]
        statusTopic        <- Topic[F, Option[Status[F]]](None)
        t                  <- triggers(projectService, manualTriggerQueue, refreshQueue)
        buildFiber         <- F.start(t.through(buildService.build).map(Some(_)).through(statusTopic.publish).compile.drain)
      } yield new Forge(projectService, manualTriggerQueue, buildFiber, statusTopic)
    })
  }

  def triggers(projectService: ProjectService[F], manualTriggerQueue: Queue[F, Trigger[F]], refreshQueue: Queue[F, Boolean])(implicit F: Concurrent[F], timer: Timer[F], contextShift: ContextShift[F]): F[Stream[F, Trigger[F]]] = {
    def peek: Pipe[F, Trigger[F], Unit] = {
      _.evalMap({ trigger =>
        F.delay(println(s"trigger=${trigger}"))
      })
    }

    def go(triggerQueue: Queue[F, Trigger[F]]): F[Unit] = {
      println("go()")
      for {
        trigger  <- projectService.triggers.interruptWhen(refreshQueue.dequeue).through(triggerQueue.enqueue).compile.drain
        _        <- go(triggerQueue)
      } yield ()
    }

    for {
      gitTriggerQueue <- Queue.unbounded[F, Trigger[F]]
      _               <- F.start(go(gitTriggerQueue))
    } yield gitTriggerQueue.dequeue
  }

}

object ForgeBuilder {

  def apply[F[_]](baseFolderPath: Path): ForgeBuilder[F] = {
    new ForgeBuilder[F](baseFolderPath)
  }

}

class Forge[F[_]](projectService: ProjectService[F], manualTriggerQueue: Queue[F, Trigger[F]], buildFiber: Fiber[F, Unit], statusTopic: StatusTopic[F]) {

  def projects: ProjectService[F] = projectService

  def stop: F[Unit] = {
    buildFiber.cancel
  }

  def await: F[Unit] = {
    buildFiber.join
  }

  def trigger(project: Project[F], commit: Commit[F]): F[Unit] = {
    manualTriggerQueue.enqueue1(Trigger[F](project, commit))
  }

  def emptyTrash(implicit F: Sync[F]): F[Unit] = {
    projectService.emptyTrash
  }

  def statuses: Stream[F, Status[F]] = {
    statusTopic.subscribe(1).unNone
  }

}
