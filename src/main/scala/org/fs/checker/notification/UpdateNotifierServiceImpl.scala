package org.fs.checker.notification

import java.awt.Toolkit
import java.net.URI

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.BorderPanel
import scala.swing.BorderPanel.Position._
import scala.swing.Dialog
import scala.swing.FlowPanel
import scala.swing.Frame
import scala.swing.Label
import scala.swing.event.WindowOpened

import org.fs.checker.dao.TorrentEntry
import org.slf4s.Logging

/**
 * @author FS
 */
class UpdateNotifierServiceImpl extends UpdateNotifierService with Logging {
  override def notify(entries: Seq[TorrentEntry]): Unit =
    notifyInner(entries)

  def notifyInner(entries: Seq[TorrentEntry]): Future[Unit] = {
    entries.foreach {
      case TorrentEntry(alias, url) => log.warn(s"'$alias' ($url) was updated!")
    }

    Future {
      val linkLabels = entries map (e => new LinkLabel(e.alias, new URI(e.url)))
      val commaLabels = Seq.fill(linkLabels.size - 1)(new Label(", "))
      val labels = linkLabels.head +: commaLabels.zip(linkLabels.tail).flatMap(p => Seq(p._1, p._2)) :+ new Label(" updated!")

      val title = "Torrents updated"
      // Frame is dummy constructed for the dialog to be shown on the Windows taskbar
      val frame = {
        val frame = new Frame()
        frame.title = title
        frame.peer.setUndecorated(true)
        frame.peer.setLocationRelativeTo(null)
        frame
      }
      frame.reactions += {
        case e: WindowOpened => Toolkit.getDefaultToolkit().beep()
      }
      frame.visible = true
      val content = new BorderPanel {
        val inner = new FlowPanel(labels: _*) {
          hGap = 1
          vGap = 1
        }
        layout(inner) = West
      }
      Dialog.showMessage(
        parent  = frame,
        message = content.peer,
        title   = title
      )
      frame.dispose()
    }
  }
}

object UpdateNotifierServiceImpl extends App {
  import scala.concurrent._
  import scala.concurrent.duration._

  val f = (new UpdateNotifierServiceImpl).notifyInner(Seq(
    TorrentEntry("проверка", "http://www.example.com"),
    TorrentEntry("google", "http://www.google.com")
  ))
  Await.ready(f, 100.second)
}
