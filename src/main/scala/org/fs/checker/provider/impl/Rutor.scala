package org.fs.checker.provider.impl

import java.net.URL

import scala.util.Try

import org.apache.http.client.HttpClient
import org.fs.checker.dumping.PageContentDumpService
import org.fs.checker.provider.ConfiguredProvider
import org.fs.checker.provider.GenProvider
import org.fs.checker.provider.RawProvider
import org.fs.checker.utility.ResponseBodyDecoder
import org.fs.utility.web.Imports._
import org.joda.time.format.DateTimeFormatter

import com.github.nscala_time.time.Imports._
import com.typesafe.config.Config

class RutorBase extends GenProvider {
  override val prettyName: String = "rutor.info"

  override val providerKey: String = "rutor"

  val timeoutMs: Int = 60 * 1000

  override def recognizeUrl(url: String): Boolean = {
    Try(Seq("rutor.info", "rutor.is") contains (new URL(url)).getHost).getOrElse(false)
  }
}

class Rutor(httpClient: HttpClient, override val dumpService: PageContentDumpService) extends RutorBase with ConfiguredProvider {
  private val dtf: DateTimeFormatter = DateTimeFormat.forPattern("dd-MM-yyyy")

  override def fetch(url: String): String = {
    val resp = httpClient.request(GET(url).addTimeout(timeoutMs))
    ResponseBodyDecoder.bodyToString(resp)
  }

  override def parseDateLastUpdated(content: String): DateTime = {
    // 28-02-2019 23:19:22  (15 дней назад)
    val body = parseElement(content) \ "body"
    val tables = body \\ "table[@id='details']"
    val detailsTable = body \\ "table" filter (_.attribute("id").exists(_.text == "details"))
    val row = (detailsTable \ "tr") filter (tr => (tr \ "td" filterByClass "header").trimmedText contains "Добавлен")
    val lastUpdatedNode = (row \ "td")(1)
    val lastUpdatedString = lastUpdatedNode.trimmedText
    val lastUpdatedDateString = lastUpdatedString.split(" ").head
    dtf.parseDateTime(lastUpdatedDateString)
  }
}

object Rutor extends RutorBase with RawProvider {
  override val requiresAuth = false

  override def withConfig(config: Config, dumpService: PageContentDumpService): Rutor = {
    val (httpClient, cookieStore) = simpleClientWithStore()
    // Ddoes not require authentication
    new Rutor(httpClient, dumpService)
  }
}
