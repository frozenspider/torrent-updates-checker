package org.fs.checker

import scala.collection.immutable.ListMap
import scala.reflect.io.File

import org.fs.checker.dumping.PageContentDumper
import org.fs.checker.provider.Providers
import org.fs.checker.web.TorrentUpdatesCheckerWebUi
import org.slf4j.bridge.SLF4JBridgeHandler
import org.slf4s.Logging

import com.github.nscala_time.time.Imports._
import com.twitter.finagle.ListeningServer
import com.twitter.util.Await
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions

/**
 * @author FS
 */
object TorrentUpdatesCheckerEntry extends App with Logging {
  SLF4JBridgeHandler.removeHandlersForRootLogger();
  SLF4JBridgeHandler.install()

  val aliasesFile: File = getFile("urls.txt")
  if (!aliasesFile.exists) {
    log.error(s"${aliasesFile.name} is not found")
    scala.sys.exit(1)
  }
  /** Alias -> URL map */
  val aliasesMap: Map[String, String] = getAliasesMap()

  val configFile: scala.reflect.io.File = getFile("application.conf")
  def config: Config = {
    if (!configFile.exists) {
      log.error("Config file is not found")
      scala.sys.exit(1)
    }
    ConfigFactory.parseFileAnySyntax(configFile.jfile)
  }
  lazy val webUiPort = config.getInt("webui.port")
  lazy val cacheFile: scala.reflect.io.File = getFile("cache.conf")
  lazy val cache: Config = ConfigFactory.parseFileAnySyntax(cacheFile.jfile)
  lazy val cacheWriteFormat: ConfigRenderOptions = ConfigRenderOptions.concise.setFormatted(true).setJson(false)

  lazy val dumper = new PageContentDumper {
    override def dump(content: String, providerName: String): Unit = {
      val nowString = DateTime.now.toString("yyyy-MM-dd_HH-mm")
      val file = getFile(providerName + "/" + nowString + ".html")
      file.writeAll(content)
    }
  }

  lazy val updateChecker: UpdateChecker =
    new UpdateChecker(getProviders, getAliasesMap, UpdateNotifier.notifyUpdated, cache, saveCache, dumper)

  def add(args: Seq[String]): Unit = {
    val alias = args(0)
    val url = args(1)
    if (aliasesMap contains alias) {
      log.error(s"Alias ${alias} is already beign checked")
    } else if (aliasesMap.values.toSeq contains url) {
      log.error(s"URL ${url} is already beign checked under the alias '$alias'")
    } else {
      val newAliasesMap = aliasesMap + (alias -> url)
      aliasesFile.writeAll(newAliasesMap.map { case (k, v) => s"$k $v" }.mkString("\n"))
      log.info(s"'$alias' added, current aliases: ${aliasesMapToString(newAliasesMap)}")
    }
  }

  def remove(args: Seq[String]): Unit = {
    val alias = args(0)
    if (aliasesMap contains alias) {
      val url = aliasesMap(alias)
      val newAliasesMap = aliasesMap - alias
      aliasesFile.writeAll(newAliasesMap.map { case (k, v) => s"$k $v" }.mkString("\n"))
      val cache2 = cache.withoutPath("\"" + url + "\"")
      saveCache(cache2)
      log.info(s"'$alias' ($url) removed from checking, current aliases: ${aliasesMapToString(newAliasesMap)}")
    } else {
      log.error(s"'$alias' alias is not beign checked, current aliases: ${aliasesMapToString(aliasesMap)}")
    }
  }

  def list(args: Seq[String]): Unit = {
    if (!aliasesMap.isEmpty) {
      aliasesMap.foreach {
        case (alias, url) => println(s"$alias $url")
      }
    } else {
      println(s"<No aliases defined>")
    }
  }

  def start(args: Seq[String]): Unit = {
    if (webUiPort != 0) {
      startAsyncServer()
    }
    val runnable = new Runnable {
      override def run() {
        try {
          log.info("Checked thread started")
          while (!Thread.currentThread().isInterrupted()) {
            val startTime = DateTime.now
            iterate(args)
            val endTime = DateTime.now
            val nextStartTime = startTime.plusHours(1)
            if (nextStartTime > endTime) {
              Thread.sleep(nextStartTime.getMillis - endTime.getMillis)
            }
          }
          log.info("Checked thread interrupted")
        } catch {
          case th: Throwable => log.error("Exception in runner thread", th)
        }
      }
    }
    val thread = new Thread(runnable, "runner")
    thread.start()
  }

  def startServer(args: Seq[String]): Unit = {
    val server = startAsyncServer()
    Await.ready(server)
  }

  private def startAsyncServer(): ListeningServer = {
    (new TorrentUpdatesCheckerWebUi).start(webUiPort)
  }

  def iterate(args: Seq[String]): Unit = {
    updateChecker.checkForUpdates()
  }

  private def getFile(s: String): File = {
    new File(new java.io.File(s))
  }

  private def saveCache(cache: Config): Unit = {
    cacheFile.writeAll(cache.root.render(cacheWriteFormat))
  }

  private def getAliasesMap(): Map[String, String] = {
    ListMap(aliasesFile.lines.map(l => {
      val parts = l.split(" ", 2)
      parts(0) -> parts(1)
    }).toSeq: _*)
  }

  private def aliasesMapToString(urlsMap: Map[String, String]): String = {
    urlsMap.keys.toSeq.sorted.mkString("'", ", ", "'")
  }

  private def getProviders(): Providers = {
    new Providers(config)
  }

  // We might want to use scopt library for CLI args parsing
  val actions: Map[String, (Seq[String] => Unit, Range)] = Map(
    "add" -> (add, 2.range),
    "remove" -> (remove, 1.range),
    "list" -> (list, 0.range),
    "start" -> (start, 0.range),
    "startServer" -> (startServer, 0.range),
    "iterate" -> (iterate, 0.range)
  )

  args.toList match {
    case x :: xs if actions.keySet.contains(x) && actions(x)._2.contains(xs.size) =>
      actions(x)._1.apply(xs)
    case _ =>
      println("""Supported actions:
  add <alias> <url>
  remove <alias>
  list
  start
  startServer
  iterate""")
  }

  private implicit class RichInt(i: Int) {
    def range = i to i
  }
}
