package org.fs.checker

import scala.io.Codec
import scala.reflect.io.File

import org.fs.checker.cache.CacheService
import org.fs.checker.cache.CacheServiceImpl
import org.fs.checker.dao.TorrentDaoService
import org.fs.checker.dao.TorrentDaoServiceImpl
import org.fs.checker.dao.TorrentEntry
import org.fs.checker.dumping.PageContentDumpService
import org.fs.checker.migration.MigrationService
import org.fs.checker.notification.UpdateNotifierService
import org.fs.checker.notification.UpdateNotifierServiceImpl
import org.fs.checker.provider.Providers
import org.fs.checker.web.HttpServer
import org.slf4j.bridge.SLF4JBridgeHandler
import org.slf4s.Logging

import com.github.nscala_time.time.Imports._
import com.twitter.finagle.ListeningServer
import com.twitter.util.Await
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.fs.checker.dao.TorrentParseResult

/**
 * @author FS
 */
object TorrentUpdatesCheckerEntry extends App with Logging {
  SLF4JBridgeHandler.removeHandlersForRootLogger()
  SLF4JBridgeHandler.install()

  val appConfig: Config = {
    val configFile: File = getFile("application.conf")
    if (!configFile.exists) {
      log.error(s"${configFile.name} does not exist at ${absolutePath(configFile)}")
      scala.sys.exit(1)
    }
    ConfigFactory.parseFileAnySyntax(configFile.jfile)
  }

  val cacheFile: File = getFile("cache.conf")
  if (!cacheFile.exists) {
    cacheFile.writeAll("")
    log.info(s"Cache file does not exist, created")
  }

  val internalCfgFile: File = getFile("internal.conf")
  if (!internalCfgFile.exists) {
    internalCfgFile.writeAll("")
    log.info(s"Internal config file does not exist, created")
  }

  // TODO: Read name from logback appender config
  lazy val logFile: File = getFile("torrent-updates-checker.log")

  lazy val httpPort = appConfig.getInt("http.port")

  lazy val migrationService: MigrationService = new MigrationService(internalCfgFile, cacheFile)
  lazy val cacheService: CacheService = new CacheServiceImpl(cacheFile)
  lazy val daoService: TorrentDaoService = new TorrentDaoServiceImpl(checkUrlRecognized, internalCfgFile)
  lazy val updateNotifierService: UpdateNotifierService = new UpdateNotifierServiceImpl
  lazy val dumpService: PageContentDumpService = new PageContentDumpService {
    override def dump(content: String, providerName: String): Unit = {
      val nowString = DateTime.now.toString("yyyy-MM-dd_HH-mm-ss")
      val file = getFile(providerName + "/" + nowString + ".html")
      file.parent.createDirectory()
      file.writeAll(content)
    }
  }
  lazy val httpServer = {
    new HttpServer(daoService, logFile)
  }

  lazy val updateChecker: UpdateChecker =
    new UpdateChecker(getProviders _, () => daoService.list, updateNotifierService, cacheService)

  def add(args: Seq[String]): Unit = {
    wrapServiceCallForCli {
      daoService.add(new TorrentEntry(args(0), args(1)))
    }
  }

  def remove(args: Seq[String]): Unit = {
    wrapServiceCallForCli {
      daoService.remove(args(0))
    }
  }

  def list(args: Seq[String]): Unit = {
    wrapServiceCallForCli {
      val aliasesList = daoService.list
      if (!aliasesList.isEmpty) {
        aliasesList.foreach {
          case TorrentEntry(alias, url) => println(s"$alias $url")
        }
      } else {
        println(s"<No aliases defined>")
      }
    }
  }

  def start(args: Seq[String]): Unit = {
    if (httpPort != 0) {
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
    httpServer.start(httpPort)
  }

  def iterate(args: Seq[String]): Unit = {
    updateChecker.checkForUpdates()
  }

  def getLastUpdated(args: Seq[String]): Unit = {
    val url = args(0)
    val fetchResult = updateChecker.fetchUpdateDate(url, url, getProviders())
    fetchResult match {
      case Some(TorrentParseResult.Success(dateUpdated)) =>
        println("Last Updated: " + dateUpdated.toString("yyyy-MM-dd HH:mm:ss"))
      case Some(na: TorrentParseResult.Failure) =>
        println(s"Torrent is not available (${na.reason})")
      case None =>
        println("Error!")
    }
  }

  private def getFile(s: String): File = {
    new File(new java.io.File(s))(Codec.UTF8)
  }

  private def getProviders(): Providers = {
    new Providers(appConfig, dumpService)
  }

  private def checkUrlRecognized(url: String): Boolean = {
    getProviders().hasProviderFor(url)
  }

  private def absolutePath(f: File): String = f.jfile.getAbsolutePath

  private def wrapServiceCallForCli[R](code: => R): Option[R] = {
    try {
      Some(code)
    } catch {
      case ex: IllegalArgumentException =>
        log.error(ex.getMessage)
        None
      case ex: Throwable =>
        log.error("Internal error", ex)
        None
    }
  }

  // We might want to use scopt library for CLI args parsing
  val actions: Map[String, (Seq[String] => Unit, Range)] = Map(
    "add" -> (add, 2.range),
    "remove" -> (remove, 1.range),
    "list" -> (list, 0.range),
    "start" -> (start, 0.range),
    "startServer" -> (startServer, 0.range),
    "iterate" -> (iterate, 0.range),
    "getLastUpdated" -> (getLastUpdated, 1.range)
  )

  log.info(s"${BuildInfo.name} v${BuildInfo.version}")
  migrationService.apply()
  args.toList match {
    case x :: xs if actions.keySet.contains(x) && actions(x)._2.contains(xs.size) =>
      actions(x)._1.apply(xs)
    case _ =>
      println("""|Supported actions:
      |  add <alias> <url>
      |  remove <alias>
      |  list
      |  start
      |  startServer
      |  iterate
      |  getLastUpdated <url>""".stripMargin)
  }

  private implicit class RichInt(i: Int) {
    def range = i to i
  }
}
