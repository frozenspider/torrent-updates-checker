package org.fs.checker.provider

import java.io.File

import java.io.PrintWriter

import org.joda.time.DateTime
import org.slf4s.Logging
import org.fs.checker.dumping.PageParsingException

/**
 * @author FS
 */
trait Provider extends Logging {
  def prettyName: String

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

  def recognizeUrl(url: String): Boolean

  def fetch(url: String): String

  def parseDateLastUpdated(content: String): DateTime

  protected def dump(content: String): Unit = {
    val filename = s"${DateTime.now.toString("yyyy-MM-dd_HH-mm")}_${getClass.getSimpleName}.html"
    val file = new File(filename)
    val pw = new PrintWriter(file, "UTF-8")
    try {
      pw.write(content)
      pw.flush()
    } finally {
      pw.close()
    }
  }
}
