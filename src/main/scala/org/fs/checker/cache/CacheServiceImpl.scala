package org.fs.checker.cache

import scala.reflect.io.File
import scala.util.Try

import org.slf4s.Logging

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigValueFactory

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
      cacheFile.writeAll(newCache.root.render(cacheWriteFormat))
    } ensuring {
      cache == newCache
    }

  override def getCachedDetails(alias: String): Option[CachedDetails] = {
    val cachePrefix = "\"" + alias + "\""
    val lastCheckMsPath = s"$cachePrefix.lastCheckMs"
    val lastUpdateMsPath = s"$cachePrefix.lastUpdateMs"
    if (!cache.hasPath(cachePrefix)) {
      None
    } else {
      val lastCheckMsOption = Try(cache.getLong(lastCheckMsPath)).toOption
      val lastUpdateMsOption = Try(cache.getLong(lastUpdateMsPath)).toOption
      Some(CachedDetails(lastCheckMsOption, lastUpdateMsOption))
    }
  }

  override def updateCachedDetails(alias: String, cachedDetails: CachedDetails): Unit = {
    val cachePrefix = "\"" + alias + "\""
    val lastCheckMsPath = s"$cachePrefix.lastCheckMs"
    val lastUpdateMsPath = s"$cachePrefix.lastUpdateMs"
    val updateMap = Map(
      lastCheckMsPath -> cachedDetails.lastCheckMsOption,
      lastUpdateMsPath -> cachedDetails.lastUpdateMsOption
    )
    val newCache = updateMap.foldLeft(cache) {
      case (cache, (path, Some(value))) => cache.withValue(path, ConfigValueFactory.fromAnyRef(value))
      case _                            => cache
    }
    update(newCache)
  }
}
