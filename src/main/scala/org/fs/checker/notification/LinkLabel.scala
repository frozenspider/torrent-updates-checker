package org.fs.checker.notification

import java.awt.Color
import java.awt.Cursor
import java.awt.Desktop
import java.net.URI

import scala.swing.Label
import scala.swing.event.MouseClicked
import scala.swing.event.MouseEntered
import scala.swing.event.MouseExited

import org.fs.checker.notification.LinkLabel._
import org.slf4s.Logging

/**
 * Label used to display clickable HTML hyperlinks.
 *
 * @author FS
 */
class LinkLabel extends Label with Logging {
  var _uri: URI = _
  var _rawText: String = _
  private var _color: Color = LinkColor.Standard
  private var _underlined: Boolean = true
  private var _clicked: Boolean = false

  def this(text: String, uri: URI) = {
    this()
    this.uri = uri
    this.rawText = text
  }

  listenTo(this.mouse.moves, this.mouse.clicks)
  reactions += {
    case MouseEntered(_, _, _) =>
      cursor = new Cursor(Cursor.HAND_CURSOR)
      _underlined = false
      _color = LinkColor.Active
      restyle()
    case MouseExited(_, _, _) =>
      _underlined = true
      _color = if (_clicked) LinkColor.Visited else LinkColor.Standard
      cursor = new Cursor(Cursor.DEFAULT_CURSOR)
      restyle()
    case MouseClicked(_, _, _, clicksNum, _) if clicksNum == 1 =>
      openLinkIfPossible(uri)
      _underlined = true
      _color = LinkColor.Visited
      _clicked = true
      restyle()
  }

  def uri: URI = _uri

  def uri_=(uri: URI): Unit = {
    this._uri = uri
  }

  def rawText: String = _rawText

  def rawText_=(rawText: String): Unit = {
    this._rawText = rawText
    restyle()
  }

  /** Apply current style to raw text  */
  private def restyle(): Unit = {
    val fmtString = "<html><span style=\"color: #%02X%02X%02X;\">%s</span></html>"
    val text2 = if (_underlined) s"<u>$rawText</u>" else rawText
    val fmtArgs: Seq[Object] =
      (Seq(_color.getRed, _color.getGreen, _color.getBlue) map (new Integer(_))) :+ text2
    val text = String.format(fmtString, fmtArgs: _*)
    super.text = text
  }

  private def openLinkIfPossible(uri: URI): Unit = {
    try {
      // Only supported on Windows and Gnome
      // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6486393
      if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop.browse(uri)
      } else {
        // Code for other OS: https://binfalse.de/2011/01/03/adding-a-hyperlink-to-java-swing-gui/
        val osName = System.getProperty("os.name")
        val browser = System.getenv.get("BROWSER")
        if (osName.startsWith("Mac OS")) {
          val fileMgrClass = Class.forName("com.apple.eio.FileManager")
          val openUrlMethod = fileMgrClass.getDeclaredMethod("openURL", classOf[String])
          openUrlMethod.invoke(null, uri)
        } else if (browser != null) {
          Runtime.getRuntime.exec(s"$browser $uri")
        } else {
          log.warn(s"Can't open links on this system ($osName)")
        }
      }
    } catch {
      case ex: Exception =>
        log.error(s"Can't open link $uri", ex)
    }
  }
}

object LinkLabel {
  object LinkColor {
    // Source: https://www.w3.org/TR/html5/rendering.html#non-replaced-elements-phrasing-content
    val Standard = new Color(0x00, 0x00, 0xEE)
    val Visited = new Color(0x55, 0x1A, 0x8B)
    val Active = new Color(0xFF, 0x00, 0x00)
  }
}
