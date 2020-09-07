package org.fs.checker.provider

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import org.fs.checker.dumping.PageContentDumpService
import org.fs.checker.provider.impl._
import org.slf4s.Logging

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/**
 * @author FS
 */
class Providers(config: Config, dumpService: PageContentDumpService) extends Logging {
  private val rawProvider: Seq[RawProvider] =
    Seq(
      TasIx,
      Alltor,
      Rutor,
      RuTracker,
      NonameClub,
      BookTracker,
      MetalTracker
    )

  private lazy val configuredProviders: Seq[ConfiguredProvider] = rawProvider map (raw => {
    val subConfig = if (config.hasPath(raw.providerKey)) {
      config.getConfig(raw.providerKey)
    } else {
      ConfigFactory.empty()
    }
    if (raw.requiresAuth && (!subConfig.hasPath("login") || subConfig.getString("login").isEmpty)) {
      log.debug(s"Couldn't find config for '${raw.prettyName}', skipping")
      None
    } else {
      Try(
        raw.withConfig(subConfig, dumpService)
      ) match {
          case Success(p: ConfiguredProvider) =>
            Some(p)
          case Failure(ex) =>
            reportProviderInitFailure(raw, ex)
            None
        }
    }
  }) collect {
    case Some(p) => p
  }

  def hasProviderFor(url: String): Boolean = {
    rawProvider.exists(_.recognizeUrl(url))
  }

  def providerFor(url: String): Option[ConfiguredProvider] = {
    configuredProviders.find(_.recognizeUrl(url))
  }

  private def reportProviderInitFailure(raw: RawProvider, ex: Throwable): Unit = ex match {
    case _: ConnectException | _: UnknownHostException | _: SocketTimeoutException =>
      // Do not log non-informative stacktraces
      log.warn(s"Exception creating provider for ${raw.getClass.getSimpleName} - ${ex.getClass.getName}: ${ex.getMessage}")
    case _: IllegalStateException =>
      // Do not log non-informative stacktraces
      log.warn(s"Exception creating provider for ${raw.getClass.getSimpleName} - ${ex.getMessage}")
    case _ =>
      log.warn(s"Exception creating provider for ${raw.getClass.getSimpleName}", ex)
  }
}
