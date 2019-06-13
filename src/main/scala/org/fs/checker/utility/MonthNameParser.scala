package org.fs.checker.utility

import com.github.nscala_time.time.Imports._

/**
 * @author FS
 */
object MonthNameParser {
  private val monthStrings = IndexedSeq(
    Seq("янв", "январь", "января", "jan", "january"),
    Seq("фев", "февраль", "февраля", "feb", "february"),
    Seq("мар", "март", "марта", "mar", "march"),
    Seq("апр", "апрель", "апреля", "apr", "april"),
    Seq("май", "мая", "may"),
    Seq("июн", "июнь", "июня", "jun", "june"),
    Seq("июл", "июль", "июля", "jul", "july"),
    Seq("авг", "август", "августа", "aug", "august"),
    Seq("сен", "сентябрь", "сентрября", "sep", "september"),
    Seq("окт", "октябрь", "октября", "oct", "october"),
    Seq("ноя", "ноябрь", "ноября", "nov", "november"),
    Seq("дек", "декабрь", "декабря", "dec", "december")
  ) ensuring (_.size == 12)

  def parse(string: String): Int = {
    val normalizedInput = string.trim.toLowerCase
    val resOption = monthStrings.zipWithIndex find {
      case ((strings, idx)) => strings contains normalizedInput
    }
    resOption map (_._2 + 1) getOrElse (
      throw new IllegalArgumentException("Cannot parse month from '" + string + "'")
    )
  }
}

