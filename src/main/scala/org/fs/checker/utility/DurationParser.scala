package org.fs.checker.utility

import com.github.nscala_time.time.Imports._
import org.joda.time.Months

/**
 * @author FS
 */
object DurationParser {
  def parse(string: String): Duration = {
    // e.g. 2 месяца 5 дней
    val groupedSplit = (string split " " grouped 2).toSeq map (v =>
      v(0).toInt -> v(1)
    )
    val parsedParts = groupedSplit map (parseInner _).tupled
    parsedParts reduce ((a, b) => a + b)
  }

  private def parseInner(amount: Int, unit: String): Duration = {
    val now = DateTime.now
    val then = unit match {
      case x if Seq("секунда", "секунды", "секунд") contains x =>
        now - amount.seconds
      case x if Seq("минута", "минуты", "минут") contains x =>
        now - amount.minutes
      case x if Seq("час", "часа", "часов") contains x =>
        now - amount.hours
      case x if Seq("день", "дня", "дней") contains x =>
        now - amount.days
      case x if Seq("месяц", "месяца", "месяцев", "мес.") contains x =>
        now - amount.months
    }
    new Duration(then, now)
  }
}
