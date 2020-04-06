package ir.logicbase.calendarweekview.app

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import ir.logicbase.calendarweekview.TimeRange
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var currentMode: DisplayMode = DisplayMode.WEEK

    // dayView
    private val dayViewEventTimeRanges = listOf(TimeRange(180, 240), TimeRange(180, 300), TimeRange(300, 360))
    private val dayViewEventTitles = listOf("Wash dishes", "Buy groceries", "Call jessica")
    private val dayViewScheduleTimeRanges = listOf(TimeRange(60, 420), TimeRange(540, 900))

    // weekView
    private val weekViewDay0EventTimeRanges = listOf(TimeRange(180, 240))
    private val weekViewDay0ScheduleTimeRanges = listOf(TimeRange(60, 300))
    private val weekViewDay1EventTimeRanges = listOf(TimeRange(300, 360))
    private val weekViewDay1ScheduleTimeRanges = listOf(TimeRange(240, 360))
    private val weekViewDay4EventTimeRanges = listOf(TimeRange(300, 420))
    private val weekViewDay4ScheduleTimeRanges = listOf(TimeRange(300, 460))
    private val weekViewEventTimeRanges = mapOf(
        0 to weekViewDay0EventTimeRanges,
        1 to weekViewDay1EventTimeRanges,
        4 to weekViewDay4EventTimeRanges
    )
    private val weekViewScheduleTimeRanges = mapOf(
        0 to weekViewDay0ScheduleTimeRanges,
        1 to weekViewDay1ScheduleTimeRanges,
        4 to weekViewDay4ScheduleTimeRanges
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        inflateHourLabels()
        inflateWeekView()
    }

    @SuppressLint("SetTextI18n")
    private fun inflateHourLabels() = with(weekView) {
        val hourLabels = mutableListOf<View>()
        for (i in 0..24) {
            val hourLabel = layoutInflater.inflate(
                R.layout.view_hour_label, this, false
            ) as TextView
            hourLabel.text = "${i.toZeroTail()}:00"
            hourLabels.add(hourLabel)
        }
        setHourLabelViews(hourLabels)
    }

    private fun inflateDayView() = with(weekView) {
        val eventViews = mutableListOf<View>()
        val allRecycledEvents = removeEventViews()[0]
        var remaining = allRecycledEvents?.size ?: 0
        dayViewEventTitles.forEach {
            val recycledEvent = if (remaining > 0) {
                allRecycledEvents?.get(remaining)
            } else null
            remaining--
            eventViews.add(getDayViewEvent(recycledEvent, this, it))
        }
        setEventViews(mapOf(0 to eventViews), mapOf(0 to dayViewEventTimeRanges))
        setSchedules(mapOf(0 to dayViewScheduleTimeRanges))
        requestLayout()
    }

    private fun inflateWeekView() = with(weekView) {
        val allRecycledEvents = removeEventViews()
        val allEventViews = hashMapOf<Int, MutableList<View>>()
        for ((column , timeRanges) in weekViewEventTimeRanges) {
            val eventViews = mutableListOf<View>()
            var remaining = allRecycledEvents[column]?.size ?: 0
            repeat(timeRanges.size) {
                val recycledEvent = if (remaining > 0) {
                    allRecycledEvents[column]?.get(remaining)
                } else null
                remaining--
                eventViews.add(getWeekViewEvent(recycledEvent, this))
            }
            allEventViews[column] = eventViews
        }
        setEventViews(allEventViews, weekViewEventTimeRanges)
        setSchedules(weekViewScheduleTimeRanges)
        requestLayout()
    }

    private fun getDayViewEvent(recycled: View?, parent: ViewGroup, title: String): View {
        val event = (recycled ?: layoutInflater.inflate(R.layout.view_event_day_view, parent, false))
        event.findViewById<TextView>(R.id.textView_viewEventDayView_title).text = title
        event.setOnClickListener {
            Toast.makeText(this, "Clicked on event : $title", Toast.LENGTH_SHORT).show()
        }
        return event
    }

    private fun getWeekViewEvent(recycled: View?, parent: ViewGroup): View {
        val event = (recycled ?: layoutInflater.inflate(R.layout.view_event_week_view, parent, false))
        event.setOnClickListener {
            Toast.makeText(this, "Clicked on event", Toast.LENGTH_SHORT).show()
        }
        return event
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuItem_activityMain_displayMode -> {
                changeDisplayMode(item)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun changeDisplayMode(item: MenuItem) {
        when (currentMode) {
            DisplayMode.WEEK -> {
                setToolbarTitle(R.string.display_mode_day)
                item.icon = ContextCompat.getDrawable(this, R.drawable.ic_view_day_black_24dp)
                currentMode = DisplayMode.DAY
                weekView.dayCount = 1
                inflateDayView()
            }
            DisplayMode.DAY -> {
                setToolbarTitle(R.string.display_mode_week)
                item.icon = ContextCompat.getDrawable(this, R.drawable.ic_view_week_black_24dp)
                currentMode = DisplayMode.WEEK
                weekView.dayCount = 7
                inflateWeekView()
            }
        }
    }
}