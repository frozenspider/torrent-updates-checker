package org.fs.checker.provider

import java.net.ConnectException
import java.net.UnknownHostException

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import org.fs.checker.dumping.PageContentDumpService
import org.fs.checker.provider.impl.AlltorMe
import org.fs.checker.provider.impl.TasIxMe
import org.slf4s.Logging

import com.typesafe.config.Config

/**
 * @author FS
 */
class Providers(config: Config, dumpService: PageContentDumpService) extends Logging {
  private val rawProvider: Seq[RawProvider] =
    Seq(
      TasIxMe,
      AlltorMe
    )

  private lazy val configuredProviders: Seq[ConfiguredProvider] = rawProvider map (raw => {
    if (!config.hasPath(raw.providerKey)) {
      log.debug(s"Couldn't find config for '${raw.prettyName}', skipping")
      None
    } else {
      Try(
        raw.withConfig(config.getConfig(raw.providerKey), dumpService)
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
    case _: ConnectException | _: UnknownHostException =>
      // Do not log non-informative stacktraces
      log.warn(s"Exception creating provider for ${raw.getClass.getSimpleName} - ${ex.getClass.getName}: ${ex.getMessage}")
    case _ =>
      log.warn(s"Exception creating provider for ${raw.getClass.getSimpleName}", ex)
  }
}
