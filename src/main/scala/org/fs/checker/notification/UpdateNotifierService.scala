package org.fs.checker.notification

import org.fs.checker.dao.TorrentEntry

/**
 * @author FS
 */
trait UpdateNotifierService {
  def notify(entries: Seq[TorrentEntry]): Unit
}
