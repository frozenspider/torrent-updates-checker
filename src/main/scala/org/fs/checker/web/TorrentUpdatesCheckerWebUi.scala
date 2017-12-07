package org.fs.checker.web

import scala.io.Source

import com.twitter.finagle.Http
import com.twitter.finagle.Service
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.io.Buf
import com.twitter.util.Await
import com.twitter.util.Future

import io.circe.generic.auto._
import io.finch._
import io.finch.circe._

/**
 * @author FS
 */
class TorrentUpdatesCheckerWebUi {
  import TorrentUpdatesCheckerWebUi._

  def start(port: Int) {
    val server = Http.server
      .withLabel("torrent-updates-checker-web-ui")
      .withStreaming(enabled = true)
      .serve(s"localhost:$port", combinedSevice)

    Await.ready(server)
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
    val string = resource.mkString
    resource.close()
    string
  }
}

object TorrentUpdatesCheckerWebUi extends scala.App {
  (new TorrentUpdatesCheckerWebUi).start(8100)

  case class JsonCcEntry(alias: String, url: String)

  case class JsonCcList(entries: Seq[JsonCcEntry])
}
