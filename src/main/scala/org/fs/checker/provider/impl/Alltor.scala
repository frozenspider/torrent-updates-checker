package org.fs.checker.provider.impl

import java.net.URL
import java.nio.charset.StandardCharsets

import scala.util.Try

import org.apache.http.client.HttpClient
import org.fs.checker.dao.TorrentParseResult
import org.fs.checker.dumping.PageContentDumpService
import org.fs.checker.provider.ConfiguredProvider
import org.fs.checker.provider.GenProvider
import org.fs.checker.provider.RawProvider
import org.fs.checker.utility.DurationParser
import org.fs.checker.utility.ResponseBodyDecoder
import org.fs.utility.web.Imports._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.Config

class AlltorBase extends GenProvider {
  override val prettyName: String = "alltor.me"

  override val providerKey: String = "alltor"

  override def recognizeUrl(url: String): Boolean = {
    Try("alltor.me" == (new URL(url)).getHost).getOrElse(false)
  }
}

class Alltor(httpClient: HttpClient, override val dumpService: PageContentDumpService) extends AlltorBase with ConfiguredProvider {
  override def fetch(url: String): String = {
    val resp = httpClient.request(GET(url).addTimeout(timeoutMs))
    ResponseBodyDecoder.bodyToString(resp)
  }

  override def parseDateLastUpdated(content: String): TorrentParseResult = {
    val body = parseElement(content) \ "body"
    val downloadTable = body \\ "table" filterByClass "dl_list"
    if (downloadTable.isEmpty) {
      val msgTable = body \\ "table" filterByClass "forumline" filterByClass "message"
      val msgTd = msgTable \\ "td"
      val msg: String = msgTd(0).trimmedText
      if (msg contains "не существует") {
        TorrentParseResult.Failure.NotFound
      } else {
        throw new IllegalStateException(s"Unexpected service message error")
      }
    } else {
      val infoNode = (downloadTable \ "tr")(1) \ "td"
      val lastUpdatedNode = (infoNode \ "b")(1)
      val lastUpdatedString = lastUpdatedNode.trimmedText
      val duration = DurationParser.parse(lastUpdatedString)
      val result = DateTime.now - duration
      TorrentParseResult.Success(result)
    }
  }
}

object Alltor extends AlltorBase with RawProvider {
  override val requiresAuth = true

  override def withConfig(config: Config, dumpService: PageContentDumpService): Alltor = {
    val (httpClient, cookieStore) = simpleClientWithStore()
    val authReq = POST("https://alltor.me/login.php")
      .addTimeout(timeoutMs)
      .setCharset(StandardCharsets.UTF_8) // Needed for transmitting non-ASCII parameters
      .addParameters(Map(
        "login_username" -> config.getString("login"),
        "login_password" -> config.getString("password"),
        "autologin" -> "1",
        "login" -> "Вход"
      ))
    val response = httpClient.request(authReq)
    if (response.code != 302) {
      dumpService.dump(response.bodyString, providerKey)
      throw new IllegalArgumentException(s"Failed to auth, got code ${response.code}, content dumped to file")
    }
    new Alltor(httpClient, dumpService)
  }
}
