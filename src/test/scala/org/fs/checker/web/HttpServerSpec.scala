package org.fs.checker.web

import scala.reflect.io.File

import org.fs.checker.TestHelper
import org.fs.checker.dao.TorrentEntry
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

import com.twitter.finagle.http.Method
import com.twitter.finagle.http.Request
import com.twitter.io.Buf
import com.twitter.util.Return
import com.twitter.util.Throw

import io.finch._
import io.finch.circe._
import shapeless._
import java.net.URLEncoder

@RunWith(classOf[JUnitRunner])
class HttpServerSpec
    extends FlatSpec
    with TestHelper {

  private type TE = TorrentEntry
  private val TE = TorrentEntry

  private val logFile: File = {
    val f = File.makeTemp()
    f.writeAll((0 to 9).mkString("\n"))
    f
  }

  private val server = new HttpServer(daoServiceMock, logFile)

  behavior of "finch http server"

  it should "serve root page" in {
    assert(requestTextChecked(server.rootEndpoint, "/").startsWith("<html>"))
  }

  it should "serve static resources" in {
    assert(requestTextChecked(server.staticEndpoint, "/static/non-existing-file", 404) === "")
    assert(requestTextChecked(server.staticEndpoint, "/static/root.html").startsWith("<html>"))
  }

  it should "serve log" in {
    assert(requestTextChecked(server.logEndpoint, "/log") === "0\n1\n2\n3\n4\n5\n6\n7\n8\n9")
    assert(requestTextChecked(server.logEndpoint, "/log?tailLines=1") === "9")
    assert(requestTextChecked(server.logEndpoint, "/log?tailLines=3") === "7\n8\n9")
  }

  it should "process CRUD operations for entries" in {
    def req(subUrl: String, method: Method) = {
      requestChecked(server.entriesEndpoint, "/entries" + subUrl, method)
    }
    assert(req("", Method.Get) === Some(Nil))
    assert(req("/a1?url=u1", Method.Post) === Some(TE("a1", "u1") :: Nil))
    assert(req("/a1?url=u2", Method.Post) === None)
    assert(req("/a2?url=u1", Method.Post) === None)
    assert(req("/a2?url=u2", Method.Post) === Some(TE("a1", "u1") :: TE("a2", "u2") :: Nil))
    assert(req("", Method.Get) === Some(TE("a1", "u1") :: TE("a2", "u2") :: Nil))
    assert(req("/a1", Method.Delete) === Some(TE("a2", "u2") :: Nil))
    assert(req("/a1", Method.Delete) === Some(TE("a2", "u2") :: Nil))
    assert(req("/a2", Method.Delete) === Some(Nil))
    assert(req("", Method.Get) === Some(Nil))
  }

  it should "properly handle entry with non-URL characters" in {
    def req(subUrl: String, method: Method) = {
      requestChecked(server.entriesEndpoint, "/entries" + subUrl, method)
    }
    val string = s"a${nonStandardAllowedChars}b"
    val encoded = URLEncoder.encode(string, utf8.name)
    assert(req(s"/$encoded?url=$encoded", Method.Post) === Some(TE(string, string) :: Nil))
    assert(req(s"/$encoded", Method.Delete) === Some(Nil))
  }

  /** Issue request to an endpoint, verify that it's 200 and return the content */
  private def requestChecked(
      endpoint:     Endpoint[_],
      url:          String,
      method:       Method      = Method.Get,
      expectedCode: Int         = 200
  ): Option[Any] = {
    val input = Input.fromRequest(Request(method, Request.queryString(url)))
    val resultOptionTry = endpoint(input).awaitOutput()
    assert(resultOptionTry.isInstanceOf[Some[_]])
    val Some(resultTry) = resultOptionTry
    resultTry match {
      case Return(result) =>
        assert(result.status.code === expectedCode)
        try {
          Some(unwrapFirst(result.value))
        } catch {
          // Server returned no output
          case ex: IllegalStateException => None
        }
      case Throw(ex) =>
        None
    }
  }

  /** Issue request to a text endpoint, verify that it's 200 and return the content as UTF-8 string */
  private def requestTextChecked(endpoint: Endpoint[Buf], url: String, expectedCode: Int = 200): String = {
    val valueOption = requestChecked(endpoint, url, Method.Get, expectedCode)
    valueOption map (v => Buf.decodeString(v.asInstanceOf[Buf], utf8)) getOrElse ""
  }

  /** Disregard type information to unwrap shapeless list, looking for first non-empty element */
  private def unwrapFirst(c: Any): Any = {
    val inner = c match {
      case Inl(v) => v
      case Inr(v) => v
      case v      => v
    }
    inner match {
      case c: Coproduct => unwrapFirst(c)
      case c            => c
    }
  }
}
