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
  override def notify(updated: Seq[TorrentEntry], notAvailable: Seq[(TorrentEntry, String)]): Unit = {
    if (updated.nonEmpty || notAvailable.nonEmpty) {
      notifyInner(updated, notAvailable)
    }
  }

  def notifyInner(updated: Seq[TorrentEntry], notAvailable: Seq[(TorrentEntry, String)]): Future[Unit] = {
    updated.foreach {
      case TorrentEntry(alias, url) => log.warn(s"'$alias' ($url) was updated!")
    }
    notAvailable.foreach {
      case (TorrentEntry(alias, url), reason) => log.warn(s"'$alias' ($url) is invalid: $reason!")
    }

    Future {
      val updatedLabels = if (updated.nonEmpty) {
        val linkLabels = updated map (e => new LinkLabel(e.alias, new URI(e.url)))
        val commaLabels = Seq.fill(linkLabels.size - 1)(new Label(", "))
        linkLabels.head +: commaLabels.zip(linkLabels.tail).flatMap(p => Seq(p._1, p._2)) :+ new Label(" updated! ")
      } else Seq.empty

      val notAvailableLabels = if (notAvailable.nonEmpty) {
        val linkLabels = notAvailable map (e => new LinkLabel(e._1.alias, new URI(e._1.url)))
        val commaLabels = Seq.fill(linkLabels.size - 1)(new Label(", "))
        linkLabels.head +: commaLabels.zip(linkLabels.tail).flatMap(p => Seq(p._1, p._2)) :+ new Label(" unavailable!")
      } else Seq.empty

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
        val inner = new FlowPanel((updatedLabels ++ notAvailableLabels): _*) {
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
  ), Seq(
    (TorrentEntry("qwe", "http://www.example.com"), "!11")
  ))
  Await.ready(f, 100.second)
}
