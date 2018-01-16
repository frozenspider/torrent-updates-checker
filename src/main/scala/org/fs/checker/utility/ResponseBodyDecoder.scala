package org.fs.checker.utility

import java.nio.charset.Charset

import org.apache.http.entity.ContentType
import org.fs.utility.web.http.SimpleHttpResponse
import org.mozilla.universalchardet.UniversalDetector

object ResponseBodyDecoder {
  /**
   * Convert response body to string.
   * Necessary since some providers do not respond with `Content-Type`
   * whilst using non-default encoding.
   */
  def bodyToString(resp: SimpleHttpResponse): String = {
    val charset: Charset =
      resp.charsetOption orElse {
        val charsetDetector = new UniversalDetector
        charsetDetector.handleData(resp.body)
        charsetDetector.dataEnd()
        val charsetString = charsetDetector.getDetectedCharset
        Option(charsetString) map Charset.forName
      } getOrElse {
        ContentType.DEFAULT_TEXT.getCharset
      }
    new String(resp.body, charset)
  }
}
