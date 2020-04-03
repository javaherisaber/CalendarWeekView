package ir.logicbase.calendarweekview

/**
 * Represents the start and end time of a calendar entity.
 * Both times are in minutes since the start of the day.
 */
class TimeRange(val startMinute: Int, val endMinute: Int) {
    /**
     * @param range the time range to compare
     * @return true if the time range to compare overlaps in any way with this time range
     */
    fun conflicts(range: TimeRange): Boolean {
        return startMinute >= range.startMinute && startMinute < range.endMinute
                || endMinute > range.startMinute && endMinute <= range.endMinute
                || range.startMinute in startMinute until endMinute
                || range.endMinute in (startMinute + 1)..endMinute
    }
}