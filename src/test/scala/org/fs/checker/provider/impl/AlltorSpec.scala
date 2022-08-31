package org.fs.checker.provider.impl

import java.io.File

import scala.io.Source

import org.fs.checker.TestHelper
import org.fs.checker.dao.TorrentParseResult
import org.joda.time.Hours
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

import com.github.nscala_time.time.Imports._

@RunWith(classOf[JUnitRunner])
class AlltorSpec
    extends FlatSpec
    with TestHelper {

  val instance: Alltor = new Alltor(null, null)

  behavior of "alltor provider"

  it should "parse 1-day 11-hours ago state" in {
    val content = Source.fromFile(new File(pagesFolder, "blacklist_1d11h.html"), "UTF-8").mkString
    val parsed = instance.parseDateLastUpdated(content)
    assert(parsed.isInstanceOf[TorrentParseResult.Success])
    val now = DateTime.now
    assert(Hours.hoursBetween(parsed.asInstanceOf[TorrentParseResult.Success].dt, now) === Hours.hours(24 + 11))
  }

  it should "parse a not found case" in {
    val content = Source.fromFile(new File(pagesFolder, "_not-found.html"), "UTF-8").mkString
    val parsed = instance.parseDateLastUpdated(content)
    assert(parsed === TorrentParseResult.Failure.NotFound)
  }

  val pagesFolder: java.io.File = new File(resourcesFolder, instance.providerKey)
}
