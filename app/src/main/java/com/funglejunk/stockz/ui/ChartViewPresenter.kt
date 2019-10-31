package com.funglejunk.stockz.ui

import com.funglejunk.stockz.data.DrawableHistoricData
import java.text.SimpleDateFormat
import java.util.*

class ChartViewPresenter {

    private companion object {
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd")
    }

    fun calculateLabels(data: DrawableHistoricData, viewWidth: Int): List<Pair<Date, Float>> {
        val distanceYValues = viewWidth.toFloat() / data.size
        return data.mapIndexed { index, (date, _) -> date to index * distanceYValues }
    }

    fun calculateChartValues(data: DrawableHistoricData, viewHeight: Int): List<Float> {
        val maxValueY = data.maxBy { it.value }?.value
        val minValueY = data.minBy { it.value }?.value
        return if (maxValueY != null && minValueY != null) {
            val verticalSpan = maxValueY - minValueY
            val factorY = (viewHeight / verticalSpan)
            data.mapIndexed { index, (_, value) ->
                (value - minValueY) * factorY
            }
        } else {
            emptyList()
        }
    }

    fun getYearMarkers(data: List<Pair<Date, Float>>): List<Pair<String, Float>> {
        val calendarLabels = data.map { (date, x) ->
            Calendar.getInstance().apply { time = date } to x
        }
        var currentYear = calendarLabels[0].first.get(Calendar.YEAR)
        return calendarLabels.filter { (c, _) ->
            val cYear = c.get(Calendar.YEAR)
            val isNewYear = cYear > currentYear
            currentYear = cYear
            isNewYear
        }.map { (calendar, x) ->
            dateFormatter.format(calendar.time) to x
        }
    }

    fun getMonthMarkers(data: List<Pair<Date, Float>>): List<Pair<String, Float>> {
        val calendarLabels = data.map { (date, x) ->
            Calendar.getInstance().apply { time = date } to x
        }
        var currentMonth = calendarLabels[0].first.get(Calendar.MONTH)
        return calendarLabels.filter { (c, _) ->
            val cMonth = c.get(Calendar.MONTH)
            val isNewMonth = cMonth > currentMonth
            currentMonth = cMonth
            isNewMonth
        }.map { (calendar, x) ->
            dateFormatter.format(calendar.time) to x
        }
    }

}