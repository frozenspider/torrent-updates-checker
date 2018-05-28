package org.fs.checker.utility

import com.typesafe.config._

trait ConfigImplicits {
  def quoted(s: String): String = "\"" + s.replaceAllLiterally("\"", "\\\"") + "\""

  def emptyConfig: Config = {
    ConfigFactory.empty()
  }

  def newConfig(pairs: (String, _)*): Config = {
    ConfigFactory.parseMap(scala.collection.JavaConverters.mapAsJavaMap(Map(pairs: _*)))
  }

  def newConfig(map: Map[String, _]): Config =
    newConfig(map.toList: _*)

  implicit class RichConfigObject(c: ConfigObject) {
    def toMap: Map[String, ConfigValue] = {
      scala.collection.JavaConverters.mapAsScalaMap(c).toMap
    }

    def keys: Iterable[String] = {
      toMap.keys
    }
  }

  implicit class RichConfig(c: Config) {
    def withValue(path: String, cfg: Config): Config =
      c.withValue(path, cfg.root)

    def withValue(path: String, any: Any): Config =
      c.withValue(path, ConfigValueFactory.fromAnyRef(any))
  }
}

object ConfigImplicits extends ConfigImplicits
