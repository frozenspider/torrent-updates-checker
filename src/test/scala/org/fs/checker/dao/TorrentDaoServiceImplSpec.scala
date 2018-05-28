package org.fs.checker.dao

import scala.reflect.io.File

import org.fs.checker.TestHelper
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.FlatSpec

import org.fs.checker.utility.ConfigImplicits._
import scala.io.Source

@RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TorrentDaoServiceImplSpec
  extends FlatSpec
  with BeforeAndAfter
  with TestHelper {

  private val internalCfgFile: File = File.makeTemp(suffix = ".conf")
  private val service = new TorrentDaoServiceImpl(
    (url: String) => url startsWith s"http://xyz$nonStandardAllowedChars",
    internalCfgFile
  )

  before {
    internalCfgFile.writeAll("")
  }

  behavior of "TorrentDaoServiceImpl"

  it should "add valid entries" in {
    assert(internalCfgFile.jfile.length === 0, quoted(Source.fromFile(internalCfgFile.jfile).mkString))
    val entries = Seq(1, 2, 3, 10, 5) map validEntry
    entries.foreach(
      service.add
    )
    assert(!internalCfgFile.isEmpty)
    assert(service.list === entries)
    assertConfigCorrectness(entries)
  }

  it should "not add invalid entries" in {
    assert(internalCfgFile.jfile.length === 0, quoted(Source.fromFile(internalCfgFile.jfile).mkString))
    service.add(validEntry(1))
    intercept[IllegalArgumentException] {
      service.add(validEntry(1))
    }
    intercept[IllegalArgumentException] {
      service.add(validEntry(1).copy(alias = "ali\\as"))
    }
    intercept[IllegalArgumentException] {
      service.add(validEntry(1).copy(alias = "ali\"as"))
    }
    intercept[IllegalArgumentException] {
      service.add(TorrentEntry(validEntry(1).alias, validEntry(2).url))
    }
    intercept[IllegalArgumentException] {
      service.add(TorrentEntry(validEntry(2).alias, validEntry(1).url))
    }
    intercept[IllegalArgumentException] {
      service.add(TorrentEntry("alias2", "xy"))
    }
    assert(!internalCfgFile.isEmpty)
    assert(service.list === validEntry(1) :: Nil)
  }

  it should "remove existing entries" in {
    val entries = Seq(1, 2, 3, 10, 5).toIndexedSeq map validEntry
    entries.foreach(
      service.add
    )
    service.remove(entries(1).alias)
    service.remove(entries(0).alias)
    service.remove(entries(4).alias)
    assert(service.list === Seq(3, 10).map(validEntry))
    service.add(validEntry(1))
    assert(service.list === Seq(3, 10, 1).map(validEntry))
    assertConfigCorrectness(Seq(3, 10, 1).map(validEntry))
  }

  private def validEntry(i: Int): TorrentEntry = {
    TorrentEntry(s"alias$nonStandardAllowedChars$i", s"http://xyz$nonStandardAllowedChars$i")
  }

  private def assertConfigCorrectness(entries: Seq[TorrentEntry]): Unit = {
    val internalCfgConfig = service.accessor.config
    entries.zipWithIndex.foreach {
      case (TorrentEntry(alias, url), idx) =>
        assert(internalCfgConfig.getInt("manual." + quoted(alias) + ".index") === idx + 1)
        assert(internalCfgConfig.getString("manual." +quoted(alias) + ".url") === url)
    }
  }
}
