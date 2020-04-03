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
    private val dayViewEventTimeRanges = listOf(TimeRange(180, 240), TimeRange(180, 300), TimeRange(300, 360))
    private val dayViewEventTitles = listOf("Wash dishes", "Buy groceries", "Call jessica")
    private val dayViewScheduleTimeRanges = listOf(TimeRange(60, 420), TimeRange(540, 900))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        inflateHourLabels()
        setToolbarTitle(R.string.display_mode_day)
        inflateDayView()
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
        val allRecycledEvents = removeEventViews()
        var remaining = allRecycledEvents?.size ?: 0
        dayViewEventTitles.forEach {
            val recycledEvent = if (remaining > 0) {
                allRecycledEvents?.get(remaining)
            } else null
            remaining--
            eventViews.add(getDayViewEvent(recycledEvent, this, it))
        }
        setEventViews(eventViews, dayViewEventTimeRanges)
        setSchedules(dayViewScheduleTimeRanges)
    }

    private fun getDayViewEvent(recycled: View?, parent: ViewGroup, title: String): View {
        val event = (recycled ?: layoutInflater.inflate(R.layout.view_event_day_view, parent, false))
        event.findViewById<TextView>(R.id.textView_viewEventDayView_title).text = title
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
        Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show()
    }
}