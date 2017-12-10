package org.fs.checker.dao

/**
 * @author FS
 */
trait TorrentDaoService {
  def list: Seq[TorrentEntry]

  def add(entry: TorrentEntry): Seq[TorrentEntry]

  def remove(alias: String): Seq[TorrentEntry]
}
