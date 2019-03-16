package org.fs.checker.provider.impl

import java.io.File

import scala.io.Source

import org.fs.checker.TestHelper
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
    val content = Source.fromFile(new File(routerFolder, "blacklist_1d11h.html"), "UTF-8").mkString
    val parsed = instance.parseDateLastUpdated(content)
    val now = DateTime.now
    assert(Hours.hoursBetween(parsed, now) === Hours.hours(24 + 11))
  }

  val routerFolder: java.io.File = new File(resourcesFolder, "alltor")
}
