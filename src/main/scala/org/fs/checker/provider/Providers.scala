package org.fs.checker.provider

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import java.net.ConnectException
import java.net.UnknownHostException

import org.fs.checker.provider.impl.AlltorMe
import org.fs.checker.provider.impl.TasIxMe
import org.joda.time.DateTime
import org.slf4s.Logging

import com.typesafe.config.Config

/**
 * @author FS
 */
class Providers(config: Config) extends Logging {
  private val providerFactories: Seq[ProviderFactory[_ <: Provider]] =
    Seq(
      TasIxMe,
      AlltorMe
    )

  private val providers: Seq[Provider] = providerFactories map (pf => {
    if (!config.hasPath(pf.providerKey)) {
      log.debug(s"Couldn't find config for '${pf.prettyName}', skipping")
      None
    } else {
      Try(
        pf(config.getConfig(pf.providerKey))
      ) match {
          case Success(p: Provider) =>
            Some(p)
          case Failure(ex) =>
            reportProviderInitFailure(pf, ex)
            None
        }
    }
  }) collect {
    case Some(p) => p
  }

  def providerFor(url: String): Option[Provider] = {
    providers.find(_.recognizeUrl(url))
  }

  private def reportProviderInitFailure(pf: ProviderFactory[_], ex: Throwable): Unit = ex match {
    case _: ConnectException | _: UnknownHostException =>
      // Do not log non-informative stacktraces
      log.warn(s"Exception creating provider for ${pf.getClass.getSimpleName} - ${ex.getClass.getName}: ${ex.getMessage}")
    case _ =>
      log.warn(s"Exception creating provider for ${pf.getClass.getSimpleName}", ex)
  }
}
