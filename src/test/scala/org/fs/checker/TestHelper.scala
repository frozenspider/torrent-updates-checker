package org.fs.checker

import java.io.File

import org.fs.checker.dao.TorrentDaoService
import org.fs.checker.dao.TorrentEntry
import org.scalatest.Suite
import java.nio.charset.StandardCharsets

trait TestHelper {
  val utf8 = StandardCharsets.UTF_8

  val resourcesFolder = new File("src/test/resources")

  val daoServiceMock: TestHelper.TorrentDaoServiceMock =
    new TestHelper.TorrentDaoServiceMock
}

object TestHelper {
  class TorrentDaoServiceMock extends TorrentDaoService {
    var storage = Seq.empty[TorrentEntry]

    def reset(): Unit = {
      storage = Seq.empty[TorrentEntry]
    }

    def list: Seq[TorrentEntry] =
      storage

    def add(entry: TorrentEntry): Seq[TorrentEntry] = {
      require(!storage.exists(_.alias == entry.alias), "Alias exists")
      require(!storage.exists(_.url == entry.url), "URL exists")
      storage = storage :+ entry
      storage
    }

    def remove(alias: String): Seq[TorrentEntry] = {
      storage = storage.filter(_.alias != alias)
      storage
    }
  }
}
