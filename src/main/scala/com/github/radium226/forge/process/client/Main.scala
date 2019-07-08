package com.github.radium226.forge.process.client

import _root_.cats.effect._
import _root_.cats.implicits._
import _root_.org.http4s.client.blaze._
import _root_.org.http4s.client._

import _root_.scala.concurrent.ExecutionContext.Implicits.global
import _root_.com.github.radium226.system.execute._
import _root_.fs2._
import _root_.fs2.concurrent._
import _root_.java.net._
import _root_.java.net.http._
import _root_.java.util.concurrent._
import _root_.java.util.function._

import _root_.io.circe._
import _root_.io.circe.Decoder._
import _root_.io.circe.parser._


object Main extends IOApp {

  implicit def sourceDecoder: Decoder[Source] = new Decoder[Source] {
    override def apply(cursor: HCursor): Result[Source] = {
      cursor.as[String].flatMap({
        case "stdout" =>
          Right(StdOut)

        case "stderr" =>
          Right(StdErr)

        case other =>
          Left(DecodingFailure(s"Unable to parse ${other} as source", cursor.history))
      })
    }
  }

  implicit def lineDecoder: Decoder[Line] =  new Decoder[Line] {

    override def apply(cursor: HCursor): Result[Line] = {
      for {
        source  <- cursor.downField("source").as[Source]
        content <- cursor.downField("content").as[String]
        number  <- cursor.downField("number").as[Long]
      } yield Line(number, content, source)
    }
  }

  def lines[F[_]](uri: String)(implicit F: ConcurrentEffect[F]): Stream[F, Line] = {
    for {
      queue <- Stream.eval(Queue.unbounded[F, Either[Throwable, Line]])
      _     <-  Stream.eval({
        F.delay(HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create(uri), new WebSocket.Listener {

          override def onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage[_] = {
            F.runAsync({
              val line = decode[Line](data.toString)
              queue.enqueue1(line)
            })(_ => IO.unit ).unsafeRunSync()
            super.onText(webSocket, data, last)
          }

        }))
      })
      line  <- queue.dequeue.rethrow
    } yield line
  }


  override def run(arguments: List[String]): IO[ExitCode] = {
    lines[IO]("ws://localhost:8080/output").map(_.content).showLinesStdOut.compile.drain.as(ExitCode.Success)
  }

}
