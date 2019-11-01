package com.funglejunk.stockz

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val localDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun LocalDate.toFboerseString() = format(localDateFormatter)

fun String.toLocalDate() = LocalDate.parse(this, localDateFormatter)

fun <T> LiveData<T>.mutable() = this as MutableLiveData<T>