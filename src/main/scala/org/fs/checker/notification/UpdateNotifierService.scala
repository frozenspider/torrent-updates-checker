package org.fs.checker.notification

import org.fs.checker.dao.TorrentEntry

trait UpdateNotifierService {
  /** Notify user about updates, if any. */
  def notify(updated: Seq[TorrentEntry], notAvailable: Seq[(TorrentEntry, String)]): Unit
}
