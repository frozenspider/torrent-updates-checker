package org.fs.checker.dumping

/**
 * @author FS
 */
case class PageParsingException(providerName: String, url: String, content: String, th: Throwable)
  extends RuntimeException(th)
