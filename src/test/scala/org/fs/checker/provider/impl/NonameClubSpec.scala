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
class NonameClubSpec
    extends FlatSpec
    with TestHelper {

  val instance: NonameClub = new NonameClub(null, null)

  behavior of "nnmclub provider"

  it should "parse 09 Jun 2019" in {
    val content = Source.fromFile(new File(pagesFolder, "elementary_2019-06-09.html"), "windows-1251").mkString
    val parsed = instance.parseDateLastUpdated(content)
    assert(parsed === DateTime.parse("2019-06-09T14:11:53"))
  }

  val pagesFolder: java.io.File = new File(resourcesFolder, instance.providerKey)
}
