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
  private val providerCompanions: Seq[ProviderCompanion[_ <: Provider]] =
    Seq(
      TasIxMe,
      AlltorMe
    )

  private val providers: Seq[Provider] = providerCompanions map (pc => {
    if (!config.hasPath(pc.providerKey)) {
      log.warn(s"Couldn't find a key for ${pc.getClass.getSimpleName}, skipping provider")
      None
    } else {
      Try(pc(config.getConfig(pc.providerKey))) match {
        case Success(p: Provider) =>
          Some(p)
        case Failure(ex) =>
          // Do not log non-informative stacktraces
          ex match {
            case _: ConnectException | _: UnknownHostException =>
              log.warn(s"Exception creating provider for ${pc.getClass.getSimpleName}: ${ex.getMessage}")
            case _ =>
              log.warn(s"Exception creating provider for ${pc.getClass.getSimpleName}", ex)
          }
          None
      }
    }
  }) collect {
    case Some(p) => p
  }

  def providerFor(url: String): Option[Provider] = {
    providers.find(_.recognizeUrl(url))
  }
}
