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
    require(!(aliasesMap contains entry.alias), s"Alias ${entry.alias} is already beign checked")
    require(!(aliasesMap.values.toSeq contains entry.url), s"URL ${entry.url} is already beign checked under the alias '${aliasesMap.find(_._2 == entry.url).get._1}'")
    require(checkUrlRecognized(entry.url), s"URL ${entry.url} is not recognized by any provider")
    val newAliasesMap = aliasesMap + (entry.alias -> entry.url)
    val cache = cacheService.cache
    val cachePrefix = "\"" + entry.url + "\""
    val lastUpdateMsPath = s"$cachePrefix.lastUpdateMs"
    cacheService.update(cacheService.cache
      .withValue(s"$cachePrefix.alias", ConfigValueFactory.fromAnyRef(entry.alias))
      .withValue(s"$cachePrefix.index", ConfigValueFactory.fromAnyRef(newAliasesMap.size)))
    log.info(s"'${entry.alias}' added, current aliases: ${aliasesMapToString(newAliasesMap)}")
  }

  override def remove(alias: String): Unit = {
    val aliasesMap = getAliasesMap()
    require((aliasesMap contains alias), s"'$alias' alias is not beign checked, current aliases: ${aliasesMapToString(aliasesMap)}")
    val url = aliasesMap(alias)
    val newAliasesMap = aliasesMap - alias
    cacheService.update(cacheService.cache.withoutPath("\"" + url + "\""))
    log.info(s"'$alias' ($url) removed from checking, current aliases: ${aliasesMapToString(newAliasesMap)}")
  }

  /** Alias -> URL map */
  private def getAliasesMap(): ListMap[String, String] = {
    val cache = cacheService.cache
    val urls = cache.root.keys.toSeq
    val sortedSeq = urls.map { u =>
      u -> cache.getConfig(u)
    }.sortBy {
      case (_, c) if c.hasPath("index") => c.getInt("index")
      case _                            => 0
    }
    ListMap(sortedSeq.map {
      case (url, c) => url -> c.getString("alias")
    }: _*)
  }

  private def aliasesMapToString(urlsMap: ListMap[String, String]): String = {
    urlsMap.keys.toSeq.mkString("'", ", ", "'")
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
