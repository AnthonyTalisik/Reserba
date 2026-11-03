package com.example.reserba.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TimeUtils {

    private const val MINUTE_MS = 60_000L

    private fun dateFmt() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun dateTimeFmt() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    fun toMillis(date: String, time: String): Long =
        dateTimeFmt().parse("$date $time")!!.time

    fun timeToMinutes(time: String): Int {
        val p = time.split(":")
        return p[0].toInt() * 60 + p[1].toInt()
    }

    fun minutesToTime(minutes: Int): String =
        String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)

    /** Rounds up to the next 30-minute step (used for "next free time" hints). */
    fun roundUp30(minutes: Int): Int = ((minutes + 29) / 30) * 30

    /** "13:30" -> "1:30 PM" */
    fun format12(time: String): String {
        val m = timeToMinutes(time)
        val h = m / 60
        val mm = m % 60
        val ampm = if (h < 12) "AM" else "PM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        return String.format(Locale.US, "%d:%02d %s", h12, mm, ampm)
    }

    fun formatRange(start: String, end: String) = "${format12(start)} - ${format12(end)}"

    fun formatDate(date: String): String =
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(dateFmt().parse(date)!!)

    /** Two-line label for the date strip, e.g. "Sat\n24". */
    fun dayLabel(date: String): String =
        SimpleDateFormat("EEE\nd", Locale.getDefault()).format(dateFmt().parse(date)!!)

    fun formatDateTime(millis: Long): String =
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(millis)

    fun dateOffset(days: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, days)
        return dateFmt().format(c.time)
    }

    fun dateFromParts(year: Int, month0: Int, day: Int): String =
        String.format(Locale.US, "%04d-%02d-%02d", year, month0 + 1, day)

    /**
     * Business rule BR-01.
     * A conflict exists when:  newStart < existingEnd + buffer  AND  newEnd + buffer > existingStart
     * Example: a room approved until 11:00 with a 20-minute buffer can next start at 11:20.
     */
    fun conflicts(
        newStart: Long, newEnd: Long,
        existingStart: Long, existingEnd: Long,
        bufferMinutes: Int
    ): Boolean {
        val buf = bufferMinutes * MINUTE_MS
        return newStart < existingEnd + buf && newEnd + buf > existingStart
    }

    /** Plain overlap with no buffer (used for blocked dates and the availability timeline). */
    fun overlaps(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long): Boolean =
        aStart < bEnd && aEnd > bStart
}
