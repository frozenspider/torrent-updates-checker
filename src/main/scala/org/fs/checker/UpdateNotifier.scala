package org.fs.checker

import org.slf4s.Logging
import scala.swing.Dialog
import scala.swing.Frame

/**
 * @author FS
 */
object UpdateNotifier extends Logging {
  def notifyUpdated(aliasesMap: Map[String, String]): Unit = {
    aliasesMap.foreach {
      case (alias, url) => log.warn(s"'$alias' ($url) was updated!")
    }

    val aliasesString = aliasesMap.keys.mkString(", ")
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
    Dialog.showMessage(parent = frame, message = s"$aliasesString updated", title = title)
    frame.dispose()
  }
}
