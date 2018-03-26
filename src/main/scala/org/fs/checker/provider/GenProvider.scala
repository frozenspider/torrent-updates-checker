package org.fs.checker.provider

import java.io.File
import java.io.PrintWriter

import org.joda.time.DateTime

trait GenProvider {
  /** User-readable name for this provider */
  def prettyName: String

  /** Settings prefix for this provider */
  def providerKey: String

  /** Whether or not the given URL can be checked by this provider */
  def recognizeUrl(url: String): Boolean

  /** Dump the given HTML content to a file for further examination by hand */
  protected def dump(content: String): Unit = {
    val filename = s"${DateTime.now.toString("yyyy-MM-dd_HH-mm")}_${getClass.getSimpleName}.html"
    val file = new File(filename)
    val pw = new PrintWriter(file, "UTF-8")
    try {
      pw.write(content)
      pw.flush()
    } finally {
      pw.close()
    }
  }
}
