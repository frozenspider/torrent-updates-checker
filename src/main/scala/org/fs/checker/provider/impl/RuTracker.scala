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

class RuTrackerBase extends GenProvider {
  override val prettyName: String = "rutracker.org"

  override val providerKey: String = "rutracker"

  override def recognizeUrl(url: String): Boolean = {
    Try("rutracker.org" == (new URL(url)).getHost).getOrElse(false)
  }
}

class RuTracker(httpClient: HttpClient, override val dumpService: PageContentDumpService) extends RuTrackerBase with ConfiguredProvider {
  override def fetch(url: String): String = {
    val resp = httpClient.request(GET(url).addTimeout(timeoutMs))
    ResponseBodyDecoder.bodyToString(resp)
  }

  override def parseDateLastUpdated(content: String): DateTime = {
    // Format (bottom section): 10-Июн-19 20:39
    val body = parseElement(content) \ "body"
    val downloadTable = body \\ "table" filterByClass "attach"
    val lastUpdatedNode = ((downloadTable \ "tr" filterByClass "row1")(0) \\ "li").head
    val lastUpdatedString = lastUpdatedNode.trimmedText
    val split = lastUpdatedString.split("[- :]")
    assert(split.size == 5, "Unexpected page format!")
    val monthInt = MonthNameParser.parse(split(1))
    new DateTime(2000 + split(2).toInt, monthInt, split(0).toInt, split(3).toInt, split(4).toInt, 0)
  }
}

object RuTracker extends RuTrackerBase with RawProvider {
  override val requiresAuth = true

  override def withConfig(config: Config, dumpService: PageContentDumpService): RuTracker = {
    val (httpClient, cookieStore) = simpleClientWithStore()
    val authReq = POST("https://rutracker.org/forum/login.php")
      .addTimeout(timeoutMs)
      .setCharset(StandardCharsets.UTF_8) // Needed for transmitting non-ASCII parameters
      .addParameters(Map(
        "login_username" -> config.getString("login"), //.replace(" ", "+"),
        "login_password" -> config.getString("password"),
        "login" -> "%E2%F5%EE%E4"
      ))
    val response = httpClient.request(authReq)
    if (response.code == 200 && response.bodyString.contains("Форум временно отключен")) {
      throw new IllegalStateException(s"Tracker is down for maintenance")
    } else if (response.code != 302) {
      dumpService.dump(response.bodyString, providerKey)
      throw new IllegalArgumentException(s"Failed to auth, got code ${response.code}, content dumped to file")
    }
    new RuTracker(httpClient, dumpService)
  }
}

