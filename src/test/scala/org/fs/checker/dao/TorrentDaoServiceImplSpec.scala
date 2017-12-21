package org.fs.checker.dao

import scala.reflect.io.File

import org.fs.checker.TestHelper
import org.fs.checker.cache.CacheServiceImpl
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

import com.typesafe.config.ConfigFactory

@RunWith(classOf[JUnitRunner])
class TorrentDaoServiceImplSpec
    extends FlatSpec
    with BeforeAndAfter
    with TestHelper {

  private val cacheService = {
    val cacheFile: File = File.makeTemp(suffix = ".conf")
    new CacheServiceImpl(cacheFile)
  }
  private val service = new TorrentDaoServiceImpl(
    (url: String) => url startsWith s"http://xyz$nonStandardAllowedChars",
    cacheService
  )

  before {
    cacheService.update(ConfigFactory.empty)
  }

  behavior of "TorrentDaoServiceImpl"

  it should "add valid entries" in {
    assert(cacheService.cache.isEmpty)
    val entries = Seq(1, 2, 3, 10, 5) map validEntry
    entries.foreach(
      service.add
    )
    assert(!cacheService.cache.isEmpty)
    assert(service.list === entries)
    assertCacheCorrectness(entries)
  }

  it should "not add invalid entries" in {
    assert(cacheService.cache.isEmpty)
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
    assert(!cacheService.cache.isEmpty)
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
    assertCacheCorrectness(Seq(3, 10, 1).map(validEntry))
  }

  private def validEntry(i: Int): TorrentEntry = {
    TorrentEntry(s"alias$nonStandardAllowedChars$i", s"http://xyz$nonStandardAllowedChars$i")
  }

  private def assertCacheCorrectness(entries: Seq[TorrentEntry]): Unit = {
    entries.zipWithIndex.foreach {
      case (TorrentEntry(alias, url), idx) =>
        assert(cacheService.cache.getInt("\"" + alias + "\".index") === idx + 1)
        assert(cacheService.cache.getString("\"" + alias + "\".url") === url)
    }
  }
}
