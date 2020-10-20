package org.fs.checker.cache

import scala.reflect.io.File

import org.fs.checker.utility.ConfigAccessor
import org.fs.checker.utility.ConfigImplicits._
import org.slf4s.Logging

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
    val cachePrefix = quoted(url)
    val lastCheckMsPath = s"$cachePrefix.lastCheckMs"
    val lastUpdateMsPath = s"$cachePrefix.lastUpdateMs"
    val isUnavailablePath = s"$cachePrefix.isUnavailable"
    if (!accessor.config.hasPath(cachePrefix)) {
      None
    } else {
      val lastCheckMsOption = accessor.config.get[Option[Long]](lastCheckMsPath).value
      val lastUpdateMsOption = accessor.config.get[Option[Long]](lastUpdateMsPath).value
      val isUnavailable = accessor.config.get[Boolean](isUnavailablePath).valueOrElse(false)
      Some(CachedDetails(lastCheckMsOption, lastUpdateMsOption, isUnavailable))
    }
  }

  override def updateCachedDetails(url: String, cachedDetails: CachedDetails): Unit = {
    val cachePrefix = quoted(url)
    val lastCheckMsPath = s"$cachePrefix.lastCheckMs"
    val lastUpdateMsPath = s"$cachePrefix.lastUpdateMs"
    val isUnavailablePath = s"$cachePrefix.isUnavailable"
    val updateMap = Map(
      lastCheckMsPath -> cachedDetails.lastCheckMsOption,
      lastUpdateMsPath -> cachedDetails.lastUpdateMsOption,
      isUnavailablePath -> cachedDetails.isUnavailable
    )
    val newConfig = updateMap.foldLeft(accessor.config) {
      case (config, (path, Some(value))) => config.withValue(path, value)
      case (config, (path, None))        => config
      case (config, (path, value))       => config.withValue(path, value)
    }
    accessor.update(newConfig)
  }
}
