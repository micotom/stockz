package com.funglejunk.stockz.model

import com.funglejunk.stockz.data.ChartValue
import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import java.time.temporal.ChronoUnit
import kotlin.math.pow
import kotlin.math.sqrt

enum class Period(val days: Int) {
    DAYS_7(7), DAYS_30(30), DAYS_365(365)
}

fun simpleMovingAverage(
    data: List<ChartValue>,
    period: Period,
    periodInterval: Int
): List<ChartValue> =
    when (data.isNotEmpty()) {
        true -> {
            data.partition(period, periodInterval).filter {
                it.isNotEmpty()
            }.map {
                it.first().copy(value = simpleMovingAverageOnPartition(it))
            }
        }
        false -> emptyList()
    }

private fun simpleMovingAverageOnPartition(data: List<ChartValue>): Float {
    require(data.isNotEmpty())
    val av = data.sumByDouble { it.value.toDouble() }.toFloat() / data.size
    return av
}

fun bollingerBands(
    data: List<ChartValue>,
    period: Period,
    periodInterval: Int
): Pair<List<ChartValue>, List<ChartValue>> {
    val partitions = data.partition(period, periodInterval)
        .filter { it.isNotEmpty() }
    val upperBands = partitions.map {
        it.first().copy(value = simpleMovingAverageOnPartition(it) + 2 * standardDeviationOnPartition(it))
    }
    val lowerBands = partitions.map {
        it.first().copy(value = simpleMovingAverageOnPartition(it) - 2 * standardDeviationOnPartition(it))
    }
    return upperBands to lowerBands
}

private fun standardDeviationOnPartition(data: List<ChartValue>): Float {
    require(data.isNotEmpty())
    val mu = data.sumByDouble { it.value.toDouble() } / data.size
    val snv = data.map { (it.value - mu).pow(2.0) }
    val variance = snv.sumByDouble { it } / snv.size
    return sqrt(variance).toFloat()
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
