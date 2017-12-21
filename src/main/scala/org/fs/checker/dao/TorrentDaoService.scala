package org.fs.checker.dao

/**
 * @author FS
 */
trait TorrentDaoService {
  def list: Seq[TorrentEntry]

  def add(entry: TorrentEntry): Unit

  def remove(alias: String): Unit
}
