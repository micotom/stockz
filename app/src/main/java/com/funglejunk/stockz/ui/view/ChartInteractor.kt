package com.funglejunk.stockz.ui.view

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Path
import arrow.syntax.function.partially1
import com.funglejunk.stockz.data.DrawableHistoricData
import com.funglejunk.stockz.round
import com.funglejunk.stockz.toMonthDayString
import com.funglejunk.stockz.toYearString

typealias XyValue = Pair<Float, Float>
typealias LineCoordinates = Pair<XyValue, XyValue>
typealias LabelWithLineCoordinates = Pair<String, LineCoordinates>

typealias AnimatorInitFunc = (List<Float>, Float) -> () -> ValueAnimator
typealias MonthMarkersDrawFunc = (List<LabelWithLineCoordinates>) -> (Canvas) -> Unit
typealias HorizontalBarsDrawFunc = (List<LabelWithLineCoordinates>) -> (Canvas) -> Unit
typealias YearMarkersDrawFunc = (List<LabelWithLineCoordinates>) -> (Canvas) -> Unit
typealias PathResetFunc = (Float) -> (Path) -> Unit

typealias DrawFunc = (Canvas) -> Unit

class ChartInteractor {

    data class DrawFuncRegister(
        val pathResetFunc: (Path) -> Unit,
        val animatorInitFunc: () -> ValueAnimator,
        val monthMarkersDrawFunc: DrawFunc,
        val horizontalBarsDrawFunc: DrawFunc,
        val yearMarkersDrawFunc: DrawFunc
    )

    fun prepareDrawing(
        data: DrawableHistoricData,
        viewWidth: Float,
        viewHeight: Float,
        chartView: ChartViewInterface
    ): DrawFuncRegister {

        val xSpreadFactor = viewWidth / data.size
        val chartYValues = calculateChartPoints(data, viewHeight)
        val firstY = when (chartYValues.isNotEmpty()) {
            true -> chartYValues[0]
            false -> 0f
        }

        val verticalMonthLines =
            calculateVerticalMonthLines(data, viewHeight, xSpreadFactor)
        val horizontalValueLines = calculateHorizontalValueLines(data, viewWidth, viewHeight)
        val verticalYearLines = calculateVerticalYearLines(data, viewHeight, xSpreadFactor)

        return DrawFuncRegister(
            pathResetFunc = chartView.pathResetFunc.partially1(firstY).invoke(),
            animatorInitFunc = chartView.animatorInitFunc.partially1(chartYValues).partially1(
                xSpreadFactor
            ).invoke(),
            monthMarkersDrawFunc = chartView.monthMarkersDrawFunc.partially1(verticalMonthLines).invoke(),
            yearMarkersDrawFunc = chartView.yearMarkerDrawFunc.partially1(verticalYearLines).invoke(),
            horizontalBarsDrawFunc = chartView.horizontalBarsDrawFunc.partially1(
                horizontalValueLines
            ).invoke()
        )
    }

    private fun calculateChartPoints(
        data: DrawableHistoricData,
        viewHeight: Float
    ): List<Float> {
        return when (data.isNotEmpty()) {
            true -> {
                val maxValueY = data.maxBy { it.value }!!.value
                val minValueY = data.minBy { it.value }!!.value
                val verticalSpan = maxValueY - minValueY
                val factorY = (viewHeight / verticalSpan)
                data.map { (_, value) ->
                    (value - minValueY) * factorY
                }
            }
            false -> emptyList()
        }
    }

    private fun calculateVerticalMonthLines(
        data: DrawableHistoricData,
        viewHeight: Float,
        xSpreadFactor: Float
    ): List<LabelWithLineCoordinates> {
        return when (data.isNotEmpty()) {
            true -> {
                var currentMonth =
                    data.first().date.month // TODO folding and flattening would be prettier
                data.mapIndexed { index, chartValue ->
                    index to chartValue
                }.filter { (_, chartValue) ->
                    val (date, _) = chartValue
                    val dateMonth = date.month
                    val isNewMonth = dateMonth > currentMonth
                    currentMonth = dateMonth
                    isNewMonth
                }.map { (index, chartValue) ->
                    val (date, _) = chartValue
                    val xCoordinate = index * xSpreadFactor
                    val startPoint = xCoordinate to 0f
                    val endPoint = xCoordinate to viewHeight
                    date.toMonthDayString() to (startPoint to endPoint)
                }.filterIndexed() { index, _ ->
                    index % 3 == 0 // TODO only works in portrait, landscape needs more
                }
            }
            false -> emptyList()
        }
    }

    private fun calculateHorizontalValueLines(
        data: DrawableHistoricData,
        viewWidth: Float,
        viewHeight: Float
    ): List<LabelWithLineCoordinates> {
        val minValue = data.data.minBy { it.value }?.value
        val maxValue = data.data.maxBy { it.value }?.value
        return when (maxValue == null || minValue == null) {
            true -> emptyList()
            false -> {
                val numberOfLines = 10 // TODO fixing this creates clutter on orientation change
                val valueSteps = (maxValue - minValue) / numberOfLines
                val verticalDistance = viewHeight / numberOfLines
                (0..numberOfLines).map {
                    val y = (numberOfLines - it) * verticalDistance
                    val label = (it * valueSteps + minValue).round().toString()
                    val startPoint = 0f to y
                    val endPoint = viewWidth to y
                    label to (startPoint to endPoint)
                }
            }
        }
    }

    private fun calculateVerticalYearLines(
        data: DrawableHistoricData,
        viewHeight: Float,
        xSpreadFactor: Float
    ): List<LabelWithLineCoordinates> {
        return when (data.isNotEmpty()) {
            true -> {
                var currentYear = data.first().date.year
                data.mapIndexed { index, chartValue ->
                    index to chartValue
                }.filter { (_, chartValue) ->
                    val (date, _) = chartValue
                    val dataYear = date.year
                    val isNewYear = dataYear > currentYear
                    currentYear = dataYear
                    isNewYear
                }.map { (index, chartValue) ->
                    val (date, _) = chartValue
                    val xCoordinate = index * xSpreadFactor
                    val startPoint = xCoordinate to 0f
                    val endPoint = xCoordinate to viewHeight
                    date.toYearString() to (startPoint to endPoint)
                }
            }
            false -> emptyList()
        }
    }

}