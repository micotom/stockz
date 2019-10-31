package com.funglejunk.stockz

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val localDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun LocalDate.toFboerseString() = format(localDateFormatter)

fun String.toLocalDate() = LocalDate.parse(this, localDateFormatter)