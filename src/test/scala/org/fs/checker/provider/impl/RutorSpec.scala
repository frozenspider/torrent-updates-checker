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
class RutorSpec
    extends FlatSpec
    with TestHelper {

  val instance: Rutor = new Rutor(null, null)

  behavior of "rutor provider"

  it should "parse 2019-02-28 state" in {
    val content = Source.fromFile(new File(routerFolder, "young-sheldon_2019-02-28.html"), "UTF-8").mkString
    val parsed = instance.parseDateLastUpdated(content)
    assert(parsed === DateTime.parse("2019-02-28T23:19:22"))
  }

  val routerFolder: java.io.File = new File(resourcesFolder, "rutor")
}
