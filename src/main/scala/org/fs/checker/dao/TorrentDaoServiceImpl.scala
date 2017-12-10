package org.fs.checker.dao

import scala.collection.immutable.ListMap
import scala.reflect.io.File
import org.slf4s.Logging
import org.fs.checker.cache.CacheService

/**
 * @author FS
 */
class TorrentDaoServiceImpl(
  aliasesFile:        File,
  checkUrlRecognized: String => Boolean,
  cacheService:       CacheService
)
    extends TorrentDaoService
    with Logging {

  override def list(): Seq[TorrentEntry] = {
    getAliasesMap().map(TorrentEntry.tupled).toSeq
  }

  override def add(entry: TorrentEntry): Seq[TorrentEntry] = {
    val aliasesMap = getAliasesMap()
    require(!(aliasesMap contains entry.alias), s"Alias ${entry.alias} is already beign checked")
    require(!(aliasesMap.values.toSeq contains entry.url), s"URL ${entry.url} is already beign checked under the alias '${aliasesMap.find(_._2 == entry.url).get._1}'")
    require(checkUrlRecognized(entry.url), s"URL ${entry.url} is not recognized by any provider")
    val newAliasesMap = aliasesMap + (entry.alias -> entry.url)
    aliasesFile.writeAll(newAliasesMap.map { case (k, v) => s"$k $v" }.mkString("\n"))
    log.info(s"'${entry.alias}' added, current aliases: ${aliasesMapToString(newAliasesMap)}")
    list()
  }

  override def remove(alias: String): Seq[TorrentEntry] = {
    val aliasesMap = getAliasesMap()
    require((aliasesMap contains alias), s"'$alias' alias is not beign checked, current aliases: ${aliasesMapToString(aliasesMap)}")
    val url = aliasesMap(alias)
    val newAliasesMap = aliasesMap - alias
    aliasesFile.writeAll(newAliasesMap.map { case (k, v) => s"$k $v" }.mkString("\n"))
    cacheService.update(cacheService.cache.withoutPath("\"" + url + "\""))
    log.info(s"'$alias' ($url) removed from checking, current aliases: ${aliasesMapToString(newAliasesMap)}")
    list()
  }

  /** Alias -> URL map */
  private def getAliasesMap(): ListMap[String, String] = {
    ListMap(aliasesFile.lines.map(l => {
      val parts = l.split(" ", 2)
      parts(0) -> parts(1)
    }).toSeq: _*)
  }

  private def aliasesMapToString(urlsMap: ListMap[String, String]): String = {
    urlsMap.keys.toSeq.mkString("'", ", ", "'")
  }
}
