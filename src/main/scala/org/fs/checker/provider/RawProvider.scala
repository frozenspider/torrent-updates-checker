package org.fs.checker.provider

import com.typesafe.config.Config

/**
 * Unconfigured/unauthed provider version
 *
 * @author FS
 */
trait RawProvider extends GenProvider {
  /** Create a provider, initialized using a given (full) config */
  def withConfig(config: Config): ConfiguredProvider
}
