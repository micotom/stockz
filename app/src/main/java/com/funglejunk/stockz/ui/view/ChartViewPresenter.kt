package com.funglejunk.stockz.ui.view

import com.funglejunk.stockz.data.DrawableHistoricData
import com.funglejunk.stockz.round
import com.funglejunk.stockz.toFboerseString
import timber.log.Timber
import java.time.LocalDate

class ChartViewPresenter {

    fun calculateHorizontalLines(
        data: DrawableHistoricData,
        viewHeight: Int
    ): List<Pair<String, Float>> {
        val minValue = data.data.minBy { it.value }?.value
        val maxValue = data.data.maxBy { it.value }?.value
        return when (maxValue == null || minValue == null) {
            true -> emptyList()
            false -> {
                val numberOfLines = 10
                val valueSteps = (maxValue - minValue) / numberOfLines
                val verticalDistance = viewHeight.toFloat() / numberOfLines
                (0..numberOfLines).map {
                    val y = (numberOfLines - it) * verticalDistance
                    val label = (it * valueSteps + minValue).round().toString()
                    label to y
                }

            }
        }
    }

    fun calculateLabels(data: DrawableHistoricData, viewWidth: Int): List<Pair<LocalDate, Float>> {
        val distanceYValues = viewWidth.toFloat() / data.size
        return data.mapIndexed { index, (date, _) -> date to index * distanceYValues }
    }

    fun calculateChartValues(data: DrawableHistoricData, viewHeight: Int): List<Float> {
        val maxValueY = data.maxBy { it.value }?.value
        val minValueY = data.minBy { it.value }?.value
        return if (maxValueY != null && minValueY != null) {
            val verticalSpan = maxValueY - minValueY
            val factorY = (viewHeight / verticalSpan)
            data.map { (_, value) ->
                (value - minValueY) * factorY
            }
        } else {
            emptyList()
        }
    }

    // Brings list with only first values from a year
    fun getYearMarkers(data: List<Pair<LocalDate, Float>>): List<Pair<String, Float>> {
        var currentYear = data.first().first.year
        return data.filter { (date, _) ->
            val dataYear = date.year
            val isNewYear = dataYear > currentYear
            currentYear = dataYear
            isNewYear
        }.map { (date, value) ->
            date.toFboerseString() to value
        }
    }

    fun getMonthMarkers(data: List<Pair<LocalDate, Float>>): List<Pair<String, Float>> {
        var currentMonth = data.first().first.month
        return data.filter { (date, _) ->
            val dateMonth = date.month
            val isNewMonth = dateMonth > currentMonth
            currentMonth = dateMonth
            isNewMonth
        }.map { (date, value) ->
            date.toFboerseString() to value
        }
    }
}
