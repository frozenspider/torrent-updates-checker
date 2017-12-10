package org.fs.checker.dumping

trait PageContentDumperService {
  def dump(content: String, providerName: String): Unit
}
