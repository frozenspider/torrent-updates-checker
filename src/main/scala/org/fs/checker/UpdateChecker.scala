package org.fs.checker

import org.fs.checker.provider.Providers
import com.github.nscala_time.time.Imports._
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import org.slf4s.Logging
import org.joda.time.Days
import org.joda.time.Hours
import com.typesafe.config.ConfigValueFactory
import scala.reflect.io.File
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import org.fs.checker.dumping.PageParsingException
import org.fs.checker.dumping.PageContentDumper

/**
 * @author FS
 */
class UpdateChecker(getProviders: () => Providers,
                    getAliasesMap: () => Map[String, String],
                    notifyUpdated: Map[String, String] => Unit,
                    var cache: Config,
                    saveCache: Config => Unit,
                    dumper: PageContentDumper)
    extends Logging {

  def checkForUpdates(): Unit = {
    val aliasesMap = getAliasesMap()
    if (!aliasesMap.isEmpty) {
      log.info(s"Check iteration started")
      val providers = getProviders()
      val mapOfUpdatedAliases = aliasesMap.filter {
        case (alias, url) => isUpdated(alias, url, providers)
      }
      if (!mapOfUpdatedAliases.isEmpty) {
        notifyUpdated(mapOfUpdatedAliases)
      }
      log.info(s"Check iteration complete")
    } else {
      log.info(s"Check iteration skipped, no aliases defined")
    }
  }

  private def isUpdated(alias: String, url: String, providers: Providers): Boolean =
    providers.providerFor(url) match {
      case Some(provider) =>
        try {
          val dateUpdated = provider.checkDateLastUpdated(url)
          val cachePrefix = "\"" + url + "\""
          val lastCheckMsPath = s"$cachePrefix.lastCheckMs"
          val lastUpdateMsPath = s"$cachePrefix.lastUpdateMs"
          val lastCheckDate = try {
            new DateTime(cache.getLong(lastCheckMsPath))
          } catch {
            case ex: ConfigException.Missing =>
              log.info(s"URL $url wasn't checked before")
              // Treat it as not updated
              dateUpdated.plusSeconds(1)
          }
          val now = DateTime.now
          cache = cache
            .withValue(lastCheckMsPath, ConfigValueFactory.fromAnyRef(now.getMillis))
            .withValue(lastUpdateMsPath, ConfigValueFactory.fromAnyRef(dateUpdated.getMillis))
          saveCache(cache)
          dateUpdated >= lastCheckDate
        } catch {
          case PageParsingException(providerName, url, content, th) =>
            log.error("$name failed to process $url, content dumped", th)
            dumper.dump(content, providerName)
            false
        }
      case None =>
        log.warn(s"Can't find provider for '$alias' ($url)")
        false
    }
}
