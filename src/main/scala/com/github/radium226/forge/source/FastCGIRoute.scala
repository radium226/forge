package com.github.radium226.forge.source

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.util

import org.http4s._
import cats.effect._
import cats.effect.concurrent._
import cats.implicits._
import org.jfastcgi.api.{RequestAdapter, ResponseAdapter}
import org.jfastcgi.client.{FastCGIHandler, FastCGIHandlerFactory}

import scala.concurrent.ExecutionContext
import java.util.{Collections => JavaCollections, Enumeration => JavaEnumeration, Map => JavaMap}

import org.http4s.headers.Location
import org.http4s.util.CaseInsensitiveString

import scala.collection.JavaConverters._


import org.http4s.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.server.blaze._
import org.http4s.implicits._
import org.http4s.server._
import org.http4s._
import org.http4s.dsl.io.{Path => Http4sPath}


class Http4sRequestAdapter[F[_]](request: Request[F], byteArrayInputStream: ByteArrayInputStream)(implicit concurrent: Concurrent[F]) extends RequestAdapter {

  override def getInputStream: InputStream = {
    byteArrayInputStream
  }

  override def getRequestURI: String = {
    request.pathInfo
  }

  override def getMethod: String = {
    request.method.name
  }

  override def getServerName: String = {
    "0.0.0.0"
  }

  override def getServerPort: Int = {
    8080
  }

  override def getRemoteAddr: String = {
    "0.0.0.0"
  }

  override def getRemoteHost: String = {
    "0.0.0.0"
  }

  override def getRemoteUser: String = {
    null
  }

  override def getAuthType: String = {
    null
  }

  override def getProtocol: String = {
    "http"
  }

  override def getQueryString: String = {
    request.queryString
  }

  override def getContentType: String = {
    request
      .contentType
      .map(_.name.toString())
      .getOrElse(null)
  }

  override def getServletPath: String = {
    "/"
  }

  override def getPathInfo: String = {
    "/"
  }

  override def getRealPath(s: String): String = {
    "/"
  }

  override def getContextPath: String = {
    "/"
  }

  override def getContentLength: Int = {
    request.contentLength.map(_.toInt).getOrElse(0)
  }

  override def getHeaderNames: JavaEnumeration[String] = {
    JavaCollections.enumeration(request.headers.toList.map(_.name).map(_.toString()).asJava)
  }

  override def getHeader(headerName: String): String = {
    request
      .headers
      .get(CaseInsensitiveString(headerName))
      .map(_.value)
      .getOrElse(null)
  }
}

class Http4sResponseAdapter[F[_]]()(implicit F: Concurrent[F], contextShift: ContextShift[F]) extends ResponseAdapter {

  private var response: Response[F] = Response[F]()

  val byteArrayOutputStream = new ByteArrayOutputStream()

  override def sendError(errorCode: Int): Unit = {
    println(s"errorCode=${errorCode}")
    response = this.response.withStatus(Status.apply(errorCode))
  }

  override def setStatus(statusCode: Int): Unit = {
    response = this.response.withStatus(Status.apply(statusCode))
  }

  override def sendRedirect(url: String): Unit = {
    response = response.withStatus(Status.TemporaryRedirect).withHeaders(Location(Uri.unsafeFromString(url)))
  }

  override def addHeader(key: String, value: String): Unit = {
    response = response.withHeaders(Header(key, value))
  }

  override def getOutputStream: OutputStream = {
    byteArrayOutputStream
  }

  def http4sResponse: Response[F] = {
    response.withBodyStream(fs2.io.readInputStream(F.delay(new ByteArrayInputStream(byteArrayOutputStream.toByteArray)), 32, ExecutionContext.global))
  }

}

object FastCGIRoute {

  def of[F[_]]()(implicit F: Concurrent[F], contextShift: ContextShift[F]): F[HttpRoutes[F]] = {
    for {
      fastCGIHandler     <- F.delay({
        val fastCGIHandler = FastCGIHandlerFactory.create(JavaMap.of("server-address", "unix:///tmp/fcgi.sock"))
        fastCGIHandler.startProcess("/home/adrien.besnard/Personal/Projects/http4s-fastcgi/fastcgi-app.py -process 4 -socket /tmp/fcgi.sock")
        fastCGIHandler
      })
    } yield HttpRoutes.of[F]({
      case request =>
        val byteArrayOutputStream = new ByteArrayOutputStream()
        request
          .body
          .through(
            fs2.io.writeOutputStream[F](F.pure(byteArrayOutputStream), ExecutionContext.global, true)
          )
          .compile
          .drain
          .as((request, byteArrayOutputStream))
          .flatMap({ case (request, byteArrayOutputStream) =>
            println(s"byteArrayOutputStream=${byteArrayOutputStream}")
            F.delay({
              val byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray)
              val requestAdapter = new Http4sRequestAdapter[F](request, byteArrayInputStream)
              val responseAdapter = new Http4sResponseAdapter[F]()
              fastCGIHandler.service(requestAdapter, responseAdapter)
              val response = responseAdapter.http4sResponse
              println(response)
              response
            })
          })
    })
  }

}
