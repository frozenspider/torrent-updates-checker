package org.fs.checker.migration

import scala.collection.immutable.ListMap
import scala.reflect.io.File

import org.fs.checker.utility.ConfigAccessor
import org.fs.checker.utility.ConfigImplicits._
import org.slf4s.Logging

import com.typesafe.config.Config

import configs.Result
import configs.syntax._

/**
 * @author FS
 */
class MigrationService(internalCfgFile: File, cacheFile: File)
  extends Logging {

  private val LastAppliedKey = "general.lastAppliedMigration"

  private lazy val cacheAccessor = new ConfigAccessor(cacheFile)
  private lazy val internalCfgAccessor = new ConfigAccessor(internalCfgFile)

  val migrationMap: ListMap[String, () => Unit] = ListMap(
    "post-1.3_1" -> `post-1.3_1`_
  )

  def `post-1.3_1`(): Unit = {
    val (cache, internalCfg) = cacheAccessor.config.root().keys.foldLeft((cacheAccessor.config, emptyConfig)) {
      case ((cache, internalCfg), alias) =>
        val quotedAlias = quoted(alias)
        val cacheObj = cache.get[Config](quotedAlias).value
        val url = cacheObj.get[String]("url").value
        val cache2 = cache
          .withoutPath(quotedAlias)
          .withValue("\"" + url + "\"", newConfig(Map(
            "lastCheckMs" -> cacheObj.get[Option[Long]]("lastCheckMs").value,
            "lastUpdateMs" -> cacheObj.get[Option[Long]]("lastUpdateMs").value
          ).collect { case (k, Some(v)) => (k, v) }).root())
        val internalCfgObj2 = internalCfg.get[Config]("manual").valueOrElse(emptyConfig)
          .withValue(quotedAlias, newConfig(
            "index" -> cacheObj.get[Int]("index").valueOrElse(0L),
            "url" -> cacheObj.get[String]("url").value
          ).root())
        val internalCfg2 = internalCfg.withValue("manual", internalCfgObj2.root())
        (cache2, internalCfg2)
    }
    cacheAccessor.update(cache)
    internalCfgAccessor.update(internalCfg)
  }

  def apply(): Unit = {
    val lastApplied = internalCfgAccessor.config.get[String](LastAppliedKey)
    if (remainingMigrations.size > 0) {
      log.info(s"Applying ${remainingMigrations.size} migration(s)...")
      remainingMigrations.foreach(ver => migrationMap(ver).apply())
      log.info(s"Migrated")
    }
    internalCfgAccessor.update(internalCfgAccessor.config.withValue(LastAppliedKey, migrationMap.last._1))
  }

  private def remainingMigrations: Seq[String] = {
    val internalCfg = internalCfgAccessor.config
    val lastAppliedRes = internalCfg.get[String](LastAppliedKey)
    lastAppliedRes match {
      case Result.Success(lastApplied) if migrationMap.keySet contains lastApplied =>
        // Last applied migration is either latest or at least known
        migrationMap.keys.toSeq.dropWhile(_ != lastApplied).tail
      case Result.Success(lastApplied) =>
        // Last applied migration is unknown
        throw new Exception("Unknwon migration: " + lastApplied)
      case Result.Failure(_) if cacheAccessor.config.isEmpty =>
        // Fresh application
        Seq.empty
      case Result.Failure(_) =>
        // Oldest application version, before migrations were introduced
        migrationMap.keys.toSeq
    }
  }
}
