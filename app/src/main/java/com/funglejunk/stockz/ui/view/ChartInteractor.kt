package com.funglejunk.stockz.ui.view

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Path
import arrow.syntax.function.partially1
import com.funglejunk.stockz.data.ChartValue
import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import com.funglejunk.stockz.model.Period
import com.funglejunk.stockz.model.averageTrueRange
import com.funglejunk.stockz.model.bollingerBands
import com.funglejunk.stockz.model.simpleMovingAverage
import com.funglejunk.stockz.round
import com.funglejunk.stockz.toLocalDate
import com.funglejunk.stockz.toMonthDayString
import com.funglejunk.stockz.toYearString
import java.lang.IllegalStateException

typealias XyValue = Pair<Float, Float>
typealias LineCoordinates = Pair<XyValue, XyValue>
typealias LabelWithLineCoordinates = Pair<String, LineCoordinates>

typealias AnimatorInitFunc = (List<Float>, Float) -> () -> ValueAnimator
typealias MonthMarkersDrawFunc = (List<LabelWithLineCoordinates>) -> (Canvas) -> Unit
typealias HorizontalBarsDrawFunc = (List<LabelWithLineCoordinates>) -> (Canvas) -> Unit
typealias YearMarkersDrawFunc = (List<LabelWithLineCoordinates>) -> (Canvas) -> Unit
typealias PathResetFunc = (Float) -> (Path) -> Unit
typealias SimpleXyDrawFunc = (List<XyValue>) -> (Canvas) -> Unit
typealias DoubleXyDrawFunc = (Pair<List<XyValue>, List<XyValue>>) -> (Canvas) -> Unit

typealias DrawFunc = (Canvas) -> Unit

typealias XBoundaries = Pair<Float, Float>
typealias Sector = Pair<XBoundaries, List<FBoerseHistoryData.Data>>

class ChartInteractor {

    data class DrawFuncRegister(
        val pathResetFunc: (Path) -> Unit,
        val animatorInitFunc: () -> ValueAnimator,
        val monthMarkersDrawFunc: DrawFunc,
        val horizontalBarsDrawFunc: DrawFunc,
        val yearMarkersDrawFunc: DrawFunc,
        val simpleAvDrawFunc: DrawFunc,
        val bollingerDrawFunc: DrawFunc,
        val atrDrawFunc: DrawFunc
    )

    private lateinit var dataCache: () -> FBoerseHistoryData
    private lateinit var sectorsCache: () -> Pair<List<Sector>, List<Sector>>

    fun showAllSectors(
        viewWidth: Float, viewHeight: Float, isInPortraitMode: Boolean,
        view: ChartView
    ): DrawFuncRegister {
        return with(dataCache.invoke()) {
            prepareDrawingInternal(this, viewWidth, viewHeight, isInPortraitMode, view)
        }
    }

    fun showSector(
        tapX: Float, viewWidth: Float, viewHeight: Float, isInPortraitMode: Boolean,
        view: ChartView
    ): DrawFuncRegister =
        with(sectorsCache.invoke()) {
            val (portraitSectors, landscapeSectors) = this
            with(dataCache.invoke()) {
                showSectorsInternal(
                    this,
                    portraitSectors,
                    landscapeSectors,
                    tapX,
                    viewWidth,
                    viewHeight,
                    isInPortraitMode,
                    view
                )
            }
        }

    private val showSectorsInternal:
                (FBoerseHistoryData, List<Sector>, List<Sector>, Float, Float, Float, Boolean, ChartView) -> DrawFuncRegister =
        { data, portraitSectors, landscapeSectors, tapX, viewWidth, viewHeight, isInPortraitMode, view ->
            val targetSectors = when (isInPortraitMode) {
                true -> portraitSectors
                false -> landscapeSectors
            }
            val tappedSector = targetSectors.find {
                val (xBounds, _) = it
                tapX >= xBounds.first && tapX <= xBounds.second
            }!!
            prepareDrawingInternal(
                data.copy(content = tappedSector.second),
                viewWidth,
                viewHeight,
                isInPortraitMode,
                view
            )
        }

    private val appliedSectors: (List<Sector>, List<Sector>) -> () -> Pair<List<Sector>, List<Sector>> =
        { portraitSectors, landscapeSector ->
            {
                portraitSectors to landscapeSector
            }
        }

    private val appliedData: (FBoerseHistoryData) -> () -> FBoerseHistoryData = { data ->
        {
            data
        }
    }

    fun prepareDrawing(
        data: FBoerseHistoryData, // TODO hand over content object directly
        viewWidth: Float,
        viewHeight: Float,
        isInPortraitMode: Boolean,
        chartView: ChartViewInterface
    ): DrawFuncRegister {
        initDataAndSectorsCache(viewWidth, data, viewHeight)
        return prepareDrawingInternal(data, viewWidth, viewHeight, isInPortraitMode, chartView)
    }

    private fun initDataAndSectorsCache(
        viewWidth: Float,
        data: FBoerseHistoryData,
        viewHeight: Float
    ) {
        val xSpreadFactor = viewWidth / data.content.size
        val portraitBounds = calculateVerticalMonthLines(
            data.content, viewHeight, xSpreadFactor, true
        )
        val landscapeBounds = calculateVerticalMonthLines(
            data.content, viewHeight, xSpreadFactor, false
        )

        val sectorsPortrait = portraitBounds.mapIndexed { index, (_, rightBoundXy) ->
            val leftBound = when (index) {
                0 -> 0f
                else -> portraitBounds[index - 1].second.first.first
            }
            val rightBound = when (index) {
                portraitBounds.size - 1 -> viewWidth
                else -> rightBoundXy.first.first
            }
            Pair(leftBound, rightBound) to mutableListOf<FBoerseHistoryData.Data>()
        }

        val sectorsLandscape = landscapeBounds.mapIndexed { index, (_, rightBoundXy) ->
            val leftBound = when (index) {
                0 -> 0f
                else -> landscapeBounds[index - 1].second.first.first
            }
            val rightBound = when (index) {
                landscapeBounds.size - 1 -> viewWidth
                else -> rightBoundXy.first.first
            }
            Pair(leftBound, rightBound) to mutableListOf<FBoerseHistoryData.Data>()
        }

        // TODO prettify by not using forEach
        data.content.forEachIndexed { index, dataPoint ->
            val dataPointX = index * xSpreadFactor
            sectorsPortrait.forEachIndexed { sectorIndex, (minMaxX, list) ->
                val (minX, maxX) = minMaxX
                if (dataPointX >= minX && dataPointX < maxX) {
                    list.add(dataPoint)
                }
            }
        }

        // TODO prettify by not using forEach
        data.content.forEachIndexed { index, dataPoint ->
            val dataPointX = index * xSpreadFactor
            sectorsLandscape.forEachIndexed { sectorIndex, (minMaxX, list) ->
                val (minX, maxX) = minMaxX
                if (dataPointX >= minX && dataPointX < maxX) {
                    list.add(dataPoint)
                }
            }
        }

        dataCache = appliedData.invoke(data)
        sectorsCache = appliedSectors.invoke(sectorsPortrait, sectorsLandscape)
    }

    private fun prepareDrawingInternal(
        data: FBoerseHistoryData,
        viewWidth: Float,
        viewHeight: Float,
        isInPortraitMode: Boolean,
        chartView: ChartViewInterface
    ): DrawFuncRegister {
        val xSpreadFactor = viewWidth / data.content.size
        val chartYValues = calculateChartPoints(data.content, viewHeight)
        val firstY = when (chartYValues.isNotEmpty()) {
            true -> chartYValues[0]
            false -> 0f
        }

        val verticalMonthLines =
            calculateVerticalMonthLines(data.content, viewHeight, xSpreadFactor, isInPortraitMode)
        val horizontalValueLines =
            calculateHorizontalValueLines(data.content, viewWidth, viewHeight, isInPortraitMode)
        val verticalYearLines = calculateVerticalYearLines(data.content, viewHeight, xSpreadFactor)

        val dataAsChartValues = data.content.map {
            ChartValue(it.date.toLocalDate(), it.close.toFloat())
        }

        val smaPoints = calculateAlgorithmPoints(
            simpleMovingAverage(dataAsChartValues, Period.DAYS_30, 2),
            data.content,
            xSpreadFactor,
            viewHeight
        )
        val atrPoints = calculateChartUnboundAlgorithmPoints(
            averageTrueRange(data.content), data.content, xSpreadFactor, viewHeight, 2f
        )
        val bollingerPointsRaw = bollingerBands(
            dataAsChartValues,
            Period.DAYS_7, 2
        )
        val bollingerPoints =
            calculateAlgorithmPoints(
                bollingerPointsRaw.first,
                data.content,
                xSpreadFactor,
                viewHeight
            ) to
                    calculateAlgorithmPoints(
                        bollingerPointsRaw.second,
                        data.content,
                        xSpreadFactor,
                        viewHeight
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
        originalData: List<FBoerseHistoryData.Data>, // TODO move vertical span to precalculation
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

    private fun calculateAlgorithmPoints(
        data: List<ChartValue>,
        originalData: List<FBoerseHistoryData.Data>, // TODO move vertical span to precalculation
        xSpreadFactor: Float,
        viewHeight: Float
    ): List<XyValue> = when (data.isNotEmpty()) {
        true -> {
            val maxValueY = originalData.maxBy { it.close }!!.close.toFloat()
            val minValueY = originalData.minBy { it.close }!!.close.toFloat()
            val verticalSpan = maxValueY - minValueY
            val factorY = (viewHeight / verticalSpan)
            val xValueRegister = originalData.mapIndexed { index, originalValue ->
                val x = index * xSpreadFactor
                originalValue.date to x
            }
            data.map { value ->
                val x = xValueRegister.find { it.first.toLocalDate() == value.date }!!.second
                val y = (value.value - minValueY) * factorY
                x to y
            }
        }
        false -> emptyList()
    }

    private fun calculateChartPoints(
        data: List<FBoerseHistoryData.Data>,
        viewHeight: Float
    ): List<Float> {
        return when (data.isNotEmpty()) {
            true -> {
                val maxValueY = data.maxBy { it.close }!!.close
                val minValueY = data.minBy { it.close }!!.close
                val verticalSpan = maxValueY - minValueY
                val factorY = (viewHeight / verticalSpan)
                data.map {
                    ((it.close - minValueY) * factorY).toFloat()
                }
            }
            false -> emptyList()
        }
    }

    private fun calculateVerticalMonthLines(
        data: List<FBoerseHistoryData.Data>,
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

    private fun calculateHorizontalValueLines(
        data: List<FBoerseHistoryData.Data>,
        viewWidth: Float,
        viewHeight: Float,
        isInPortraitMode: Boolean
    ): List<LabelWithLineCoordinates> {
        val minValue = data.minBy { it.close }?.close
        val maxValue = data.maxBy { it.close }?.close
        return when (maxValue == null || minValue == null) {
            true -> emptyList()
            false -> {
                val numberOfLines = when (isInPortraitMode) {
                    true -> ChartView.HORIZONTAL_LINE_COUNT_PORTRAIT
                    false -> ChartView.HORIZONTAL_LINE_COUNT_LANDSCAPE
                }
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
        data: List<FBoerseHistoryData.Data>,
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