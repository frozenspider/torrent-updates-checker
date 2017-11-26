package org.fs.checker.provider.impl

import java.net.URL

import org.apache.http.client.HttpClient
import org.apache.http.impl.cookie.BasicClientCookie
import org.fs.checker.provider.Provider
import org.fs.checker.provider.ProviderCompanion
import org.fs.checker.utility.DurationParser
import org.fs.utility.web.Imports._

import com.github.nscala_time.time.Imports._
import com.typesafe.config.Config
import scala.util.Try

/**
 * @author FS
 */
class TasIxMe(httpClient: HttpClient) extends Provider {

  override val prettyName: String = "tas-ix.me"

  override def recognizeUrl(url: String): Boolean = {
    Try(Seq("tas-ix.me", "tas-ix.net") contains (new URL(url)).getHost).getOrElse(false)
  }

  override def fetch(url: String): String = {
    val resp = httpClient.request(GET(url).addTimeout(TasIxMe.timeoutMs))
    resp.bodyString
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

object TasIxMe extends ProviderCompanion[TasIxMe] {
  override val providerKey: String = "tasix"

  val timeoutMs: Int = 60 * 1000

  override def apply(config: Config): TasIxMe = {
    val (httpClient, cookieStore) = simpleClientWithStore()
    // Recently introduced "anti-ddos", so to speak
    val antiDdosCookie = {
      val c = new BasicClientCookie("trololofm", "test")
      c.setDomain("tas-ix.me")
      c.setPath("/")
      c
    }
    cookieStore.addCookie(antiDdosCookie)
    val authReq = POST("http://tas-ix.me/login.php")
      .addTimeout(timeoutMs)
      .addParameters(Map(
        "login_username" -> config.getString("login"),
        "login_password" -> config.getString("password"),
        "autologin" -> "1",
        "login" -> "Вход",
        "redirect" -> "index.php"
      ))
    val response = httpClient.request(authReq)
    if (response.code != 302) {
      throw new IllegalArgumentException(s"Failed to auth, got code ${response.code}, content ${response.bodyString}")
    }
    new TasIxMe(httpClient)
  }
}
