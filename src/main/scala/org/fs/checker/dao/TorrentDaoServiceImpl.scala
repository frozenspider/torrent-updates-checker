package org.fs.checker.dao

import scala.collection.immutable.ListMap

import org.fs.checker.cache.CacheService
import org.slf4s.Logging

import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueFactory

/**
 * @author FS
 */
class TorrentDaoServiceImpl(
  checkUrlRecognized: String => Boolean,
  cacheService:       CacheService
)
    extends TorrentDaoService
    with Logging {

  override def list(): Seq[TorrentEntry] = {
    getAliasesMap().map(TorrentEntry.tupled).toSeq
  }

  override def add(entry: TorrentEntry): Unit = {
    val aliasesMap = getAliasesMap()
    requireNoSpecialChars("Alias", entry.alias)
    requireNoSpecialChars("URL", entry.url)
    require(!(aliasesMap contains entry.alias), s"Alias ${entry.alias} is already beign checked")
    require(!(aliasesMap.values.toSeq contains entry.url), s"URL ${entry.url} is already beign checked under the alias '${aliasesMap.find(_._2 == entry.url).get._1}'")
    require(checkUrlRecognized(entry.url), s"URL ${entry.url} is not recognized by any provider")
    val newAliasesMap = aliasesMap + (entry.alias -> entry.url)
    val cache = cacheService.cache
    val cachePrefix = doubleQuote(entry.alias)
    val lastUpdateMsPath = s"$cachePrefix.lastUpdateMs"
    cacheService.update(cacheService.cache
      .withValue(s"$cachePrefix.index", ConfigValueFactory.fromAnyRef(newAliasesMap.size))
      .withValue(s"$cachePrefix.url", ConfigValueFactory.fromAnyRef(entry.url)))
    log.info(s"'${entry.alias}' added, current aliases: ${aliasesMapToString(newAliasesMap)}")
  }

  override def remove(alias: String): Unit = {
    val aliasesMap = getAliasesMap()
    require((aliasesMap contains alias), s"'$alias' alias is not being checked, current aliases: ${aliasesMapToString(aliasesMap)}")
    val url = aliasesMap(alias)
    val newAliasesMap = aliasesMap - alias
    val newCache = {
      // Exclude removed path and renumerate remaining entries
      val cacheWithoutRemoved = cacheService.cache.withoutPath(doubleQuote(alias))
      newAliasesMap.zipWithIndex.foldLeft(cacheWithoutRemoved) {
        case (cache, ((alias, url), idx)) =>
          val cachePrefix = doubleQuote(alias)
          cache.withValue(s"$cachePrefix.index", ConfigValueFactory.fromAnyRef(idx + 1))
      }
    }
    cacheService.update(newCache)
    log.info(s"'$alias' ($url) removed from checking, current aliases: ${aliasesMapToString(newAliasesMap)}")
  }

  /** Alias -> URL map */
  private def getAliasesMap(): ListMap[String, String] = {
    val cache = cacheService.cache
    val aliases = cache.root.keys.toSeq
    val sortedSeq = aliases.map { alias =>
      alias -> cache.getConfig(doubleQuote(alias))
    }.sortBy {
      case (_, c) if c.hasPath("index") => c.getInt("index")
      case _                            => 0
    }
    ListMap(sortedSeq.map {
      case (alias, c) => alias -> c.getString("url")
    }: _*)
  }

  private def doubleQuote(url: String): String =
    "\"" + url + "\""

  private def aliasesMapToString(urlsMap: ListMap[String, String]): String = {
    urlsMap.keys.toSeq.mkString("'", ", ", "'")
  }

  private def requireNoSpecialChars(paramName: String, value: String): Unit = {
    require(!(value contains "\\"), s"$paramName can't contain backslashes")
    require(!(value contains "\""), s"$paramName can't contain double-quotes")
  }

  private implicit class RichConfigObject(c: ConfigObject) {
    def toMap: Map[String, ConfigValue] = {
      scala.collection.JavaConverters.mapAsScalaMap(c).toMap
    }

    def keys: Iterable[String] = {
      toMap.keys
    }
  }
}
