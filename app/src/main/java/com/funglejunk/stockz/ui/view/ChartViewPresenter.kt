package com.funglejunk.stockz.ui.view

import com.funglejunk.stockz.data.DrawableHistoricData
import com.funglejunk.stockz.toFboerseString
import java.time.LocalDate

class ChartViewPresenter {

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
