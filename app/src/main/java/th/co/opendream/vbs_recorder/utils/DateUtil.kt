package th.co.opendream.vbs_recorder.utils

import android.util.Log
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DateUtil {
    companion object {
        fun formatLocaleDate(date: Date?): String {
            val defaultLocale = Locale.getDefault()
            return SimpleDateFormat("dd MMM yyyy", defaultLocale).format(date)
        }

        fun formatLocaleDateTime(date: Date?): String {
            val defaultLocale = Locale.getDefault()
            return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", defaultLocale).format(date)
        }

        fun formatLocaleDateTimeFile(date: Date?): String {
            val defaultLocale = Locale.getDefault()
            return SimpleDateFormat("yyyy-MM-dd'T'HHmmss_SSS", defaultLocale).format(date)
        }

        fun formatLocaleDateTitle(date: Date?): String {
            val defaultLocale = Locale.getDefault()
            return SimpleDateFormat("yyyy/MM/dd", defaultLocale).format(date)
        }

        fun formatLocaleTimeTitle(date: Date?): String {
            val defaultLocale = Locale.getDefault()
            return SimpleDateFormat("HH:mm:ss", defaultLocale).format(date)
        }

        fun fromJsonDateString(dateStr: String?, tz: Int?): Date? {
            val formats = arrayOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'z'",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ss.Z",
                "yyyy-MM-dd'T'HH:mm:ss.'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ss.SSSzzzz",  // Blogger Atom feed has millisecs also
                "yyyy-MM-dd'T'HH:mm:sszzzz",
                "yyyy-MM-dd'T'HH:mm:ss z",
                "yyyy-MM-dd'T'HH:mm:ssz",  // ISO_8601
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HHmmss.SSSz",
            )
            val defaultLocale = Locale.getDefault()
            var date: Date? = null
            for (format in formats) {
                try {
                    val parser =
                        SimpleDateFormat(format, defaultLocale)
                    date = parser.parse(dateStr)
                    val c = Calendar.getInstance()
                    c.time = date
                    if (tz != null) {
                        c.add(Calendar.HOUR, tz)
                    }
                    date = c.time
                    break
                } catch (e: ParseException) {
                    // Do nothing.
                }
            }
            return date
        }

        fun currentTimeToLong(): Long {
            return System.currentTimeMillis()
        }

        fun convertLongToTime(time: Long): String {
            val date = Date(time)
            val format = SimpleDateFormat("yyyy.MM.dd HH:mm")
            return format.format(date)
        }

        fun convertStringToTime(date: String, format: String, addDays: Int = 0): Long {
            Log.d("convertStringToTime", "date: $date, format: $format")

            val formatter = SimpleDateFormat(format)
            val date = formatter.parse(date)

            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.DATE, addDays)

            return calendar.timeInMillis
        }

    }


}