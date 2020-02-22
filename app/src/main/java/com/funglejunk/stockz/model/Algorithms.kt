package com.funglejunk.stockz.model

import com.funglejunk.stockz.data.ChartValue
import com.funglejunk.stockz.data.RepoHistoryData
import com.funglejunk.stockz.toLocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
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
        it.first()
            .copy(value = simpleMovingAverageOnPartition(it) + 2 * standardDeviationOnPartition(it))
    }
    val lowerBands = partitions.map {
        it.first()
            .copy(value = simpleMovingAverageOnPartition(it) - 2 * standardDeviationOnPartition(it))
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

fun averageTrueRange(data: List<RepoHistoryData.Data>): List<ChartValue> {
    return when (data.isNotEmpty()) {
        true -> {
            val trueRanges = data.mapIndexed { index, currentValue ->
                when (index) {
                    0 -> ChartValue(
                        date = currentValue.date.toLocalDate(),
                        value = abs(currentValue.high - currentValue.close).toFloat()
                    )
                    else -> {
                        val previousValue = data[index - 1]
                        val s1 = abs(currentValue.high - currentValue.close)
                        val s2 = abs(previousValue.close - currentValue.high)
                        val s3 = abs(previousValue.close - currentValue.low)
                        val trueRange = max(max(s1, s2), s3).toFloat()
                        ChartValue(currentValue.date.toLocalDate(), trueRange)
                    }
                }
            }
            val partitions = trueRanges.partition(Period.DAYS_7, 2)
            partitions.foldIndexed(mutableListOf()) { index, acc, new ->
                when (index) {
                    0 -> {
                        acc.apply {
                            add(new.first().copy(
                                value = (new.sumByDouble { it.value.toDouble() } / new.size).toFloat()
                            ))
                        }
                    }
                    else -> {
                        val currentTr =
                            (partitions[index].sumByDouble { it.value.toDouble() } / partitions[index - 1].size).toFloat()
                        val nextAtr = (acc[index - 1].value * (index - 1) + currentTr) / index
                        acc.apply {
                            add(new.first().copy(value = nextAtr)) // TODO this might crash
                        }
                    }
                }
            }
        }
        false -> emptyList()
    }
}

fun List<ChartValue>.partition(
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