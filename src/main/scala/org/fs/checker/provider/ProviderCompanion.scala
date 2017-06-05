package org.fs.checker.provider

import com.typesafe.config.Config

/**
 * @author FS
 */
trait ProviderCompanion[P <: Provider] {
  def providerKey: String

  def apply(config: Config): P
}
