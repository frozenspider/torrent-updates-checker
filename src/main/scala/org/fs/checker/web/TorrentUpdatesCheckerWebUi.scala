package org.fs.checker.web

import scala.io.Source

import com.twitter.finagle.Http
import com.twitter.finagle.ListeningServer
import com.twitter.finagle.Service
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.io.Buf
import com.twitter.util.Future

import io.circe.generic.auto._
import io.finch._
import io.finch.circe._

import org.slf4s.Logging
import org.fs.checker.dao.TorrentDaoService
import org.fs.checker.dao.TorrentEntry
import com.twitter.finagle.http.Message
import java.nio.charset.Charset

/**
 * @author FS
 */
class TorrentUpdatesCheckerWebUi(daoService: TorrentDaoService) extends Logging {

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
      Future {
        val source = readResource("root.html")
        Ok(Buf.Utf8(source))
      }
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

  private val exceptionFilter = new ExceptionInterceptingFilter

  // Service for different content-types
  // See https://github.com/finagle/finch/pull/794
  private val combinedSevice: Service[Request, Response] =
    exceptionFilter andThen Bootstrap
      .serve[Application.Json](entriesEndpoint.withCharset(Utf8))
      .serve[Text.Html](htmlEndpoint.withCharset(Utf8))
      .toService

  //
  // Helpers
  //

  private def readResource(path: String): String = {
    val resource = Source.fromResource(path)
    try {
      resource.mkString
    } finally {
      resource.close()
    }
  }

  private def Utf8 = Charset.forName("UTF-8")
}
