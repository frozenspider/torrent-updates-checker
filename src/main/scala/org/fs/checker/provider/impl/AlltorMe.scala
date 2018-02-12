package org.fs.checker.provider.impl

import java.net.URL

import scala.util.Try

import org.apache.http.client.HttpClient
import org.fs.checker.provider.ConfiguredProvider
import org.fs.checker.provider.GenProvider
import org.fs.checker.provider.RawProvider
import org.fs.checker.utility.DurationParser
import org.fs.checker.utility.ResponseBodyDecoder
import org.fs.utility.web.Imports._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.Config

class AlltorMeBase extends GenProvider {
  override val prettyName: String = "alltor.me"

  override val providerKey: String = "alltor"

  val timeoutMs: Int = 60 * 1000

  override def recognizeUrl(url: String): Boolean = {
    Try("alltor.me" == (new URL(url)).getHost).getOrElse(false)
  }
}

class AlltorMe(httpClient: HttpClient) extends AlltorMeBase with ConfiguredProvider {
  override def fetch(url: String): String = {
    val resp = httpClient.request(GET(url).addTimeout(timeoutMs))
    ResponseBodyDecoder.bodyToString(resp)
  }

  override def parseDateLastUpdated(content: String): DateTime = {
    val body = parseElement(content) \ "body"
    val downloadTable = body \\ "table" filterByClass "dl_list"
    val infoNode = (downloadTable \ "tr")(1) \ "td"
    val lastUpdatedNode = (infoNode \ "b")(1)
    val lastUpdatedString = lastUpdatedNode.trimmedText
    val duration = DurationParser.parse(lastUpdatedString)
    val result = DateTime.now - duration
    result
  }
}

object AlltorMe extends AlltorMeBase with RawProvider {
  override def withConfig(config: Config): AlltorMe = {
    val (httpClient, cookieStore) = simpleClientWithStore()
    val authReq = POST("https://alltor.me/login.php")
      .addTimeout(timeoutMs)
      .addParameters(Map(
        "login_username" -> config.getString("login"),
        "login_password" -> config.getString("password"),
        "autologin" -> "on",
        "login" -> "Вход",
        "redirect" -> "index.php"
      ))
    val response = httpClient.request(authReq)
    if (response.code != 302) {
      throw new IllegalArgumentException(s"Failed to auth, got code ${response.code}, content ${response.bodyString}")
    }
    new AlltorMe(httpClient)
  }
}
