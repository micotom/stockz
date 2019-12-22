package com.funglejunk.stockz

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.core.Either
import arrow.fx.IO
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val localDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private val monthDayDateFormatter = DateTimeFormatter.ofPattern("MM-dd")

private val yearDateFormatter = DateTimeFormatter.ofPattern("yyyy")

fun LocalDate.toFboerseString() = format(localDateFormatter)

fun LocalDate.toMonthDayString() = format(monthDayDateFormatter)

fun LocalDate.toYearString() = format(yearDateFormatter)

fun String.toLocalDate() = LocalDate.parse(this, localDateFormatter)

fun <T> LiveData<T>.mutable() = this as MutableLiveData<T>

fun <T> Fragment.withSafeContext(f: (Context) -> T) = context?.let { f(it) }

fun Disposable.addTo(compositeDisposable: CompositeDisposable) =
    compositeDisposable.add(this)

fun Float.round() = kotlin.math.round(this * 100) / 100

fun Double.round() = kotlin.math.round(this * 100) / 100

fun <T, V> Single<T>.zipToPairWith(other: Single<V>) =
    zipWith(other, BiFunction<T, V, Pair<T, V>> { t, v ->
        t to v
    })

fun <T, E: Either<Throwable, T>> Single<E>.flatten() = flatMap {
    it.fold(
        { e -> Single.error<T>(e) },
        { Single.just(it) }
    )
}

fun not(b: Boolean) = !b

fun not(b: IO<Boolean>) = b.map { !it }