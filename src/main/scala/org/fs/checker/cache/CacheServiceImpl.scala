package org.fs.checker.cache

import scala.reflect.io.File

import org.fs.checker.utility.ConfigAccessor
import org.slf4s.Logging

import com.typesafe.config.ConfigValueFactory

import configs.syntax._

/**
 * @author FS
 */
class CacheServiceImpl(cacheFile: File)
  extends CacheService
  with Logging {

  protected[cache] val accessor = new ConfigAccessor(cacheFile)

  log.debug("Cache file: " + cacheFile.jfile.getAbsolutePath)

  override def getCachedDetailsOption(url: String): Option[CachedDetails] = {
    val cachePrefix = "\"" + url + "\""
    val lastCheckMsPath = s"$cachePrefix.lastCheckMs"
    val lastUpdateMsPath = s"$cachePrefix.lastUpdateMs"
    if (!accessor.config.hasPath(cachePrefix)) {
      None
    } else {
      val lastCheckMsOption = accessor.config.get[Option[Long]](lastCheckMsPath).value
      val lastUpdateMsOption = accessor.config.get[Option[Long]](lastUpdateMsPath).value
      Some(CachedDetails(lastCheckMsOption, lastUpdateMsOption))
    }
  }

  override def updateCachedDetails(url: String, cachedDetails: CachedDetails): Unit = {
    val cachePrefix = "\"" + url + "\""
    val lastCheckMsPath = s"$cachePrefix.lastCheckMs"
    val lastUpdateMsPath = s"$cachePrefix.lastUpdateMs"
    val updateMap = Map(
      lastCheckMsPath -> cachedDetails.lastCheckMsOption,
      lastUpdateMsPath -> cachedDetails.lastUpdateMsOption
    )
    val newConfig = updateMap.foldLeft(accessor.config) {
      case (config, (path, Some(value))) => config.withValue(path, ConfigValueFactory.fromAnyRef(value))
      case (config, _)                   => config
    }
    accessor.update(newConfig)
  }
}
