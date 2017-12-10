package org.fs.checker.cache

import com.typesafe.config.Config

/**
 * @author FS
 */
trait CacheService {
  def cache: Config

  def update(newCache: Config): Unit
}
