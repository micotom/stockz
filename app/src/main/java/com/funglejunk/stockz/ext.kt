package com.funglejunk.stockz

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.fx.IO
import com.funglejunk.stockz.data.ChartValue
import com.funglejunk.stockz.data.RepoHistoryData
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val localDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private val monthDayDateFormatter = DateTimeFormatter.ofPattern("MM-dd")

private val yearDateFormatter = DateTimeFormatter.ofPattern("yyyy")

fun LocalDate.toYearMonthDayString() = format(localDateFormatter)

fun LocalDate.toMonthDayString() = format(monthDayDateFormatter)

fun LocalDate.toYearString() = format(yearDateFormatter)

fun String.toLocalDate() = LocalDate.parse(this, localDateFormatter)

fun <T> LiveData<T>.mutable() = this as MutableLiveData<T>

fun <T> Fragment.withSafeContext(f: (Context) -> T) = context?.let { f(it) }

fun Float.round() = kotlin.math.round(this * 100) / 100

fun Double.round() = kotlin.math.round(this * 100) / 100

fun not(b: Boolean) = !b

fun not(b: IO<Boolean>) = b.map { not(it) }

fun RepoHistoryData.mapToDrawableData(): List<ChartValue> =
    content
        .map { dayHistory ->
            ChartValue(dayHistory.date.toLocalDate(), dayHistory.close.toFloat())
        }

private val percentFormat = NumberFormat.getPercentInstance().apply {
    minimumFractionDigits = 2
}
private val currencyFormat = NumberFormat.getCurrencyInstance().apply {
    minimumFractionDigits = 3
}
fun Double.textStringPercent() = percentFormat.format(this / 100.0)
fun BigDecimal.textStringPercent() = percentFormat.format(this / BigDecimal.valueOf(100.0))
fun BigDecimal.textStringCurrency() = currencyFormat.format(this)