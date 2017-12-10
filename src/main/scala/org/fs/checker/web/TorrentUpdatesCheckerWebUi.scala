package org.fs.checker.web

import java.io.{ File => JFile }
import java.nio.charset.Charset

import scala.reflect.io.File

import org.fs.checker.dao.TorrentDaoService
import org.fs.checker.dao.TorrentEntry
import org.slf4s.Logging

import com.twitter.finagle.Http
import com.twitter.finagle.ListeningServer
import com.twitter.finagle.Service
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status
import com.twitter.io.Buf
import com.twitter.io.Reader
import com.twitter.util.Future

import io.circe.generic.auto._
import io.finch._
import io.finch.circe._

/**
 * @author FS
 */
class TorrentUpdatesCheckerWebUi(daoService: TorrentDaoService, logFile: File) extends Logging {

  def start(port: Int): ListeningServer = {
    require((1 to 65535) contains port, "Web UI server port should be between 1 and 65535")

    val server = Http.server
      .withLabel("torrent-updates-checker-web-ui")
      .withStreaming(enabled = true)
      .serve(s"localhost:$port", combinedSevice)

    log.info(s"Web UI server started at port $port")
    server
  }

  //
  // Endpoints
  //

  private val htmlEndpoint: Endpoint[Buf] =
    get(/) {
      serveResource(Seq("root.html"))
    }

  private val logEndpoint: Endpoint[Buf] =
    get("log") {
      val reader: Reader = Reader.fromFile(logFile.jfile)
      Reader.readAll(reader).map(Ok)
    }

  private val entriesEndpoint = {
    val listEndpoint: Endpoint[Seq[TorrentEntry]] =
      get(/) {
        Future {
          Ok(daoService.list)
        }
      }

    val addEndpoint: Endpoint[Seq[TorrentEntry]] =
      post(/ :: path[String] :: param("url")) { (alias: String, url: String) =>
        Future {
          Ok(daoService.add(TorrentEntry(alias, url)))
        }
      }

    val removeEndpoint: Endpoint[Seq[TorrentEntry]] =
      delete(/ :: path[String]) { (alias: String) =>
        Future {
          Ok(daoService.remove(alias))
        }
      }

    "entries" :: (listEndpoint :+: addEndpoint :+: removeEndpoint)
  }

  private val staticEndpoint: Endpoint[Buf] =
    get("static" :: paths[String])(serveResource _)

  private val exceptionFilter = new ExceptionInterceptingFilter

  // Service for different content-types
  // See https://github.com/finagle/finch/pull/794
  private val combinedSevice: Service[Request, Response] =
    exceptionFilter andThen Bootstrap
      .serve[Application.Json](entriesEndpoint.withCharset(Utf8))
      .serve[Text.Plain](logEndpoint.withCharset(Utf8))
      .serve[Text.Plain](staticEndpoint.withCharset(Utf8))
      .serve[Text.Html](htmlEndpoint.withCharset(Utf8))
      .toService

  //
  // Helpers
  //

  /**
   * Serve the local resource to user if it's represented by a valid relative path and does exist,
   * return 404 otherwise
   */
  private def serveResource(pathSegments: Seq[String]): Future[Output[Buf]] = {
    Future {
      (
        for {
          _ <- Some(true) // To get the chain going
          if isValidRelativePath(pathSegments)
          path = "static/" + pathSegments.mkString("/")
          classLoader = Thread.currentThread.getContextClassLoader
          //          stream <- Option(classLoader.getResourceAsStream(path))
          url <- Option(classLoader.getResource(path))
          file = new JFile(url.toURI)
          if file.isFile
          reader = Reader.fromFile(file)
          content = Reader.readAll(reader)
          response = content map Ok
        } yield response
      ) getOrElse Future.value(Output.empty(Status.NotFound))
    }.flatten
  }

  private def Utf8 = Charset.forName("UTF-8")

  private def isValidRelativePath(xs: Seq[String]): Boolean =
    // Not-so-concise, but more readable
    xs match {
      case _ if xs contains ".."           => false
      case _ if xs contains "."            => false
      case _ if xs exists (_ contains ":") => false
      case _ if xs exists (_ contains "?") => false
      case _                               => true
    }
}
