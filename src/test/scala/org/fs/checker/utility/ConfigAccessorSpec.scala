package org.fs.checker.utility

import scala.reflect.io.File

import org.fs.checker.TestHelper
import org.junit.runner.RunWith
import org.scalatest.FlatSpec

import com.typesafe.config.ConfigValueFactory
import com.typesafe.config.Config

@RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ConfigAccessorSpec
  extends FlatSpec
  with TestHelper {

  private val cacheFile: File = File.makeTemp(suffix = ".conf")

  private val accessor = new ConfigAccessor(cacheFile)

  behavior of "file-based cache service implementation"

  it should "correctly update cache" in {
    assert(accessor.config.isEmpty)
    accessor.update(accessor.config.withValue("a.b", ConfigValueFactory.fromAnyRef(12345)))
    assert(!accessor.config.isEmpty)
    assert(accessor.config.getInt("a.b") === 12345)
    assert(accessor.config.getConfig("a").getInt("b") === 12345)
  }

}
