package com.funglejunk.stockz

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.core.Either
import arrow.fx.IO
import com.github.kittinunf.fuel.core.ResponseResultOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val localDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private val monthDayDateFormatter = DateTimeFormatter.ofPattern("MM-dd")

private val yearDateFormatter = DateTimeFormatter.ofPattern("yyyy")

fun LocalDate.toMonthDayString() = format(monthDayDateFormatter)

fun LocalDate.toYearString() = format(yearDateFormatter)

fun String.toLocalDate() = LocalDate.parse(this, localDateFormatter)

fun <T> LiveData<T>.mutable() = this as MutableLiveData<T>

fun <T> Fragment.withSafeContext(f: (Context) -> T) = context?.let { f(it) }

fun Float.round() = kotlin.math.round(this * 100) / 100

fun Double.round() = kotlin.math.round(this * 100) / 100

fun not(b: Boolean) = !b

fun not(b: IO<Boolean>) = b.map { !it }

fun <T: Any> ResponseResultOf<T>.toEither(): Either<Throwable, T> = third.fold(
    { Either.right(it) },
    { Either.left(it) }
)