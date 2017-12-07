package org.fs.checker.web

import com.twitter.finagle.Service
import com.twitter.finagle.SimpleFilter
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status
import com.twitter.util.Future

/**
 * @author FS
 */
class ExceptionInterceptingFilter
    extends SimpleFilter[Request, Response] {
  import ExceptionInterceptingFilter._

  def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    service(request).rescue {
      case ex: IllegalArgumentException => Future {
        val res = Response(request.version, Status.BadRequest)
        res.setContentTypeJson()
        res.contentString = toJson(ex)
        res
      }
    }
  }
}

object ExceptionInterceptingFilter {
  import io.circe.generic.auto._
  import io.circe._

  def toJson(th: Throwable): String = {
    jsonExceptionEncoder(ExceptionResponse(th.getMessage)).noSpaces
  }

  private val jsonExceptionEncoder = implicitly[Encoder[ExceptionResponse]]

  private case class ExceptionResponse(message: String)
}
