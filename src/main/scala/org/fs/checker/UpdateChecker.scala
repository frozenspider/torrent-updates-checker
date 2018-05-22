package org.fs.checker

import org.fs.checker.cache.CacheService
import org.fs.checker.cache.CachedDetails
import org.fs.checker.dao.TorrentEntry
import org.fs.checker.dumping.PageParsingException
import org.fs.checker.notification.UpdateNotifierService
import org.fs.checker.provider.Providers
import org.slf4s.Logging

import com.github.nscala_time.time.Imports._

/**
 * @author FS
 */
class UpdateChecker(
  getProviders:          () => Providers,
  listEntries:           () => Seq[TorrentEntry],
  updateNotifierService: UpdateNotifierService,
  cacheService:          CacheService
) extends Logging {

  def checkForUpdates(): Unit = {
    val entries = listEntries()
    if (!entries.isEmpty) {
      log.info(s"Check iteration started")
      val providers = getProviders()
      val updatedEntries = entries.filter {
        case TorrentEntry(alias, url) => checkUpdated(alias, url, providers)
      }
      if (!updatedEntries.isEmpty) {
        updateNotifierService.notify(updatedEntries)
      }
      log.info(s"Check iteration complete")
    } else {
      log.info(s"Check iteration skipped, no aliases defined")
    }
  }

  private def checkUpdated(alias: String, url: String, providers: Providers): Boolean = {
    val cachedDetailsOption = cacheService.getCachedDetails(url)
    (providers.providerFor(url), cachedDetailsOption) match {
      case (Some(provider), Some(cachedDetails)) =>
        try {
          val dateUpdated = provider.checkDateLastUpdated(url)
          val lastCheckDate = cachedDetails.lastCheckDateOption.getOrElse {
            log.info(s"'$alias' ($url) wasn't checked before")
            // Treat it as not updated
            dateUpdated.plusSeconds(1)
          }
          val now = DateTime.now
          cacheService.updateCachedDetails(url, CachedDetails(Some(now.getMillis), Some(dateUpdated.getMillis)))
          dateUpdated >= lastCheckDate
        } catch {
          case PageParsingException(providerName, url, content, th) =>
            log.error(s"${provider.prettyName} failed to process $url, content dumped", th)
            provider.dump(content)
            false
        }
      case (Some(provider), None) =>
        log.warn(s"Provider for '$alias' found, but it has been removed from cache")
        false
      case (None, _) =>
        log.warn(s"Can't find provider for '$alias' ($url)")
        false
    }
  }
}
