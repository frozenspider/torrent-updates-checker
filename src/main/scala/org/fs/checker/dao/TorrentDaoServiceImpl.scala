package org.fs.checker.dao

import scala.collection.immutable.ListMap
import scala.reflect.io.File

import org.fs.checker.utility.ConfigAccessor
import org.fs.checker.utility.ConfigImplicits._
import org.slf4s.Logging

import com.typesafe.config.Config

import configs.syntax._

/**
 * @author FS
 */
class TorrentDaoServiceImpl(
  checkUrlRecognized: String => Boolean,
  internalCfgFile:    File
)
  extends TorrentDaoService
  with Logging {

  protected[dao] val accessor = new ConfigAccessor(internalCfgFile)

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
    val cachePrefix = "manual." + quoted(entry.alias)
    accessor.update(accessor.config
      .withValue(s"$cachePrefix.index", newAliasesMap.size)
      .withValue(s"$cachePrefix.url", entry.url))
    log.info(s"'${entry.alias}' added, current aliases: ${aliasesMapToString(newAliasesMap)}")
  }

  override def remove(alias: String): Unit = {
    val aliasesMap = getAliasesMap()
    require((aliasesMap contains alias), s"'$alias' alias is not being checked, current aliases: ${aliasesMapToString(aliasesMap)}")
    val url = aliasesMap(alias)
    val newAliasesMap = aliasesMap - alias
    val newConfig = {
      // Exclude removed path and renumerate remaining entries
      val cacheWithoutRemoved = accessor.config.withoutPath("manual." + quoted(alias))
      newAliasesMap.zipWithIndex.foldLeft(cacheWithoutRemoved) {
        case (cache, ((alias, url), idx)) =>
          val cachePrefix = "manual." + quoted(alias)
          cache.withValue(s"$cachePrefix.index", idx + 1)
      }
    }
    accessor.update(newConfig)
    log.info(s"'$alias' ($url) removed from checking, current aliases: ${aliasesMapToString(newAliasesMap)}")
  }

  /** Alias -> URL map */
  private def getAliasesMap(): ListMap[String, String] = {
    val manualConfig = accessor.config.getOrElse[Config]("manual", emptyConfig).value
    val aliases = manualConfig.root.keys.toSeq
    val sortedSeq = aliases.map { alias =>
      alias -> manualConfig.getConfig(quoted(alias))
    }.sortBy {
      case (_, c) => c.get[Int]("index").valueOrElse(0)
    }
    ListMap(sortedSeq.map {
      case (alias, c) => alias -> c.getString("url")
    }: _*)
  }

  private def aliasesMapToString(urlsMap: ListMap[String, String]): String = {
    urlsMap.keys.toSeq.mkString("'", ", ", "'")
  }

  private def requireNoSpecialChars(paramName: String, value: String): Unit = {
    require(!(value contains "\\"), s"$paramName can't contain backslashes")
    require(!(value contains "\""), s"$paramName can't contain double-quotes")
  }
}
