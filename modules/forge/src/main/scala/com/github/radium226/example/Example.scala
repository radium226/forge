package com.github.radium226.example

import java.nio.file.Paths

import cats.data.ReaderT
import cats.{Applicative, Functor}
import cats.effect._
import cats.implicits._
import fs2._
import org.http4s.{Response, Status}

import scala.concurrent.ExecutionContext
import cats.implicits._

object Example extends IOApp {

  object EmailService {

    def start[F[_]](port: Int)(implicit F: Sync[F]): F[EmailService[F]] = F.delay(println("Starting Email Service")).as(EmailService(port))

    def stop[F[_]](emailService: EmailService[F])(implicit F: Sync[F]): F[Unit] = F.delay(println("Stopping Email Service"))

    def resource[F[_]](port: Int)(implicit F: Sync[F]): Resource[F, EmailService[F]] = {
      Resource.make[F, EmailService[F]](EmailService.start[F](port))({ emailService => EmailService.stop[F](emailService) })
    }

  }

  object SMSService {

    def start[F[_]](implicit F: Sync[F]): F[SMSService[F]] = F.delay(println("Starting SMS Service")).as(SMSService())

    def stop[F[_]](emailService: SMSService[F])(implicit F: Sync[F]): F[Unit] = F.delay(println("Stopping SMS Service"))

    def resource[F[_]](implicit F: Sync[F]): Resource[F, SMSService[F]] = {
      Resource.make[F, SMSService[F]](SMSService.start[F])({ smsService => SMSService.stop[F](smsService) })
    }

  }

  case class EmailService[F[_]](port: Int) {

    def sendEmail(message: String)(implicit F: Sync[F]): F[Unit] = F.delay(println(s"Sending Email with message=${message}"))

  }

  case class SMSService[F[_]]() {

    def sendSMS(message: String)(implicit F: Sync[F]): F[Unit] = {
      F.delay(println(s"Sending SMS with message=${message}"))
    }

  }

  case class Env[F[_]](emailService: EmailService[F], smsService: SMSService[F])

  type App[F[_]] = ReaderT[F, Env[F], ExitCode]

  object App {

    def apply[F[_]](f: Env[F] => F[ExitCode]): App[F] = ReaderT(f)

  }

  override def run(arguments: List[String]): IO[ExitCode] = {
    (for {
      emailService <- EmailService.resource[IO](123)
      smsService   <- SMSService.resource[IO]
    } yield (emailService, smsService)).map({ case (emailService, smsService) => Env[IO](emailService, smsService) }).use({ env =>
      app.run(env)
    })
  }

  def subApp1: App[IO] = App[IO]({ env =>
    env.smsService.sendSMS("coucou").as(ExitCode.Success)
  })

  def subApp2: App[IO] = App[IO]({ env =>
    env.emailService.sendEmail("coucou").as(ExitCode.Success)
  })

  def app: App[IO] = for {
    _ <- subApp1
    _ <- subApp2
  } yield ExitCode.Success

}
