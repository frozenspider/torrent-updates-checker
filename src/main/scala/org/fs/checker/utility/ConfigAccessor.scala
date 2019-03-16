package org.fs.checker.utility

import scala.reflect.io.File

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions

class ConfigAccessor(configFile: File) {
  private lazy val writeFormat: ConfigRenderOptions =
    ConfigRenderOptions.concise
      .setFormatted(true)
      .setJson(false)

  def config: Config =
    this.synchronized {
      ConfigFactory.parseFileAnySyntax(configFile.jfile)
    }

  def update(newConfig: Config): Unit =
    this.synchronized {
      configFile.writeAll(newConfig.root.render(writeFormat))
    } ensuring {
      config == newConfig
    }
}
