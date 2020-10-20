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
class MetalTrackerSpec
    extends FlatSpec
    with TestHelper {

  val instance: MetalTracker = new MetalTracker(null, null)

  behavior of "metaltracker provider"

  it should "parse 31 May 2019" in {
    val content = Source.fromFile(new File(pagesFolder, "gloryhammer_2019-05-31.htm"), "UTF-8").mkString
    val parsed = instance.parseDateLastUpdated(content)
    assert(parsed.isInstanceOf[TorrentParseResult.Success])
    assert(parsed.asInstanceOf[TorrentParseResult.Success].dt === DateTime.parse("2019-05-31T23:37:16"))
  }

  val pagesFolder: java.io.File = new File(resourcesFolder, instance.providerKey)
}
