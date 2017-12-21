package org.fs.checker

import org.fs.checker.cache.CacheService
import org.fs.checker.dao.TorrentEntry
import org.fs.checker.dumping.PageContentDumperService
import org.fs.checker.dumping.PageParsingException
import org.fs.checker.notification.UpdateNotifierService
import org.fs.checker.provider.Providers
import org.slf4s.Logging

import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigValueFactory

/**
 * @author FS
 */
class UpdateChecker(
  getProviders:          () => Providers,
  listEntries:           () => Seq[TorrentEntry],
  updateNotifierService: UpdateNotifierService,
  cacheService:          CacheService,
  dumperService:         PageContentDumperService
) extends Logging {

  def checkForUpdates(): Unit = {
    val entries = listEntries()
    if (!entries.isEmpty) {
      log.info(s"Check iteration started")
      val providers = getProviders()
      val updatedEntries = entries.filter {
        case TorrentEntry(alias, url) => isUpdated(alias, url, providers)
      }
      if (!updatedEntries.isEmpty) {
        updateNotifierService.notify(updatedEntries)
      }
      log.info(s"Check iteration complete")
    } else {
      log.info(s"Check iteration skipped, no aliases defined")
    }
  }

  private def isUpdated(alias: String, url: String, providers: Providers): Boolean = {
    val cache = cacheService.cache
    val cachePrefix = "\"" + alias + "\""
    val lastCheckMsPath = s"$cachePrefix.lastCheckMs"
    val lastUpdateMsPath = s"$cachePrefix.lastUpdateMs"
    providers.providerFor(url) match {
      case Some(provider) if cache.hasPath(cachePrefix) =>
        try {
          val dateUpdated = provider.checkDateLastUpdated(url)
          val lastCheckDate = try {
            new DateTime(cache.getLong(lastCheckMsPath))
          } catch {
            case ex: ConfigException.Missing =>
              log.info(s"URL $url wasn't checked before")
              // Treat it as not updated
              dateUpdated.plusSeconds(1)
          }
          val now = DateTime.now
          val newCache = cache
            .withValue(lastCheckMsPath, ConfigValueFactory.fromAnyRef(now.getMillis))
            .withValue(lastUpdateMsPath, ConfigValueFactory.fromAnyRef(dateUpdated.getMillis))
          cacheService.update(newCache)
          dateUpdated >= lastCheckDate
        } catch {
          case PageParsingException(providerName, url, content, th) =>
            log.error("$name failed to process $url, content dumped", th)
            dumperService.dump(content, providerName)
            false
        }
      case Some(provider) =>
        log.warn(s"Provider for '$alias' found, but it has been removed from cache")
        false
      case None =>
        log.warn(s"Can't find provider for '$alias' ($url)")
        false
    }
  }
}
