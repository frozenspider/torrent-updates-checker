package org.fs.checker.web

import java.net.URLDecoder
import java.nio.charset.Charset

import scala.annotation.tailrec
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
class HttpServer(daoService: TorrentDaoService, logFile: File) extends Logging {

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

  private[web] val rootEndpoint: Endpoint[Buf] =
    get(/) {
      serveResource(Seq("root.html"))
    }

  private[web] val logEndpoint: Endpoint[Buf] =
    get("log" :: paramOptionInt("tailLines")) { (tailLinesOption: Option[Int]) =>
      val reader: Reader = Reader.fromFile(logFile.jfile)
      Reader.readAll(reader).map(buf => {
        val resBuf = tailLinesOption filter (_ > 0) map (getTailLines(buf, _)) getOrElse buf
        Ok(resBuf)
      })
    }

  private[web] val entriesEndpoint = {
    val listEndpoint: Endpoint[Seq[TorrentEntry]] =
      get(/) {
        Future {
          Ok(daoService.list)
        }
      }

    val addEndpoint: Endpoint[Seq[TorrentEntry]] =
      post(/ :: encPath :: param("url")) { (alias: String, url: String) =>
        Future {
          daoService.add(TorrentEntry(alias, url))
          Ok(daoService.list)
        }
      }

    val removeEndpoint: Endpoint[Seq[TorrentEntry]] =
      delete(/ :: encPath) { (alias: String) =>
        Future {
          daoService.remove(alias)
          Ok(daoService.list)
        }
      }

    "entries" :: (listEndpoint :+: addEndpoint :+: removeEndpoint)
  }

  private[web] val staticEndpoint: Endpoint[Buf] =
    get("static" :: paths[String])(serveResource _)

  private val exceptionFilter = new ExceptionInterceptingFilter

  // Service for different content-types
  // See https://github.com/finagle/finch/pull/794
  private val combinedSevice: Service[Request, Response] =
    exceptionFilter andThen Bootstrap
      .serve[Application.Json](entriesEndpoint.withCharset(Utf8))
      .serve[Text.Plain](logEndpoint.withCharset(Utf8))
      .serve[Text.Plain](staticEndpoint.withCharset(Utf8))
      .serve[Text.Html](rootEndpoint.withCharset(Utf8))
      .toService

  //
  // Helpers
  //

  /** URL-encoded string path element */
  private def encPath: Endpoint[String] = {
    path[String].map(s => URLDecoder.decode(s, Utf8.name))
  }

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
          resource <- Option(classLoader.getResource(path))
          // User will see directory inside JAR as an empty file
          if resource.getProtocol == "file" || resource.getProtocol == "jar"
          stream = resource.openStream()
          reader = Reader.fromStream(stream)
          content = Reader.readAll(reader).respond(_ => stream.close())
          response = content map Ok
        } yield response
      ) getOrElse Future.value(Output.empty(Status.NotFound))
    }.flatten
  }

  private def Utf8 = Charset.forName("UTF-8")

  private def isValidRelativePath(xs: Seq[String]): Boolean =
    // Not-so-concise, but more readable
    xs match {
      case _ if xs contains ".."            => false
      case _ if xs contains "."             => false
      case _ if xs exists (_ contains ":")  => false
      case _ if xs exists (_ contains "?")  => false
      case _ if xs exists (_ contains "\\") => false
      case _                                => true
    }

  // Very use-case-specific, but generalization isn't needed for now
  private def paramOptionInt(name: String): Endpoint[Option[Int]] = {
    paramOption(name) map (_ map (_.toInt))
  }

  private def getTailLines(buf: Buf, linesNumber: Int): Buf = {
    // I wonder why Buf has no standard collection methods
    val lineBreak: Byte = "\n".charAt(0).toByte
    @tailrec
    def scanRecurse(idx: Int, remCount: Int): Buf =
      if (idx < 0) {
        buf
      } else if (remCount == 0) {
        val startIdx = math.min(idx + 2, buf.length - 1)
        buf.slice(startIdx, buf.length)
      } else {
        buf.get(idx) match {
          case `lineBreak` =>
            scanRecurse(idx - 1, remCount - 1)
          case _ =>
            scanRecurse(idx - 1, remCount)
        }
      }
    scanRecurse(buf.length - 1, linesNumber)
  }
}
