package org.fs.checker.cache

import com.typesafe.config.Config

/**
 * @author FS
 */
trait CacheService {
  def cache: Config

  def update(newCache: Config): Unit

  def getCachedDetails(alias: String): Option[CachedDetails]

  def updateCachedDetails(alias: String, cachedDetails: CachedDetails): Unit
}
