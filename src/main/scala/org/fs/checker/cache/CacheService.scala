package org.fs.checker.cache

/**
 * @author FS
 */
trait CacheService {
  def getCachedDetailsOption(url: String): Option[CachedDetails]

  def updateCachedDetails(url: String, cachedDetails: CachedDetails): Unit
}
