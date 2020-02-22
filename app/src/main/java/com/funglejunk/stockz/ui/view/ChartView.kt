package com.funglejunk.stockz.ui.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import arrow.core.extensions.list.applicative.map
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.RepoHistoryData
import timber.log.Timber

class ChartView : View, ChartViewInterface {

    companion object {
        const val MONTH_LINES_MODULO_PORTRAIT = 3
        const val MONTH_LINES_MODULO_LANDSCAPE = 2
        const val HORIZONTAL_LINE_COUNT_PORTRAIT = 10
        const val HORIZONTAL_LINE_COUNT_LANDSCAPE = 3
        private const val X_PADDING_START = 96f
        private const val X_PADDING_END = 24f
        private const val CIRCLE_RADIUS = 4f
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    @Suppress("unused")
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    private val chartPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.primaryColor)
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 1.5f
    }

    private val textLabelPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.primaryLightColor)
        style = Paint.Style.FILL
        textSize = resources.displayMetrics.density * 8
    }

    private val path = Path()
    private val textBound = Rect()

    private val interactor = ChartInteractor()
    private var funcRegister: ChartInteractor.DrawFuncRegister? = null

    private var drawLabels = false
    private var showSma = false
    private var showBollinger = false
    private var showAtr = false

    private val chartAnimHandler = Handler()

    fun draw(data: RepoHistoryData) {
        post {
            val isInPortraitMode =
                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            funcRegister = interactor.prepareDrawing(
                data,
                width.toFloat() - (X_PADDING_START + X_PADDING_END),
                height.toFloat(),
                isInPortraitMode,
                48f,
                this
            )
            drawNewData()
        }
    }

    private fun drawNewData() {
        funcRegister?.let { safeFuncRegister ->
            safeFuncRegister.pathResetFunc.invoke(path)
            val animationRunnable = safeFuncRegister.animatorInitFunc.invoke()
            chartAnimHandler.removeCallbacksAndMessages(null)
            chartAnimHandler.post(animationRunnable)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (drawLabels) {
            drawLabels = false
            funcRegister?.let { safeFuncRegister ->
                with(safeFuncRegister) {
                    horizontalBarsDrawFunc.invoke(canvas)
                    yearMarkersDrawFunc.invoke(canvas)
                    monthMarkersDrawFunc.invoke(canvas)
                    if (showSma) {
                        simpleAvDrawFunc.invoke(canvas)
                    }
                    if (showBollinger) {
                        bollingerDrawFunc.invoke(canvas)
                    }
                    if (showAtr) {
                        atrDrawFunc.invoke(canvas)
                    }
                }
            }
        }
        canvas.drawPath(path, chartPaint)
    }

    override val pathResetFunc: PathResetFunc = { startY ->
        { path ->
            path.reset()
            path.moveTo(X_PADDING_START, height - startY)
        }
    }

    override val animatorInitFunc: AnimatorInitFunc =
        { chartPoints, xValueSpreadBetweenPoints ->
            {
                object : Runnable {
                    var ticks = 0
                    private val delay = 1L
                    private val paintData = when (chartPoints.size > 365) {
                        true -> {
                            chartPoints.foldIndexed(mutableListOf()) { index, acc, new ->
                                if (index % 2 == 0) {
                                    acc.add(new)
                                }
                                acc
                            }
                        }
                        false -> chartPoints
                    }
                    private val paintXSpread = when (chartPoints.size != paintData.size) {
                        true -> xValueSpreadBetweenPoints * 2
                        false -> xValueSpreadBetweenPoints
                    }
                    override fun run() {
                        path.lineTo(
                            paintXSpread * ticks + X_PADDING_START,
                            height - paintData[ticks]
                        )
                        ++ticks
                        invalidateAndDrawLabels()
                        if (ticks != paintData.size) {
                            chartAnimHandler.postDelayed(this, delay)
                        }
                    }
                }
            }
        }

    private fun invalidateAndDrawLabels() {
        drawLabels = true
        invalidate()
    }

    override val bollingerDrawFunc: DoubleXyDrawFunc =
        { (upperPoints, lowerPoints) ->
            { canvas ->
                when (upperPoints.isNotEmpty() && lowerPoints.isNotEmpty()) {
                    true -> {
                        val chartPaint = Paint().apply {
                            color = ContextCompat.getColor(context, R.color.cardBorderColor)
                            isAntiAlias = true
                            style = Paint.Style.STROKE
                            strokeWidth = resources.displayMetrics.density
                        }
                        val shiftedUpperPoints = upperPoints.map { it.shiftXOffset() }
                        val shifterLowerPoints = lowerPoints.map { it.shiftXOffset() }
                        val path = Path().apply {
                            shiftedUpperPoints.forEachIndexed { index, (x, y) ->
                                if (index == 0) {
                                    moveTo(x, height - y)
                                } else {
                                    lineTo(x, height - y)
                                }
                            }
                            shifterLowerPoints.reversed().forEachIndexed { index, (x, y) ->
                                if (index == 0) {
                                    moveTo(x, height - y)
                                } else {
                                    lineTo(x, height - y)
                                }
                            }
                        }
                        canvas.drawPath(path, chartPaint)
                    }
                }
            }
        }

    override val movingAvDrawFunc: SimpleXyDrawFunc =
        { points ->
            { canvas ->
                when (points.isNotEmpty()) {
                    true -> {
                        val chartPaint = Paint().apply {
                            color = ContextCompat.getColor(context, R.color.cardBorderColor)
                            isAntiAlias = true
                            style = Paint.Style.STROKE
                            strokeWidth = resources.displayMetrics.density * 2
                        }
                        val shiftedPoints = points.map { it.shiftXOffset() }
                        val path = Path().apply {
                            shiftedPoints.forEachIndexed { index, point ->
                                if (index == 0) {
                                    moveTo(point.first, height - point.second)
                                } else {
                                    lineTo(point.first, height - point.second)
                                }
                            }
                        }
                        canvas.drawPath(path, chartPaint)
                    }
                }
            }
        }

    override val atrDrawFunc: SimpleXyDrawFunc = movingAvDrawFunc

    override val monthMarkersDrawFunc: MonthMarkersDrawFunc =
        { markers ->
            { canvas ->
                val verticalSecondaryLinePaint = Paint().apply {
                    color = ContextCompat.getColor(context, R.color.cardBorderColor)
                    style = Paint.Style.FILL
                }
                markers.shiftXOffset().forEach { (label, coordinates) ->
                    val startPoint = coordinates.first
                    val endPoint = coordinates.second
                    textLabelPaint.getTextBounds(label, 0, label.length, textBound)
                    canvas.drawText(
                        label,
                        startPoint.first + (textBound.width() / 2f),
                        endPoint.second,
                        textLabelPaint
                    )
                    canvas.drawLine(
                        startPoint.first, startPoint.second, endPoint.first, endPoint.second,
                        verticalSecondaryLinePaint
                    )
                    canvas.drawCircle(
                        startPoint.first - (CIRCLE_RADIUS / 2f),
                        endPoint.second - (textLabelPaint.textSize * 1.5f),
                        CIRCLE_RADIUS,
                        textLabelPaint
                    )
                }
            }
        }

    override val yearMarkerDrawFunc: YearMarkersDrawFunc = { markers ->
        { canvas ->
            val yearLinesPaint = Paint().apply {
                color = ContextCompat.getColor(context, R.color.primaryLightColor)
                style = Paint.Style.FILL
                strokeWidth = resources.displayMetrics.density
                pathEffect = DashPathEffect(floatArrayOf(3f, 12f), 50f)
            }
            markers.shiftXOffset().forEach { (label, coordinates) ->
                val startPoint = coordinates.first
                val endPoint = coordinates.second
                textLabelPaint.getTextBounds(label, 0, label.length, textBound)

                val textXPosMin = (startPoint.first - (textBound.width() / 2f)).coerceAtLeast(0f)
                val textXPos = textXPosMin.coerceAtMost(width.toFloat() - textBound.width())
                val circleXPos = startPoint.first.coerceAtMost(width.toFloat() - CIRCLE_RADIUS)
                val lineXPos = startPoint.first.coerceAtMost(width.toFloat() - 1f)
                canvas.drawText(
                    label,
                    textXPos,
                    startPoint.second + textBound.height(),
                    textLabelPaint
                )
                canvas.drawLine(
                    lineXPos,
                    startPoint.second + (textBound.height() * 3f),
                    lineXPos,
                    endPoint.second - (textBound.height() * 3f),
                    yearLinesPaint
                )
                canvas.drawCircle(
                    circleXPos,
                    startPoint.second + (textBound.height() * 2f),
                    CIRCLE_RADIUS,
                    textLabelPaint
                )
            }
        }
    }

    override val horizontalBarsDrawFunc: HorizontalBarsDrawFunc = { lines ->
        { canvas ->
            val horizontalLabelLinePaint = Paint().apply {
                color = ContextCompat.getColor(context, R.color.primaryLightColor)
                style = Paint.Style.FILL
                pathEffect = DashPathEffect(floatArrayOf(3f, 6f), 50f)
            }
            lines.forEachIndexed { index, (label, coordinates) ->
                val startPoint = coordinates.first
                val endPoint = coordinates.second
                canvas.drawText(
                    label,
                    startPoint.first,
                    startPoint.second,
                    textLabelPaint
                )
                canvas.drawLine(
                    startPoint.first + X_PADDING_START,
                    startPoint.second,
                    endPoint.first + X_PADDING_START,
                    endPoint.second,
                    horizontalLabelLinePaint
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        drawLabels = hasWindowFocus
        invalidate()
    }

    override fun onDetachedFromWindow() {
        Timber.d("onDetachedFromWindow()")
        chartAnimHandler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    fun showBollinger() {
        showBollinger = true
        invalidateAndDrawLabels()
    }

    fun hideBollinger() {
        showBollinger = false
        invalidateAndDrawLabels()
    }

    fun showSma() {
        showSma = true
        invalidateAndDrawLabels()
    }

    fun hideSma() {
        showSma = false
        invalidateAndDrawLabels()
    }

    fun showAtr() {
        showAtr = true
        invalidateAndDrawLabels()
    }

    fun hideAtr() {
        showAtr = false
        invalidateAndDrawLabels()
    }

    private fun List<LabelWithLineCoordinates>.shiftXOffset() = map { (label, coordinates) ->
        val start = coordinates.first
        val end = coordinates.second
        val startShifted = start.shiftXOffset()
        val endShifted = end.shiftXOffset()
        label to (startShifted to endShifted)
    }

    private fun XyValue.shiftXOffset() = (first + X_PADDING_START) to second

}
