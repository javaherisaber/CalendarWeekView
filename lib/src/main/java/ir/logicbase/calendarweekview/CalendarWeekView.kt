package ir.logicbase.calendarweekview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.view.ViewCompat
import ir.logicbase.calendarweekview.event.ColumnSpansHelper
import kotlin.math.max
import kotlin.math.min

@Suppress("MemberVisibilityCanBePrivate", "unused")
class CalendarWeekView constructor(
    context: Context, attrs: AttributeSet?, defStyleAttr: Int, enableDrawing: Boolean
) : ViewGroup(context, attrs, defStyleAttr) {

    // rectangles being drawn directly onto canvas
    private val hourDividerRects: MutableList<DirectionalRect>
    private val halfHourDividerRects: MutableList<DirectionalRect>
    private val verticalDividerRects = mutableListOf<DirectionalRect>()
    private val scheduleRects = mutableListOf<DirectionalRect>()

    // rectangles to locate view on ViewGroup
    private val eventRects = mutableListOf<DirectionalRect>()
    private val hourLabelRects: MutableList<DirectionalRect>

    // paints
    private val hourDividerPaint: Paint
    private val halfHourDividerPaint: Paint
    private val verticalDividerPaint: Paint
    private val schedulePaint: Paint

    // views being drawn on ViewGroup
    private val hourLabelViews = mutableListOf<View>()
    private val eventViews = mutableListOf<View>()

    // time ranges
    private val scheduleTimeRanges = mutableListOf<TimeRange>()
    private val eventTimeRanges = mutableListOf<TimeRange>()

    // attrs
    private val startHour: Int
    private val startMinute: Int
    private val endHour: Int
    private val endMinute: Int
    private val minuteCount: Int
    private val hourLabelsCount: Int
    private val hourDividersCount: Int
    private val halfHourDividersCount: Int
    private val dividerHeight: Int // the height in pixels taken up by each hour and half-hour divider
    private val verticalDividerWidth: Int
    private val usableHalfHourHeight: Int
    private val hourLabelWidth: Int
    private val hourLabelMarginEnd: Int
    private val eventMargin: Int
    var dayCount: Int = 1
        set(value) {
            require(value in 1..7) { "dayCount can only be between 1 to 7" }
            field = value
            verticalDividerRects.clear()
            verticalDividerRects.addAll(MutableList(value) { DirectionalRect() })
            requestLayout()
        }

    private var eventColumnSpansHelper: ColumnSpansHelper? = null // helper class to calculate span width
    private var isRtl = false
    private var parentWidth = 0 // ViewGroup width
    private var minuteHeight = 0f // the height in pixels taken up by each minute

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : this(
        context, attrs, defStyleAttr, true
    )

    init {
        val array = context.obtainStyledAttributes(attrs, R.styleable.CalendarWeekView)
        // The total number of usable minutes in this day
        dayCount = array.getInt(R.styleable.CalendarWeekView_dayCount, 1)
        startHour = max(array.getInt(R.styleable.CalendarWeekView_startHour, MIN_START_HOUR), MIN_START_HOUR)
        startMinute = startHour * MINUTES_PER_HOUR
        endHour = min(array.getInt(R.styleable.CalendarWeekView_endHour, MAX_END_HOUR), MAX_END_HOUR)
        endMinute = endHour * MINUTES_PER_HOUR
        val hourCount = endHour - startHour
        minuteCount = hourCount * MINUTES_PER_HOUR

        // The hour labels and dividers count here is one more than the hours count so we can
        // include the start of the midnight hour of the next day, setHourLabelViews() expects
        // exactly this many labels
        hourLabelsCount = hourCount + 1
        hourDividersCount = hourCount + 1
        halfHourDividersCount = hourCount
        hourDividerRects = MutableList(hourDividersCount) { DirectionalRect() }
        halfHourDividerRects = MutableList(halfHourDividersCount) { DirectionalRect() }
        hourLabelRects = MutableList(hourLabelsCount) { DirectionalRect() }

        dividerHeight = array.getDimensionPixelSize(R.styleable.CalendarWeekView_dividerHeight, 1)
        verticalDividerWidth = array.getDimensionPixelSize(R.styleable.CalendarWeekView_verticalDividerWidth, 1)
        usableHalfHourHeight = dividerHeight + array.getDimensionPixelSize(R.styleable.CalendarWeekView_halfHourHeight, 0)
        hourDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        halfHourDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        verticalDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        schedulePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        // This view draws its hour and half hour dividers directly
        if (enableDrawing) {
            setWillNotDraw(false)
            hourDividerPaint.color = array.getColor(R.styleable.CalendarWeekView_hourDividerColor, 0)
            halfHourDividerPaint.color = array.getColor(R.styleable.CalendarWeekView_halfHourDividerColor, 0)
            verticalDividerPaint.color = array.getColor(R.styleable.CalendarWeekView_verticalDividerColor, 0)
        }
        hourLabelWidth = array.getDimensionPixelSize(R.styleable.CalendarWeekView_hourLabelWidth, 0)
        hourLabelMarginEnd = array.getDimensionPixelSize(R.styleable.CalendarWeekView_hourLabelMarginEnd, 0)
        eventMargin = array.getDimensionPixelSize(R.styleable.CalendarWeekView_eventMargin, 0)
        schedulePaint.color = array.getColor(R.styleable.CalendarWeekView_scheduleColor, 0)
        array.recycle()
    }

    /**
     * @param hourLabelViews the list of views to show as labels for each hour, this list must not
     * be null and its length must be [.hourLabelsCount]
     */
    fun setHourLabelViews(hourLabelViews: List<View>) {
        removeViews(this.hourLabelViews)
        this.hourLabelViews.clear()
        this.hourLabelViews.addAll(hourLabelViews)
        for (view in this.hourLabelViews) {
            addView(view)
        }
    }

    fun removeViews(views: List<View>) {
        for (view in views) {
            removeView(view)
        }
    }

    /**
     * @param eventViews      the list of event views to display
     * @param eventTimeRanges the list of event params that describe each event view's start/end
     * times, this list must be equal in length to the list of event views,
     * or both should be null
     */
    fun setEventViews(eventViews: List<View>?, eventTimeRanges: List<TimeRange>?) {
        removeViews(this.eventViews)
        this.eventViews.clear()
        this.eventTimeRanges.clear()
        this.eventRects.clear()
        eventColumnSpansHelper = null
        if (!eventViews.isNullOrEmpty() && !eventTimeRanges.isNullOrEmpty()) {
            this.eventViews.addAll(eventViews)
            eventTimeRanges.forEach {
                if (it.endMinute > startMinute && it.startMinute < endMinute) {
                    this.eventTimeRanges.add(it)
                }
            }
            eventColumnSpansHelper = ColumnSpansHelper(this.eventTimeRanges)
            for (view in this.eventViews) {
                addView(view)
                eventRects.add(DirectionalRect())
            }
        }
    }

    fun setSchedules(timeRanges: List<TimeRange>?) {
        scheduleRects.clear()
        scheduleTimeRanges.clear()
        if (!timeRanges.isNullOrEmpty()) {
            scheduleRects.addAll(MutableList(timeRanges.size) { DirectionalRect() })
            scheduleTimeRanges.addAll(timeRanges)
        }
        requestLayout()
    }

    /**
     * Removes all of the existing event views.
     *
     * @return the event views that have been removed, they are safe to recycle and reuse at this
     * point
     */
    fun removeEventViews(): List<View>? {
        val eventViews: List<View> = eventViews
        setEventViews(null, null)
        return eventViews
    }

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the top of the given hour.
     *
     * @param hour the hour of the day, should be between 0 (12:00 AM of the current day) and 24
     * (12:00 AM of the next day)
     * @return the vertical offset of the top of the given hour in pixels
     */
    fun getHourTop(hour: Int): Int {
        checkHour(hour)
        return hourDividerRects[hour].bottom
    }

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the bottom of the given hour.
     *
     * @param hour the hour of the day, should be between 0 (12:00 AM of the current day) and 24
     * (12:00 AM of the next day)
     * @return the vertical offset of the bottom of the given hour in pixels
     */
    fun getHourBottom(hour: Int): Int {
        checkHour(hour)
        return if (hour == hourLabelsCount - 1) {
            hourDividerRects[hour].bottom
        } else hourDividerRects[hour + 1].top
    }

    private fun checkHour(hour: Int) = check(hour > 0 || hour <= hourLabelsCount) {
        "Hour must be between 0 and $hourLabelsCount"
    }

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the top of the first event.
     *
     * @return the vertical offset of the top of the first event in pixels, or zero if there are no
     * events
     */
    val firstEventTop: Int
        get() = if (eventRects.isNotEmpty()) eventRects[0].top else 0

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the bottom of the first event.
     *
     * @return the vertical offset of the bottom of the first event in pixels, or zero if there are
     * no events
     */
    val firstEventBottom: Int
        get() = if (eventRects.isNotEmpty()) eventRects[0].bottom else 0

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the top of the last event.
     *
     * @return the vertical offset of the top of the last event in pixels, or zero if there are no
     * events
     */
    val lastEventTop: Int
        get() = if (eventRects.isNotEmpty()) eventRects.last().top else 0

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the bottom of the last event.
     *
     * @return the vertical offset of the bottom of the last event in pixels, or zero if there are
     * no events
     */
    val lastEventBottom: Int
        get() = if (eventRects.isNotEmpty()) eventRects.last().bottom else 0

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (index in hourLabelViews.indices) {
            val view = hourLabelViews[index]
            val rect = hourLabelRects[index]
            view.layout(rect.left, rect.top, rect.right, rect.bottom)
        }
        for (index in eventViews.indices) {
            val view = eventViews[index]
            val rect = eventRects[index]
            view.layout(rect.left, rect.top, rect.right, rect.bottom)
        }
    }

    override fun shouldDelayChildPressedState(): Boolean = false

    private fun Canvas.drawRect(rect: DirectionalRect, paint: Paint) {
        this.drawRect(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat(), paint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (rect in hourDividerRects) {
            canvas.drawRect(rect, hourDividerPaint)
        }
        for (rect in halfHourDividerRects) {
            canvas.drawRect(rect, halfHourDividerPaint)
        }
        for (rect in verticalDividerRects) {
            canvas.drawRect(rect, verticalDividerPaint)
        }
        for (i in scheduleRects.indices) {
            canvas.drawRect(scheduleRects[i], schedulePaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        validateChildViews()

        isRtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL

        // Start with the default measured dimension
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        parentWidth = measuredWidth

        // Measure the hour labels using two passes, this first pass is only to figure out the
        // heights
        val hourLabelStart = if (isRtl) paddingRight else paddingLeft
        val hourLabelEnd = hourLabelStart + hourLabelWidth
        var firstDividerTop = 0
        var lastDividerMarginBottom = 0
        hourLabelViews.forEachIndexed { index, view ->
            measureChild(view, widthMeasureSpec, heightMeasureSpec)
            if (index == 0) {
                firstDividerTop = view.measuredHeight / 2
            } else if (index == hourLabelViews.lastIndex) {
                lastDividerMarginBottom = view.measuredHeight / 2
            }
        }

        // Calculate the measured height
        val usableHeight = (hourDividerRects.size + halfHourDividerRects.size - 1) * usableHalfHourHeight
        minuteHeight = usableHeight.toFloat() / minuteCount
        firstDividerTop += paddingTop
        val verticalPadding = firstDividerTop + lastDividerMarginBottom + paddingBottom + dividerHeight
        val measuredHeight = usableHeight + verticalPadding

        // Calculate the horizontal positions of the dividers
        val dividerStart = hourLabelEnd + hourLabelMarginEnd
        val dividerEnd = measuredWidth - if (isRtl) paddingLeft else paddingRight

        // Set the rects for hour labels, dividers, and events
        setHourLabelRects(hourLabelStart, hourLabelEnd, firstDividerTop)
        setDividerRects(firstDividerTop, dividerStart, dividerEnd)
        setEventRects(firstDividerTop, minuteHeight, dividerStart, dividerEnd)
        setScheduleRects(firstDividerTop, minuteHeight, dividerStart, dividerEnd)

        // Measure the hour labels and events for a final time
        measureHourLabels()
        measureEvents()
        setMeasuredDimension(widthMeasureSpec, measuredHeight)
    }

    private fun measureExactly(view: View, rect: DirectionalRect) {
        view.measure(
            MeasureSpec.makeMeasureSpec(rect.right - rect.left, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(rect.bottom - rect.top, MeasureSpec.EXACTLY)
        )
    }

    /**
     * Sets the dimensions of a rect while factoring in whether or not right-to-left mode is on.
     *
     * @param rect   the rect to update
     * @param start  the start of the rect in left-to-right mode
     * @param top    the top of the rect, it will not be translated
     * @param end    the end of the rect in left-to-right mode
     * @param bottom the bottom of the rect, it will not be translated
     */
    private fun setRect(rect: DirectionalRect, start: Int, top: Int, end: Int, bottom: Int) {
        rect.set(parentWidth, start, top, end, bottom, isRtl)
    }

    private fun setHourLabelRects(hourLabelStart: Int, hourLabelEnd: Int, firstDividerTop: Int) {
        for (index in hourLabelViews.indices) {
            val view = hourLabelViews[index]
            val height = view.measuredHeight
            val top = firstDividerTop + usableHalfHourHeight * index * 2 - height / 2
            val bottom = top + height
            setRect(hourLabelRects[index], hourLabelStart, top, hourLabelEnd, bottom)
        }
    }

    private fun setDividerRects(firstDividerTop: Int, dividerStart: Int, dividerEnd: Int) {
        for (index in hourDividerRects.indices) {
            val top = firstDividerTop + index * 2 * usableHalfHourHeight
            val bottom = top + dividerHeight
            setRect(hourDividerRects[index], dividerStart, top, dividerEnd, bottom)
        }
        for (index in halfHourDividerRects.indices) {
            val top = firstDividerTop + (index * 2 + 1) * usableHalfHourHeight
            val bottom = top + dividerHeight
            setRect(halfHourDividerRects[index], dividerStart, top, dividerEnd, bottom)
        }
        val verticalDividerBottom = (firstDividerTop * 2) + (hourDividerRects.size * 2 + 1) * usableHalfHourHeight
        val verticalDividerOffset = (dividerEnd - dividerStart) / verticalDividerRects.size
        for (index in verticalDividerRects.indices) {
            val start = dividerStart + (index * verticalDividerOffset)
            setRect(verticalDividerRects[index], start, 0, start + verticalDividerWidth, verticalDividerBottom)
        }
    }

    private fun setEventRects(firstDividerTop: Int, minuteHeight: Float, dividerStart: Int, dividerEnd: Int) {
        if (eventColumnSpansHelper == null) {
            return
        }
        val eventColumnWidth = if (eventColumnSpansHelper!!.columnCount > 0) {
            (dividerEnd - dividerStart) / eventColumnSpansHelper!!.columnCount
        } else 0
        eventTimeRanges.forEachIndexed { index, timeRange ->
            val columnSpan = eventColumnSpansHelper!!.columnSpans[index]
            var filteredStartMinute = max(startMinute, timeRange.startMinute)
            var duration = min(endMinute, timeRange.endMinute) - filteredStartMinute
            if (duration < MIN_DURATION_MINUTES) {
                duration = MIN_DURATION_MINUTES
                filteredStartMinute = endMinute - duration
            }
            val start = columnSpan.startColumn * eventColumnWidth + dividerStart + eventMargin
            val end = start + (columnSpan.endColumn - columnSpan.startColumn) * eventColumnWidth - eventMargin * 2
            val topOffset = ((filteredStartMinute - startMinute) * minuteHeight).toInt()
            val top = firstDividerTop + topOffset + dividerHeight + eventMargin
            val bottom = top + (duration * minuteHeight).toInt() - eventMargin * 2 - dividerHeight
            setRect(eventRects[index], start, top, end, bottom)
        }
    }

    private fun setScheduleRects(firstDividerTop: Int, minuteHeight: Float, dividerStart: Int, dividerEnd: Int) {
        scheduleTimeRanges.forEachIndexed { index, timeRange ->
            var filteredStartMinute = max(startMinute, timeRange.startMinute)
            var duration = min(endMinute, timeRange.endMinute) - filteredStartMinute
            if (duration < MIN_DURATION_MINUTES) {
                duration = MIN_DURATION_MINUTES
                filteredStartMinute = endMinute - duration
            }
            val topOffset = ((filteredStartMinute - startMinute) * minuteHeight).toInt()
            val top = firstDividerTop + topOffset + dividerHeight
            val bottom = top + (duration * minuteHeight).toInt() - dividerHeight
            setRect(scheduleRects[index], dividerStart, top, dividerEnd, bottom)
        }
    }

    /**
     * Validates the state of the child views during [.onMeasure].
     *
     * @throws IllegalStateException thrown when one or more of the child views are not in a valid
     * state
     */
    @CallSuper
    @Throws(IllegalStateException::class)
    private fun validateChildViews() {
        check(hourLabelViews.size != 0) {
            "No hour label views, setHourLabelViews() must be called before this view is rendered"
        }
        check(hourLabelViews.size == hourLabelsCount) {
            "Inconsistent number of hour label views, there should be $hourLabelsCount but ${hourLabelViews.size} were found"
        }
    }

    private fun measureHourLabels() {
        hourLabelViews.forEachIndexed { index, view -> measureExactly(view, hourLabelRects[index]) }
    }

    private fun measureEvents() {
        eventViews.forEachIndexed { index, view -> measureExactly(view, eventRects[index]) }
    }

    companion object {
        /**
         * Because of daylight saving time, some days are shorter or longer than 24 hours. Most calendar
         * apps assume there are 24 hours in each day, and then to handle events that span a daylight
         * saving time switch those events are adjusted. For example, when daylight saving time begins,
         * an event from 1:00 AM to 3:00 AM would only last an hour since the switch happens at 2:00 AM.
         * This means for events that span the beginning of daylight saving time, they will be drawn
         * with an extra hour. For events that span the end of daylight saving time, they'll be drawn at
         * the minimum height for an event if the event's duration is roughly an hour or less.
         */
        const val MIN_START_HOUR = 0
        const val MAX_END_HOUR = 24
        private const val MINUTES_PER_HOUR = 60
        private const val MIN_DURATION_MINUTES = 15
    }
}