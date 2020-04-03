package ir.logicbase.calendarweekview.event

import ir.logicbase.calendarweekview.TimeRange
import kotlin.math.max

/**
 * Helps calculate the start and end columns for a collection of calendar events.
 */
internal class ColumnSpansHelper(private val timeRanges: List<TimeRange>) {
    val columnSpans: MutableList<ColumnSpan>
    var columnCount = 0

    init {
        columnSpans = ArrayList(timeRanges.size)
        // Find the start and end columns for each event
        for (index in timeRanges.indices) {
            findStartColumn(index)
        }
        for (index in timeRanges.indices) {
            findEndColumn(index)
        }
    }

    private fun findStartColumn(position: Int) {
        for (index in timeRanges.indices) {
            if (isColumnEmpty(index, position)) {
                val columnSpan =
                    ColumnSpan()
                columnSpan.startColumn = index
                columnSpan.endColumn = index + 1
                columnSpans.add(columnSpan)
                columnCount = max(columnCount, index + 1)
                break
            }
        }
    }

    private fun findEndColumn(position: Int) {
        val columnSpan = columnSpans[position]
        for (index in columnSpan.endColumn until columnCount) {
            if (!isColumnEmpty(index, position)) {
                break
            }
            columnSpan.endColumn++
        }
    }

    private fun isColumnEmpty(column: Int, position: Int): Boolean {
        val timeRange = timeRanges[position]
        for (index in columnSpans.indices) {
            if (position == index) {
                continue
            }
            val compareTimeRange = timeRanges[index]
            val compareColumnSpan = columnSpans[index]
            if (compareColumnSpan.startColumn == column && compareTimeRange.conflicts(timeRange)) {
                return false
            }
        }
        return true
    }
}