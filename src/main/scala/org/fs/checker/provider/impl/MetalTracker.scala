package org.fs.checker.provider.impl

import java.net.URL

import scala.util.Try

import org.apache.http.client.HttpClient
import org.fs.checker.dao.TorrentParseResult
import org.fs.checker.dumping.PageContentDumpService
import org.fs.checker.provider.ConfiguredProvider
import org.fs.checker.provider.GenProvider
import org.fs.checker.provider.RawProvider
import org.fs.checker.utility.ResponseBodyDecoder
import org.fs.utility.web.Imports._
import org.joda.time.format.DateTimeFormatter

import com.github.nscala_time.time.Imports._
import com.typesafe.config.Config

class MetalTrackerBase extends GenProvider {
  override val prettyName: String = "metal-tracker.com"

  override val providerKey: String = "metaltracker"

  override def recognizeUrl(url: String): Boolean = {
    // www.metal-tracker.com, en.metal-tracker.com
    Try((new URL(url)).getHost endsWith ".metal-tracker.com").getOrElse(false)
  }
}

class MetalTracker(httpClient: HttpClient, override val dumpService: PageContentDumpService) extends MetalTrackerBase with ConfiguredProvider {
  private val dtf: DateTimeFormatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss")

  override def fetch(url: String): String = {
    val resp = httpClient.request(GET(url).addTimeout(timeoutMs))
    ResponseBodyDecoder.bodyToString(resp)
  }

  override def parseDateLastUpdated(content: String): TorrentParseResult = {
    // Format: 13/06/2019 00:23:26
    val body = parseElement(content) \ "body"
    val detailsTable = body \\ "table" filterByClass "torrent_info"
    val detailsRightSection = (detailsTable \\ "td" filterByClass "half")(1)
    val row = (detailsRightSection \\ "tr")(0)
    val lastUpdatedNode = (row \ "td")(1)
    val lastUpdatedString = lastUpdatedNode.trimmedText
    val lastUpdatedDateString = lastUpdatedString.split(" ").take(2).mkString(" ")
    TorrentParseResult.Success(dtf.parseDateTime(lastUpdatedDateString))
  }
}

object MetalTracker extends MetalTrackerBase with RawProvider {
  override val requiresAuth = false

  override def withConfig(config: Config, dumpService: PageContentDumpService): MetalTracker = {
    val (httpClient, cookieStore) = simpleClientWithStore()
    // Does not require authentication
    new MetalTracker(httpClient, dumpService)
  }
}

