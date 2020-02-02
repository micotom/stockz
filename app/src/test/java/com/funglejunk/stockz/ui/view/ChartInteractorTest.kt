package com.funglejunk.stockz.ui.view

import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import com.funglejunk.stockz.round
import com.funglejunk.stockz.toYearMonthDayString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

class ChartInteractorTest {

    private fun createData(content: List<FBoerseHistoryData.Data>): FBoerseHistoryData =
        FBoerseHistoryData(
            isin = "Foo",
            content = content,
            totalCount = 0,
            tradedInPercent = false
        )

    private fun createDataPoint(date: LocalDate, close: Double): FBoerseHistoryData.Data =
        FBoerseHistoryData.Data(
            date = date.toYearMonthDayString(),
            openValue = 10.0,
            close = close,
            high = 25.0,
            low = 5.0,
            turnoverPieces = 1.0,
            turnoverEuro = 1.0
        )


    @Test
    fun `y points on empty data`() {
        val interactor = ChartInteractor()
        val data = createData(emptyList())
        val yPoints = interactor.calculateChartPoints(data.content, 100f, 0f)
        assertTrue(yPoints.isEmpty())
    }

    @Test
    fun `y point single correctly stretched`() {
        val viewHeight = 100f
        val close = 20.0
        val interactor = ChartInteractor()
        val dataPoint = createDataPoint(LocalDate.of(2020, 1, 1), close)
        val data = createData(listOf(dataPoint))
        val yPoints = interactor.calculateChartPoints(data.content, viewHeight, 0f)
        assertTrue(yPoints.size == 1)
        assertEquals(0f, yPoints[0])
    }

    @Test
    fun `y point set correctly stretched`() {
        val viewHeight = 100f
        val closeA = 20.0
        val closeB = 10.0
        val max = max(closeA, closeB)
        val min = min(closeA, closeB)
        val interactor = ChartInteractor()
        val dataPointA = createDataPoint(LocalDate.of(2020, 1, 1), closeA)
        val dataPointB = createDataPoint(LocalDate.of(2020, 1, 2), closeB)
        val data = createData(listOf(dataPointA, dataPointB))
        val yPoints = interactor.calculateChartPoints(data.content, viewHeight, 0f)
        assertTrue(yPoints.size == 2)
        assertEquals(((closeA - min) * (viewHeight / (max - min))).toFloat(), yPoints[0])
        assertEquals(((closeB - min) * (viewHeight / (max - min))).toFloat(), yPoints[1])
    }

    @Test
    fun `horizontal lines on empty data`() {
        val interactor = ChartInteractor()
        val data = createData(emptyList())
        val lines = interactor.calculateHorizontalValueLines(data.content, 100f, 100f, 0f, true)
        assertTrue(lines.isEmpty())
    }

    @Test
    fun `horizontal lines of single data portrait`() {
        val interactor = ChartInteractor()
        val viewHeight = 100.0f
        val viewWidth = 100.0f
        val close = 10.0
        val dataPoint = createDataPoint(LocalDate.of(2020, 1, 1), close)
        val data = createData(listOf(dataPoint))
        val lines =
            interactor.calculateHorizontalValueLines(data.content, viewWidth, viewHeight, 0f, true)
        assertTrue(lines.size == ChartView.HORIZONTAL_LINE_COUNT_PORTRAIT + 1)
        val yStep = viewHeight / (lines.size - 1)
        val valueStep = 0f
        lines.forEachIndexed { index, (label, points) ->
            assertEquals(points.first.first, 0f)
            assertEquals(points.second.first, viewWidth)
            assertEquals(points.first.second, points.second.second)
            val y = viewHeight - index * yStep
            val expectedLabel = (index * valueStep + close).round().toString()
            assertEquals(y, points.first.second, 0.001f)
            assertEquals(expectedLabel, label)
        }
    }

    @Test
    fun `horizontal lines of double data portrait`() {
        val interactor = ChartInteractor()
        val viewHeight = 100.0f
        val viewWidth = 100.0f
        val closeA = 10.0
        val closeB = 25.0
        val dataPointA = createDataPoint(LocalDate.of(2020, 1, 1), closeA)
        val dataPointB = createDataPoint(LocalDate.of(2020, 1, 2), closeB)
        val dataPoints = listOf(dataPointA, dataPointB)
        val data = createData(dataPoints)
        val lines =
            interactor.calculateHorizontalValueLines(data.content, viewWidth, viewHeight, 0f, true)
        assertTrue(lines.size == ChartView.HORIZONTAL_LINE_COUNT_PORTRAIT + 1)
        val yStep = viewHeight / (lines.size - 1)
        val valueStep = (max(closeA, closeB) - min(closeA, closeB)) / (lines.size - 1)
        lines.forEachIndexed { index, (label, points) ->
            assertEquals(points.first.first, 0f)
            assertEquals(points.second.first, viewWidth)
            assertEquals(points.first.second, points.second.second)
            val y = viewHeight - index * yStep
            val expectedLabel = (index * valueStep + min(closeA, closeB)).round().toString()
            assertEquals(y, points.first.second, 0.001f)
            assertEquals(expectedLabel, label)
        }
    }

    @Test
    fun `horizontal lines of single data landscape`() {
        val interactor = ChartInteractor()
        val viewHeight = 100.0f
        val viewWidth = 100.0f
        val close = 10.0
        val dataPoint = createDataPoint(LocalDate.of(2020, 1, 1), close)
        val data = createData(listOf(dataPoint))
        val lines =
            interactor.calculateHorizontalValueLines(data.content, viewWidth, viewHeight, 0f, false)
        assertTrue(lines.size == ChartView.HORIZONTAL_LINE_COUNT_LANDSCAPE + 1)
        val yStep = viewHeight / (lines.size - 1)
        val valueStep = 0.0
        lines.forEachIndexed { index, (label, points) ->
            assertEquals(points.first.first, 0f)
            assertEquals(points.second.first, viewWidth)
            assertEquals(points.first.second, points.second.second)
            val y = viewHeight - index * yStep
            val expectedLabel = (index * valueStep + close).round().toString()
            assertEquals(y, points.first.second, 0.001f)
            assertEquals(expectedLabel, label)
        }
    }

    @Test
    fun `horizontal lines of double data landscape`() {
        val interactor = ChartInteractor()
        val viewHeight = 100.0f
        val viewWidth = 100.0f
        val closeA = 10.0
        val closeB = 25.0
        val dataPointA = createDataPoint(LocalDate.of(2020, 1, 1), closeA)
        val dataPointB = createDataPoint(LocalDate.of(2020, 1, 2), closeB)
        val dataPoints = listOf(dataPointA, dataPointB)
        val data = createData(dataPoints)
        val lines =
            interactor.calculateHorizontalValueLines(data.content, viewWidth, viewHeight, 0f, false)
        assertTrue(lines.size == ChartView.HORIZONTAL_LINE_COUNT_LANDSCAPE + 1)
        val yStep = viewHeight / (lines.size - 1)
        val valueStep = (max(closeA, closeB) - min(closeA, closeB)) / (lines.size - 1)
        lines.forEachIndexed { index, (label, points) ->
            assertEquals(points.first.first, 0f)
            assertEquals(points.second.first, viewWidth)
            assertEquals(points.first.second, points.second.second)
            val y = viewHeight - index * yStep
            val expectedLabel = index * valueStep + min(closeA, closeB)
            assertEquals(y, points.first.second, 0.001f)
            assertEquals((expectedLabel).toString(), label)
        }
    }

}