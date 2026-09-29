package com.example.ui.components

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Sanity ceiling for a single manually-entered amount — prevents fat-fingered extra zeros
 *  from producing values that break card/chart layouts. Does not apply to PDF-parsed amounts,
 *  which reflect real statement data and shouldn't be second-guessed. */
const val MAX_MANUAL_AMOUNT = 1_00_00_000.0

object CurrencyFormatUtils {

    /**
     * Formats number in Indian Numbering System (e.g., ₹1,45,230 or ₹4,500.50)
     */
    fun formatInr(amount: Double, includeDecimals: Boolean = false): String {
        val isNegative = amount < 0
        val absAmount = Math.abs(amount)
        val wholePart = absAmount.toLong()
        val decimalPart = if (includeDecimals && (absAmount - wholePart > 0.001)) {
            String.format(Locale.US, "%.2f", absAmount - wholePart).substring(1)
        } else ""

        val wholeString = wholePart.toString()
        val formattedWhole = if (wholeString.length <= 3) {
            wholeString
        } else {
            val lastThree = wholeString.takeLast(3)
            val rest = wholeString.dropLast(3)
            val groups = mutableListOf<String>()
            var temp = rest
            while (temp.length > 2) {
                groups.add(0, temp.takeLast(2))
                temp = temp.dropLast(2)
            }
            if (temp.isNotEmpty()) {
                groups.add(0, temp)
            }
            groups.joinToString(",") + "," + lastThree
        }

        val prefix = if (isNegative) "-₹" else "₹"
        return "$prefix$formattedWhole$decimalPart"
    }

    fun formatDateFull(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateDayMonth(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatMonthYear(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatMonthShortYear(year: Int, monthIndex: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthIndex)
        val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun getMonthRange(year: Int, monthIndex: Int): Pair<Long, Long> {
        val startCal = Calendar.getInstance()
        startCal.set(year, monthIndex, 1, 0, 0, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val endCal = Calendar.getInstance()
        endCal.set(year, monthIndex, startCal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        endCal.set(Calendar.MILLISECOND, 999)

        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    fun getPast12MonthsList(): List<Pair<Int, Int>> {
        val list = mutableListOf<Pair<Int, Int>>()
        val cal = Calendar.getInstance()
        for (i in 0..11) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            list.add(Pair(c.get(Calendar.YEAR), c.get(Calendar.MONTH)))
        }
        return list
    }

    /** Returns the Monday 00:00:00.000 to Sunday 23:59:59.999 range containing [anchorMillis]. */
    fun getWeekRange(anchorMillis: Long): Pair<Long, Long> {
        val startCal = Calendar.getInstance().apply {
            timeInMillis = anchorMillis
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    fun formatWeekRange(startMillis: Long, endMillis: Long): String {
        val sameMonth = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(startMillis)) ==
            SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(endMillis))
        val startFmt = if (sameMonth) SimpleDateFormat("d", Locale.getDefault()) else SimpleDateFormat("d MMM", Locale.getDefault())
        val endFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        return "${startFmt.format(Date(startMillis))} - ${endFmt.format(Date(endMillis))}"
    }

    fun getYearRange(year: Int): Pair<Long, Long> {
        val startCal = Calendar.getInstance().apply {
            set(year, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(year, Calendar.DECEMBER, 31, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    /**
     * Material3's date pickers report selections as UTC midnight for the picked calendar day,
     * regardless of device timezone. Our stored transaction dates use local-midnight timestamps
     * (from [java.text.SimpleDateFormat] parsing), so a raw UTC millis value can't be compared
     * against them directly without shifting the effective day in non-UTC timezones (e.g. IST).
     * This re-derives the picked calendar day and returns its local start/end-of-day range.
     */
    fun localDayRangeFromUtcMillis(utcMillis: Long): Pair<Long, Long> {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        val year = utcCal.get(Calendar.YEAR)
        val month = utcCal.get(Calendar.MONTH)
        val day = utcCal.get(Calendar.DAY_OF_MONTH)

        val startCal = Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(year, month, day, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }
}
