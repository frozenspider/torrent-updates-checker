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

/**
 * @author FS
 */
class TorrentUpdatesCheckerWebUi extends Logging {
  import TorrentUpdatesCheckerWebUi._

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
    val listEndpoint: Endpoint[JsonCcList] =
      get(/) {
        Future {
          ???
          Ok(JsonCcList(Seq.empty))
        }
      }

    val addEndpoint: Endpoint[JsonCcList] =
      post(/ :: path[String] :: param("url")) { (alias: String, url: String) =>
        Future {
          ???
          Ok(JsonCcList(Seq.empty))
        }
      }

    val removeEndpoint: Endpoint[JsonCcList] =
      delete(/ :: path[String]) { (alias: String) =>
        Future {
          ???
          Ok(JsonCcList(Seq.empty))
        }
      }

    "entries" :: (listEndpoint :+: addEndpoint :+: removeEndpoint)
  }

  // Service for different content-types
  // See https://github.com/finagle/finch/pull/794
  private val combinedSevice: Service[Request, Response] =
    Bootstrap
      .serve[Application.Json](entriesEndpoint)
      .serve[Text.Html](htmlEndpoint)
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
}

object TorrentUpdatesCheckerWebUi {
  case class JsonCcEntry(alias: String, url: String)

  case class JsonCcList(entries: Seq[JsonCcEntry])
}
