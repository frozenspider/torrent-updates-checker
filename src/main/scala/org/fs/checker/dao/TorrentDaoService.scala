package org.fs.checker.dao

/**
 * @author FS
 */
trait TorrentDaoService {
  /** List existing entries */
  def list: Seq[TorrentEntry]

  /** Append entry to the end of the checked list */
  def add(entry: TorrentEntry): Unit

  /** Remove entry by its alias */
  def remove(alias: String): Unit
}
