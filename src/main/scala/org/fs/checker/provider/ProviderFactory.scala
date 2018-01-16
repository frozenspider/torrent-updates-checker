package org.fs.checker.provider

import com.typesafe.config.Config

/**
 * @author FS
 */
trait ProviderFactory[P <: Provider] {
  /** User-readable name for this provider */
  def prettyName: String

  /** Settings prefix for this provider */
  def providerKey: String

  /** Create a provider, initialized using a given (full) config */
  def apply(config: Config): P
}
