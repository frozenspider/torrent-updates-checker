package org.fs.checker.provider.impl

import java.io.File

import scala.io.Source

import org.fs.checker.TestHelper
import org.joda.time.Days
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

import com.github.nscala_time.time.Imports._

@RunWith(classOf[JUnitRunner])
class BookTrackerSpec
    extends FlatSpec
    with TestHelper {

  val instance: BookTracker = new BookTracker(null, null)

  behavior of "booktracker provider"

  it should "parse 08 Jul 2018" in {
    val content = Source.fromFile(new File(pagesFolder, "on-basilisk-station_2018-07-08.htm"), "UTF-8").mkString
    val parsed = instance.parseDateLastUpdated(content)
    assert(parsed === DateTime.parse("2018-07-08T07:10:00"))
  }

  val pagesFolder: java.io.File = new File(resourcesFolder, instance.providerKey)
}
