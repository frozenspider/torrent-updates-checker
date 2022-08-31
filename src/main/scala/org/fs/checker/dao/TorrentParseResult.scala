package org.fs.checker.dao

import org.joda.time.DateTime

sealed trait TorrentParseResult
object TorrentParseResult {
  case class Success(dt: DateTime) extends TorrentParseResult
  case class NotAvailable(reason: String) extends TorrentParseResult
}
