package org.fs.checker.provider.impl

import java.net.URL

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

class NonameClubBase extends GenProvider {
  override val prettyName: String = "nnmclub"

  override val providerKey: String = "nnmclub"

  val timeoutMs: Int = 60 * 1000

  override def recognizeUrl(url: String): Boolean = {
    Try("nnmclub.to" == (new URL(url)).getHost).getOrElse(false)
  }
}

class NonameClub(httpClient: HttpClient, override val dumpService: PageContentDumpService) extends NonameClubBase with ConfiguredProvider {
  override def fetch(url: String): String = {
    val resp = httpClient.request(GET(url).addTimeout(timeoutMs))
    ResponseBodyDecoder.bodyToString(resp)
  }

  override def parseDateLastUpdated(content: String): DateTime = {
    // Format: 09 Июн 2019 14:11:53
    val body = parseElement(content) \ "body"
    val detailsTable = body \\ "table" filterByClass "btTbl"
    val row = (detailsTable \ "tr") filter (tr => (tr \ "td")(0).trimmedText contains "Зарегистрирован")
    val lastUpdatedNode = (row \ "td")(1)
    val lastUpdatedString = lastUpdatedNode.trimmedText
    val split = lastUpdatedString.split("[ :]")
    assert(split.size == 6, "Unexpected page format!")
    val monthInt = MonthNameParser.parse(split(1))
    new DateTime(split(2).toInt, monthInt, split(0).toInt, split(3).toInt, split(4).toInt, split(5).toInt)
  }
}

object NonameClub extends NonameClubBase with RawProvider {
  override val requiresAuth = false

  override def withConfig(config: Config, dumpService: PageContentDumpService): NonameClub = {
    val (httpClient, cookieStore) = simpleClientWithStore()
    // Does not require authentication
    new NonameClub(httpClient, dumpService)
  }
}

