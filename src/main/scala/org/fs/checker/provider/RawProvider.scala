package org.fs.checker.provider

import org.fs.checker.dumping.PageContentDumpService

import com.typesafe.config.Config

/**
 * Unconfigured/unauthed provider version
 *
 * @author FS
 */
trait RawProvider extends GenProvider {
  def requiresAuth: Boolean

  /** Create a provider, initialized using a given (full) config and content dumping service */
  def withConfig(config: Config, dumpService: PageContentDumpService): ConfiguredProvider
}
