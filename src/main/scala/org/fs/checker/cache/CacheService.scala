package org.fs.checker.cache

/**
 * @author FS
 */
trait CacheService {
  def getCachedDetails(url: String): Option[CachedDetails]

  def updateCachedDetails(url: String, cachedDetails: CachedDetails): Unit
}
