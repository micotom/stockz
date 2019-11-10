package com.funglejunk.stockz

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val localDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun LocalDate.toFboerseString() = format(localDateFormatter)

fun String.toLocalDate() = LocalDate.parse(this, localDateFormatter)

fun <T> LiveData<T>.mutable() = this as MutableLiveData<T>

fun <T> Fragment.withSafeContext(f: (Context) -> T) = context?.let { f(it) }
