package com.funglejunk.stockz.model

import com.funglejunk.stockz.data.ChartValue
import java.time.temporal.ChronoUnit

enum class Period(val days: Int) {
    DAYS_7(7), DAYS_30(30), DAYS_365(365)
}

fun sma(data: List<ChartValue>, period: Period, periodInterval: Int): List<ChartValue> {
    return when (data.isNotEmpty()) {
        true -> {
            data.partition(period, periodInterval).filter {
                it.isNotEmpty()
            }.fold(mutableListOf()) { acc, new ->
                val av = new.sumByDouble { it.value.toDouble() } / new.size
                val first = new.first()
                val avEntry = first.copy(value = av.toFloat())
                acc.apply {
                    add(avEntry)
                }
            }
        }
        false -> emptyList()
    }
}

private fun List<ChartValue>.partition(
    period: Period,
    periodInterval: Int
): List<List<ChartValue>> =
    when (isNotEmpty()) {
        true -> {
            val sortedData = sortedBy { it.date }
            val daysInterval = period.days * periodInterval
            val minEntry = sortedData.minBy { it.date }!!
            val init = mutableListOf(mutableListOf(minEntry))
            sortedData.fold(init) { buckets, new ->
                val currentBucket = buckets.last()
                val currentMin = currentBucket.first()
                if (new == currentMin) {
                    buckets
                } else {
                    val daysBetween = ChronoUnit.DAYS.between(currentMin.date, new.date)
                    if (daysBetween <= daysInterval) {
                        currentBucket.add(new)
                        buckets
                    } else {
                        val newBucket = mutableListOf(new)
                        buckets.apply {
                            add(newBucket)
                        }
                    }
                }
            }
        }
        false -> emptyList()
    }
