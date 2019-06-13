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
  @throws[IllegalArgumentException]("if tracker instance creation fails, e.g. invalid credentials" +
    " (page content should already be dumped to disk)")
  @throws[IllegalStateException]("if tracker is accessible but isn't working, e.g. server-side maintenance")
  def withConfig(config: Config, dumpService: PageContentDumpService): ConfiguredProvider
}
