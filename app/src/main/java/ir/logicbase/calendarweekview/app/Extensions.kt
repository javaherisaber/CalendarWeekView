package ir.logicbase.calendarweekview.app

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.setToolbarTitle(@StringRes title: Int) = supportActionBar?.apply {
    setDisplayShowTitleEnabled(true)
    this.title = getString(title)
}

fun Int.toZeroTail(): String = if (this < 10) "0$this" else this.toString()