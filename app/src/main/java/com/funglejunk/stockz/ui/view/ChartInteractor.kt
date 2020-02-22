package com.funglejunk.stockz.ui.view

import android.graphics.Canvas
import android.graphics.Path
import androidx.annotation.VisibleForTesting
import arrow.syntax.function.partially1
import com.funglejunk.stockz.data.ChartValue
import com.funglejunk.stockz.data.RepoHistoryData
import com.funglejunk.stockz.model.Period
import com.funglejunk.stockz.model.averageTrueRange
import com.funglejunk.stockz.model.bollingerBands
import com.funglejunk.stockz.model.simpleMovingAverage
import com.funglejunk.stockz.round
import com.funglejunk.stockz.toLocalDate
import com.funglejunk.stockz.toMonthDayString
import com.funglejunk.stockz.toYearString

typealias XyValue = Pair<Float, Float>
typealias LineCoordinates = Pair<XyValue, XyValue>
typealias LabelWithLineCoordinates = Pair<String, LineCoordinates>

typealias AnimatorInitFunc = (List<Float>, Float) -> () -> Runnable
typealias MonthMarkersDrawFunc = (List<LabelWithLineCoordinates>) -> (Canvas) -> Unit
typealias HorizontalBarsDrawFunc = (List<LabelWithLineCoordinates>) -> (Canvas) -> Unit
typealias YearMarkersDrawFunc = (List<LabelWithLineCoordinates>) -> (Canvas) -> Unit
typealias PathResetFunc = (Float) -> (Path) -> Unit
typealias SimpleXyDrawFunc = (List<XyValue>) -> (Canvas) -> Unit
typealias DoubleXyDrawFunc = (Pair<List<XyValue>, List<XyValue>>) -> (Canvas) -> Unit

typealias DrawFunc = (Canvas) -> Unit

typealias XBoundaries = Pair<Float, Float>
typealias Sector = Pair<XBoundaries, List<RepoHistoryData.Data>>

class ChartInteractor {

    data class DrawFuncRegister(
        val pathResetFunc: (Path) -> Unit,
        val animatorInitFunc: () -> Runnable,
        val monthMarkersDrawFunc: DrawFunc,
        val horizontalBarsDrawFunc: DrawFunc,
        val yearMarkersDrawFunc: DrawFunc,
        val simpleAvDrawFunc: DrawFunc,
        val bollingerDrawFunc: DrawFunc,
        val atrDrawFunc: DrawFunc
    )

    fun prepareDrawing(
        data: RepoHistoryData, // TODO hand over content object directly
        viewWidth: Float,
        viewHeight: Float,
        isInPortraitMode: Boolean,
        yPadding: Float,
        chartView: ChartViewInterface
    ): DrawFuncRegister {

        val xSpreadFactor = viewWidth / data.content.size
        val chartYValues = calculateChartPoints(data.content, viewHeight, yPadding)
        val firstY = when (chartYValues.isNotEmpty()) {
            true -> chartYValues[0]
            false -> 0f
        }

        val verticalMonthLines =
            calculateVerticalMonthLines(data.content, viewHeight, xSpreadFactor, isInPortraitMode)
        val horizontalValueLines =
            calculateHorizontalValueLines(data.content, viewWidth, viewHeight, yPadding, isInPortraitMode)
        val verticalYearLines = calculateVerticalYearLines(data.content, viewHeight, xSpreadFactor)

        val dataAsChartValues = data.content.map {
            ChartValue(it.date.toLocalDate(), it.close.toFloat())
        }

        val smaPoints = calculateAlgPoints(
            simpleMovingAverage(dataAsChartValues, Period.DAYS_30, 2),
            data.content,
            xSpreadFactor,
            viewHeight,
            yPadding
        )
        val atrPoints = calculateChartUnboundAlgorithmPoints(
            averageTrueRange(data.content), data.content, xSpreadFactor, viewHeight, 2f
        )
        val bollingerPointsRaw = bollingerBands(
            dataAsChartValues,
            Period.DAYS_7, 2
        )
        val bollingerPoints =
            calculateAlgPoints(
                bollingerPointsRaw.first,
                data.content,
                xSpreadFactor,
                viewHeight,
                yPadding
            ) to calculateAlgPoints(
                bollingerPointsRaw.second,
                data.content,
                xSpreadFactor,
                viewHeight,
                yPadding
            )

        return DrawFuncRegister(
            pathResetFunc = chartView.pathResetFunc.partially1(firstY).invoke(),
            animatorInitFunc = chartView.animatorInitFunc.partially1(chartYValues).partially1(
                xSpreadFactor
            ).invoke(),
            monthMarkersDrawFunc = chartView.monthMarkersDrawFunc.partially1(verticalMonthLines).invoke(),
            yearMarkersDrawFunc = chartView.yearMarkerDrawFunc.partially1(verticalYearLines).invoke(),
            horizontalBarsDrawFunc = chartView.horizontalBarsDrawFunc.partially1(
                horizontalValueLines
            ).invoke(),
            simpleAvDrawFunc = chartView.movingAvDrawFunc.partially1(smaPoints).invoke(),
            bollingerDrawFunc = chartView.bollingerDrawFunc.partially1(bollingerPoints).invoke(),
            atrDrawFunc = chartView.atrDrawFunc.partially1(atrPoints).invoke()
        )
    }

    private fun calculateChartUnboundAlgorithmPoints(
        data: List<ChartValue>,
        originalData: List<RepoHistoryData.Data>, // TODO move vertical span to precalculation
        xSpreadFactor: Float,
        viewHeight: Float,
        scaleFactor: Float
    ): List<XyValue> = when (data.isNotEmpty()) {
        true -> {
            val xValueRegister = originalData.mapIndexed { index, originalValue ->
                val x = index * xSpreadFactor
                originalValue.date to x
            }
            val minValueY = data.minBy { it.value }!!.value
            data.map { value ->
                val x = xValueRegister.find { it.first.toLocalDate() == value.date }!!.second
                val y = (value.value - minValueY) * viewHeight * scaleFactor
                x to y
            }
        }
        false -> emptyList()
    }

    private fun calculateAlgPoints(
        data: List<ChartValue>,
        originalData: List<RepoHistoryData.Data>, // TODO move vertical span to precalculation
        xSpreadFactor: Float,
        viewHeight: Float,
        yPadding: Float
    ): List<XyValue> = when (data.isNotEmpty()) {
        true -> {
            val maxValueY = originalData.maxBy { it.close }!!.close
            val minValueY = originalData.minBy { it.close }!!.close
            val verticalSpan = when (val diff = maxValueY - minValueY) {
                0.0 -> 1.0
                else -> diff
            }
            val factorY = (viewHeight - 2 * yPadding) / verticalSpan
            val xValueRegister = originalData.mapIndexed { index, originalValue ->
                val x = index * xSpreadFactor
                originalValue.date to x
            }
            data.map { value ->
                val x = xValueRegister.find { it.first.toLocalDate() == value.date }!!.second
                val y = ((value.value - minValueY) * factorY + yPadding).toFloat()
                x to y
            }
        }
        false -> emptyList()
    }

    @VisibleForTesting
    fun calculateChartPoints(
        data: List<RepoHistoryData.Data>,
        viewHeight: Float,
        yPadding: Float
    ): List<Float> = when (data.isNotEmpty()) {
        true -> {
            val maxValueY = data.maxBy { it.close }!!.close
            val minValueY = data.minBy { it.close }!!.close
            val verticalSpan = when (val diff = maxValueY - minValueY) {
                0.0 -> 1.0
                else -> diff
            }
            val factorY = ((viewHeight - 2 * yPadding) / verticalSpan)
            data.map {
                ((it.close - minValueY) * factorY + yPadding).toFloat()
            }
        }
        false -> emptyList()
    }

    @VisibleForTesting
    fun calculateVerticalMonthLines(
        data: List<RepoHistoryData.Data>,
        viewHeight: Float,
        xSpreadFactor: Float,
        isInPortraitMode: Boolean
    ): List<LabelWithLineCoordinates> {
        return when (data.isNotEmpty()) {
            true -> {
                val drawFrequency = when (isInPortraitMode) {
                    true -> ChartView.MONTH_LINES_MODULO_PORTRAIT
                    false -> ChartView.MONTH_LINES_MODULO_LANDSCAPE
                }
                var currentMonth =
                    data.first().date.toLocalDate()
                        .month // TODO folding and flattening would be prettier
                data.mapIndexed { index, chartValue ->
                    index to chartValue
                }.filter { (_, chartValue) ->
                    val (date, _) = chartValue
                    val dateMonth = date.toLocalDate().month
                    val isNewMonth = dateMonth > currentMonth
                    currentMonth = dateMonth
                    isNewMonth
                }.map { (index, chartValue) ->
                    val (date, _) = chartValue
                    val xCoordinate = index * xSpreadFactor
                    val startPoint = xCoordinate to 0f
                    val endPoint = xCoordinate to viewHeight
                    date.toLocalDate().toMonthDayString() to (startPoint to endPoint)
                }.filterIndexed { index, _ ->
                    index % drawFrequency == 0
                }
            }
            false -> emptyList()
        }
    }

    @VisibleForTesting
    fun calculateHorizontalValueLines(
        data: List<RepoHistoryData.Data>,
        viewWidth: Float,
        viewHeight: Float,
        yPadding: Float,
        isInPortraitMode: Boolean
    ): List<LabelWithLineCoordinates> = when (data.isNotEmpty()) {
        true -> {
            val minValue = data.minBy { it.close }!!.close
            val maxValue = data.maxBy { it.close }!!.close
            val numberOfLines = when (isInPortraitMode) {
                true -> ChartView.HORIZONTAL_LINE_COUNT_PORTRAIT
                false -> ChartView.HORIZONTAL_LINE_COUNT_LANDSCAPE
            }
            val valueSteps = (maxValue - minValue) / numberOfLines
            val verticalDistance = (viewHeight - 2 * yPadding) / numberOfLines
            (0..numberOfLines).map {
                val y = (numberOfLines - it) * verticalDistance + yPadding
                val label = (it * valueSteps + minValue).round().toString()
                val startPoint = 0f to y
                val endPoint = viewWidth to y
                label to (startPoint to endPoint)
            }
        }
        false -> emptyList()
    }

    @VisibleForTesting
    fun calculateVerticalYearLines(
        data: List<RepoHistoryData.Data>,
        viewHeight: Float,
        xSpreadFactor: Float
    ): List<LabelWithLineCoordinates> {
        return when (data.isNotEmpty()) {
            true -> {
                var currentYear = data.first().date.toLocalDate().year
                data.mapIndexed { index, chartValue ->
                    index to chartValue
                }.filter { (_, chartValue) ->
                    val (date, _) = chartValue
                    val dataYear = date.toLocalDate().year
                    val isNewYear = dataYear > currentYear
                    currentYear = dataYear
                    isNewYear
                }.map { (index, chartValue) ->
                    val (date, _) = chartValue
                    val xCoordinate = index * xSpreadFactor
                    val startPoint = xCoordinate to 0f
                    val endPoint = xCoordinate to viewHeight
                    date.toLocalDate().toYearString() to (startPoint to endPoint)
                }
            }
            false -> emptyList()
        }
    }
}