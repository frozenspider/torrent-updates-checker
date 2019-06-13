package org.fs.checker.provider.impl

import java.net.URL
import java.nio.charset.StandardCharsets

import scala.util.Try

import org.apache.http.client.HttpClient
import org.fs.checker.dumping.PageContentDumpService
import org.fs.checker.provider.ConfiguredProvider
import org.fs.checker.provider.GenProvider
import org.fs.checker.provider.RawProvider
import org.fs.checker.utility.MonthNameParser
import org.fs.checker.utility.ResponseBodyDecoder
import org.fs.utility.web.Imports._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.Config
import org.joda.time.format.DateTimeFormatter

class BookTrackerBase extends GenProvider {
  override val prettyName: String = "booktracker.org"

  override val providerKey: String = "booktracker"

  override def recognizeUrl(url: String): Boolean = {
    Try("booktracker.org" == (new URL(url)).getHost).getOrElse(false)
  }
}

class BookTracker(httpClient: HttpClient, override val dumpService: PageContentDumpService) extends BookTrackerBase with ConfiguredProvider {
  private val dtf: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm")

  override def fetch(url: String): String = {
    val resp = httpClient.request(GET(url).addTimeout(timeoutMs))
    ResponseBodyDecoder.bodyToString(resp)
  }

  override def parseDateLastUpdated(content: String): DateTime = {
    // Format: 2018-07-08 07:10
    val body = parseElement(content) \ "body"
    val downloadTable = body \\ "table" filterByClass "attach"
    val lastUpdatedNode = ((downloadTable \ "tr" filterByClass "row1")(3) \ "td" \ "span").head
    val lastUpdatedString = lastUpdatedNode.trimmedText
    val lastUpdatedDateString = lastUpdatedString.split(" ").take(2).mkString(" ")
    dtf.parseDateTime(lastUpdatedDateString)
  }
}

object BookTracker extends BookTrackerBase with RawProvider {
  override val requiresAuth = true

  override def withConfig(config: Config, dumpService: PageContentDumpService): BookTracker = {
    val (httpClient, cookieStore) = simpleClientWithStore()
    val authReq = POST("https://booktracker.org/login.php")
      .addTimeout(timeoutMs)
      .setCharset(StandardCharsets.UTF_8) // Needed for transmitting non-ASCII parameters
      .addParameters(Map(
        "login_username" -> config.getString("login"),
        "login_password" -> config.getString("password"),
        "autologin" -> "on",
        "login" -> "Вход"
      ))
    val response = httpClient.request(authReq)
    if (response.code != 302) {
      dumpService.dump(response.bodyString, providerKey)
      throw new IllegalArgumentException(s"Failed to auth, got code ${response.code}, content dumped to file")
    }
    new BookTracker(httpClient, dumpService)
  }
}

