package org.fs.checker.dao

import org.joda.time.DateTime

sealed trait TorrentParseResult
object TorrentParseResult {
  case class Success(dt: DateTime) extends TorrentParseResult
  sealed class Failure(val reason: String) extends TorrentParseResult

  object Failure {
    case object NotFound extends Failure("not found")
    case object Absorbed extends Failure("absorbed")
    // Add more when needed
    case class Other(override val reason: String) extends Failure(reason)
  }
}
