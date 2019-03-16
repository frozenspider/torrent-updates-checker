package org.fs.checker.cache

import com.github.nscala_time.time.Imports._

case class CachedDetails(lastCheckMsOption: Option[Long], lastUpdateMsOption: Option[Long]) {
  def lastCheckDateOption: Option[DateTime] =
    lastCheckMsOption map (lastCheckMs => new DateTime(lastCheckMs))
}
