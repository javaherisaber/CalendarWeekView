package ir.logicbase.calendarweekview

/**
 * Similar to [android.graphics.Rect] but provides the
 * [.set] method to handle right-to-left mode.
 */
internal class DirectionalRect {
    var left: Int = 0
        private set
    var top: Int = 0
        private set
    var right: Int = 0
        private set
    var bottom: Int = 0
        private set

    /**
     * Sets the rect's points but factors in whether or not the device is in right-to-left mode.
     *
     * @param isRtl       whether or not the device is in right-to-left mode
     * @param parentWidth the width of the parent view of the rect, this will be used to figure out
     * how to translate
     * the rect in right-to-left mode
     * @param start       the start of the rect in left-to-right mode
     * @param top         the top of the rect, it will not be translated
     * @param end         the end of the rect in left-to-right mode
     * @param bottom      the bottom of the rect, it will not be translated
     */
    fun set(parentWidth: Int, start: Int, top: Int, end: Int, bottom: Int, isRtl: Boolean) {
        left = if (isRtl) parentWidth - end else start
        this.top = top
        right = if (isRtl) parentWidth - start else end
        this.bottom = bottom
    }

}