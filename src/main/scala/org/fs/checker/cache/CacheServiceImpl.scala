package org.fs.checker.cache

import scala.reflect.io.File

import org.slf4s.Logging

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions

/**
 * @author FS
 */
class CacheServiceImpl(cacheFile: File)
    extends CacheService
    with Logging {

  log.debug("Cache file: " + cacheFile.jfile.getAbsolutePath)

  private lazy val cacheWriteFormat: ConfigRenderOptions =
    ConfigRenderOptions.concise
      .setFormatted(true)
      .setJson(false)

  override def cache: Config =
    this.synchronized {
      ConfigFactory.parseFileAnySyntax(cacheFile.jfile)
    }

  override def update(newCache: Config): Unit =
    this.synchronized {
      cacheFile.writeAll(cache.root.render(cacheWriteFormat))
    }
}
