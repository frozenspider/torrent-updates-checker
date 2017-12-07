package org.fs.checker.cache

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import scala.reflect.io.File

/**
 * @author FS
 */
class CacheServiceImpl(cacheFile: File) extends CacheService {

  private lazy val cacheWriteFormat: ConfigRenderOptions = ConfigRenderOptions.concise.setFormatted(true).setJson(false)

  override def cache: Config =
    this.synchronized {
      ConfigFactory.parseFileAnySyntax(cacheFile.jfile)
    }

  override def update(newCache: Config): Unit =
    this.synchronized {
      cacheFile.writeAll(cache.root.render(cacheWriteFormat))
    }
}
