package org.fs.checker.dumping

trait PageContentDumper {
  def dump(content: String, providerName: String): Unit
}
