package org.fs.checker.provider.impl

import java.io.File

import scala.io.Source

import org.fs.checker.TestHelper
import org.fs.checker.dao.TorrentParseResult
import org.joda.time.Days
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

import com.github.nscala_time.time.Imports._

@RunWith(classOf[JUnitRunner])
class RuTrackerSpec
    extends FlatSpec
    with TestHelper {

  val instance: RuTracker = new RuTracker(null, null)

  behavior of "rutracker provider"

  it should "parse 10 Jun 2019" in {
    val content = Source.fromFile(new File(pagesFolder, "elementary_2019-06-10.htm"), "windows-1251").mkString
    val parsed = instance.parseDateLastUpdated(content)
    assert(parsed.isInstanceOf[TorrentParseResult.Success])
    assert(parsed.asInstanceOf[TorrentParseResult.Success].dt === DateTime.parse("2019-06-10T20:39:00"))
  }

  it should "parse absorbed" in {
    val content = Source.fromFile(new File(pagesFolder, "the-boys_2020-10-19.htm"), "utf8").mkString
    val parsed = instance.parseDateLastUpdated(content)
    assert(parsed === TorrentParseResult.Failure.Absorbed)
  }

  val pagesFolder: java.io.File = new File(resourcesFolder, instance.providerKey)
}
