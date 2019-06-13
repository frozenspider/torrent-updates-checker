package org.fs.checker.provider

trait GenProvider {
  /** User-readable name for this provider */
  def prettyName: String

  /** Settings prefix for this provider */
  def providerKey: String

  /** Connection timeout */
  def timeoutMs: Int = 60 * 1000

  /** Whether or not the given URL can be checked by this provider */
  def recognizeUrl(url: String): Boolean
}
