package com.github.radium226.forge.process.server

import cats._
import cats.effect.concurrent._
import cats.effect._
import cats.implicits._
import com.github.radium226.system.execute._
import com.google.common.io.Resources
import fs2.concurrent._
import fs2._
import _root_.io.circe._
import _root_.io.circe.syntax._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.websocket._
import org.http4s.websocket.WebSocketFrame._
import org.http4s.server.blaze._
import org.http4s.implicits._
import org.http4s.server._
import org.http4s._

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  implicit def lineEncoder: Encoder[Line] = new Encoder[Line] {

    override def apply(line: Line): Json = {
      Json.obj(
        "source" -> Json.fromString(line.source match {
          case StdErr =>
            "stderr"

          case StdOut =>
            "stdout"
        }),
        "content" -> Json.fromString(line.content),
        "number" -> Json.fromLong(line.number)
      )
    }

  }

  def lines[F[_]](implicit F: Concurrent[F]): Pipe[F, Output, Line] = { stream =>
    stream
      .interruptWhen(stream.collect({
        case End =>
          true

        case _ =>
          false
      }))
      .collect({
        case line @ Line(_, _, _) =>
          line
      })
  }

  def show: Pipe[IO, Line, Unit] = _.flatMap({ line => Stream.eval(IO.delay(println(line))) })

  def makeRoutes(linesMVar: MVar[IO, List[Line]], linesTopic: Topic[IO, Output]): IO[HttpRoutes[IO]] = {
    IO.delay {
      HttpRoutes.of[IO] {
        case GET -> Root =>
          IO.pure(Response[IO](Ok).withBodyStream(fs2.io.readInputStream[IO](IO.delay(Resources.getResource("index.html").openStream()), 512, ExecutionContext.global)))

        case GET -> Root / "output" =>
          val send = (Stream.eval(linesMVar.read).flatMap(Stream.emits[IO, Line](_)) ++ linesTopic.subscribe(10).through(lines[IO])).zipWithPrevious/*.observe(show).*/.flatMap({
            case (Some(Line(beforeIndex, _, _)), line @ Line(index, content, _)) if beforeIndex == index =>
              Stream.empty

            case (_, line) =>
              Stream.emit[IO, Text](Text(line.asJson.toString()))

          })
          WebSocketBuilder[IO].build(send, _.drain)
      }
    }
  }

  override def run(arguments: List[String]): IO[ExitCode] = {

    val server = (for {
      linesMVar    <- MVar.of[IO, List[Line]](List.empty)
      linesTopic   <- Executor[IO].execute("bash", "-c", "for i in $( seq 1 100 ); do sleep 0.125; echo Hello... ${i} >&2 ; sleep 0.125 ; echo ...World ; done").topic
      linesFiber   <- linesTopic
                        .subscribe(1)
                        .through(lines)
                        .evalTap({ line =>
                          for {
                            lines <- linesMVar.take
                            _     <- linesMVar.put(lines :+ line)
                            //_      = println(lines)
                          } yield ()
                        })
                        .compile
                        .drain
                        .start
      routes       <- makeRoutes(linesMVar, linesTopic)

      serverBuilder = BlazeServerBuilder[IO]
          .withHttpApp(routes.orNotFound)
          .bindHttp(8080, "0.0.0.0")
      _            <- serverBuilder.resource.use(_ => IO.never)
      _            <- linesFiber.join
    } yield ())

    server.redeem({ throwable => throwable.printStackTrace(System.err) ; ExitCode.Error}, { _ => ExitCode.Success })
  }

}
