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
class TasIxSpec
    extends FlatSpec
    with TestHelper {

  val instance: TasIx = new TasIx(null, null)

  behavior of "tas-ix provider"

  it should "parse 8-days ago state" in {
    val content = Source.fromFile(new File(pagesFolder, "expanse_8d.html"), "UTF-8").mkString
    val parsed = instance.parseDateLastUpdated(content)
    val now = DateTime.now
    assert(Days.daysBetween(parsed, now) === Days.days(8))
  }

  val pagesFolder: java.io.File = new File(resourcesFolder, instance.providerKey)
}
