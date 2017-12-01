package org.fs.checker

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.Dialog
import scala.swing.Frame

import org.slf4s.Logging

/**
 * @author FS
 */
object UpdateNotifier extends Logging {
  def notifyUpdated(aliasesMap: Map[String, String]): Unit = {
    aliasesMap.foreach {
      case (alias, url) => log.warn(s"'$alias' ($url) was updated!")
    }

    val aliasesString = aliasesMap.keys.mkString(", ")
    Future {
      val title = "Torrents updated"
      // Frame is dummy constructed for the dialog to be shown on the Windows taskbar
      val frame = {
        val frame = new Frame()
        frame.title = title
        frame.peer.setUndecorated(true)
        frame.peer.setLocationRelativeTo(null)
        frame
      }
      frame.visible = true
      Dialog.showMessage(
        parent  = frame,
        message = s"$aliasesString updated",
        title   = title
      )
      frame.dispose()
    }
  }
}
