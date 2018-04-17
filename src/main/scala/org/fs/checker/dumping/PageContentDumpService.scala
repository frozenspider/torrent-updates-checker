package org.fs.checker.dumping

trait PageContentDumpService {
  def dump(content: String, providerName: String): Unit
}
