package org.fs.checker.provider

import org.fs.checker.dumping.PageContentDumpService
import org.fs.checker.dumping.PageParsingException
import org.joda.time.DateTime
import org.slf4s.Logging

/**
 * Configured and authenticated provider which is ready for the check queries.
 *
 * @author FS
 */
trait ConfiguredProvider extends GenProvider with Logging {
  @throws[PageParsingException]
  def checkDateLastUpdated(url: String): DateTime = {
    require(recognizeUrl(url))
    val body = fetch(url)
    try {
      val parsed = parseDateLastUpdated(body)
      parsed
    } catch {
      case ex: Exception => throw new PageParsingException(prettyName, url, body, ex)
    }
  }

  def fetch(url: String): String

  def parseDateLastUpdated(content: String): DateTime

  protected def dumpService: PageContentDumpService

  /** Dump the given HTML content to a file for further examination by hand */
  def dump(content: String): Unit =
    dumpService.dump(content, providerKey)
}
