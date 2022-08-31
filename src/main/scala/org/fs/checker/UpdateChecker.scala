package org.fs.checker

import java.net.SocketTimeoutException

import org.fs.checker.cache.CacheService
import org.fs.checker.cache.CachedDetails
import org.fs.checker.dao.TorrentEntry
import org.fs.checker.dao.TorrentParseResult
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
      var updated: Seq[TorrentEntry] = Seq.empty
      var notAvailable: Seq[(TorrentEntry, String)] = Seq.empty
      entries.foreach {
        case te @ TorrentEntry(alias, url) =>
          val cachedDetailsOption = cacheService.getCachedDetailsOption(url)
          fetchUpdateDate(alias, url, providers) match {
            case Some(TorrentParseResult.Success(dateUpdated)) =>
              val lastCheckDate = cachedDetailsOption.flatMap(_.lastCheckDateOption).getOrElse {
                log.info(s"'$alias' ($url) wasn't checked before")
                // Treat it as not updated
                dateUpdated.plusSeconds(1)
              }
              cacheService.updateCachedDetails(
                url,
                CachedDetails(
                  Some(DateTime.now.getMillis),
                  Some(dateUpdated.getMillis),
                  false
                )
              )
              if (dateUpdated >= lastCheckDate) {
                updated = updated :+ te
              }
            case Some(na: TorrentParseResult.Failure) =>
              if (cachedDetailsOption.map(_.isUnavailable) getOrElse false) {
                // NOOP
              } else {
                cacheService.updateCachedDetails(
                  url,
                  CachedDetails(
                    cachedDetailsOption flatMap (_.lastCheckMsOption),
                    cachedDetailsOption flatMap (_.lastUpdateMsOption),
                    true
                  )
                )
                notAvailable = notAvailable :+ (te, na.reason)
              }
            case None =>
            // NOOP
          }
      }
      updateNotifierService.notify(updated, notAvailable)
      log.info(s"Check iteration complete")
    } else {
      log.info(s"Check iteration skipped, no aliases defined")
    }
  }

  def fetchUpdateDate(
    alias:     String,
    url:       String,
    providers: Providers
  ): Option[TorrentParseResult] = {
    providers.providerFor(url) match {
      case Some(provider) =>
        try {
          Some(provider.checkDateLastUpdated(url))
        } catch {
          case PageParsingException(providerName, url, content, th) =>
            log.error(s"${provider.prettyName} failed to process $url, content dumped", th)
            provider.dump(content)
            None
          case ex: SocketTimeoutException =>
            log.error(s"${provider.prettyName} failed to process $url, socket timeout")
            None
        }
      case None =>
        log.warn(s"Can't find provider for '$alias' ($url)")
        None
    }
  }
}
