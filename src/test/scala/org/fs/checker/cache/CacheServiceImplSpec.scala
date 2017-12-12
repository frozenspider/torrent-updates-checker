package org.fs.checker.cache

import scala.reflect.io.File

import org.fs.checker.TestHelper
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

import com.typesafe.config.ConfigValueFactory

@RunWith(classOf[JUnitRunner])
class CacheServiceImplSpec
    extends FlatSpec
    with TestHelper {

  private val cacheFile: File = File.makeTemp(suffix = ".conf")

  private val service = new CacheServiceImpl(cacheFile)

  behavior of "file-based cache service implementation"

  it should "correctly update cache" in {
    assert(service.cache.isEmpty)
    service.update(service.cache.withValue("a.b", ConfigValueFactory.fromAnyRef(12345)))
    assert(!service.cache.isEmpty)
    assert(service.cache.getInt("a.b") === 12345)
    assert(service.cache.getConfig("a").getInt("b") === 12345)
  }

}
