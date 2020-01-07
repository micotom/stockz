package com.funglejunk.stockz.ui.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import arrow.core.extensions.list.applicative.map
import arrow.syntax.function.partially1
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import timber.log.Timber
import kotlin.math.abs

class ChartView : View, ChartViewInterface {

    private companion object {
        const val HORIZONTAL_LABEL_OFFSET = 72f
        const val CIRCLE_RADIUS = 4f
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    )

    private val chartPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.primaryColor)
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2
    }

    private val textLabelPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.primaryLightColor)
        style = Paint.Style.FILL
        textSize = resources.displayMetrics.density * 8
    }

    private val path = Path()
    private val textBound = Rect()

    private var animator: ValueAnimator? = null
    private var funcRegister: ChartInteractor.DrawFuncRegister? = null

    private var drawLabels = false
    private var showSma = false
    private var showBollinger = false
    private var showAtr = false
    private var xScale = 1.0f

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

    fun draw(data: FBoerseHistoryData) {
        post {
            val isInPortraitMode =
                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            funcRegister = ChartInteractor().prepareDrawing(
                data,
                width.toFloat(),
                height.toFloat(),
                isInPortraitMode,
                this
            )
            funcRegister?.let { safeFuncRegister ->
                safeFuncRegister.pathResetFunc.invoke(path)
                animator = safeFuncRegister.animatorInitFunc.invoke()
                animator?.let {
                    it.also {
                        it.start()
                    }
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        Timber.d("ondraw() -> scale: $xScale, dx: $dx")
        canvas.scale(xScale, 1f, (width / (dx ?: 0f)) + width, 0f)
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
        canvas.restore()
    }

    override val pathResetFunc: PathResetFunc = { startY ->
        { path ->
            path.reset()
            path.moveTo(HORIZONTAL_LABEL_OFFSET, height - startY)
        }
    }

    override val animatorInitFunc: AnimatorInitFunc =
        { chartPoints, xValueSpreadBetweenPoints ->
            {
                ValueAnimator.ofInt(1, chartPoints.size - 1).apply {
                    duration = 1500
                    addUpdateListener {
                        val animatedIndex = it.animatedValue as Int
                        path.lineTo(
                            xValueSpreadBetweenPoints * animatedIndex + HORIZONTAL_LABEL_OFFSET,
                            height - chartPoints[animatedIndex]
                        )
                        invalidateAndDrawLabels()
                    }
                    addListener(object : AnimatorEndListener() {
                        override fun onAnimationEnd(animation: Animator?) = Unit
                    })
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
                        startPoint.first - (textBound.width() / 2f),
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
                val isFirstOrLastLine = index == 0 || index == lines.size - 1
                if (!isFirstOrLastLine) {
                    canvas.drawText(
                        label,
                        startPoint.first,
                        startPoint.second,
                        textLabelPaint
                    )
                    canvas.drawLine(
                        startPoint.first + HORIZONTAL_LABEL_OFFSET,
                        startPoint.second,
                        endPoint.first,
                        endPoint.second,
                        horizontalLabelLinePaint
                    )
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return when (event) {
            null -> false
            else -> when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val eventX = event.rawX
                    Timber.d("action down x: $eventX")
                    processXScale = calculateXScale.partially1(eventX).invoke()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val eventX = event.rawX
                    Timber.d("action move x: $eventX")
                    processXScale?.invoke(eventX)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    upX = event.rawX
                    true
                }
                else -> false
            }
        }
    }

    private var processXScale: ((Float) -> Unit)? = null

    private var dx: Float? = null

    private var upX: Float? = null

    private val calculateXScale: (Float) -> (Float) -> Unit = { prevX ->
        { newX ->
            val safeDx = dx ?: 0f
            dx = safeDx + (prevX - newX)
            if (dx != 0f) {
                Timber.d("view width: $width")
                Timber.w("x diff: $dx")
                xScale = (((dx ?: 0f) / (height.toFloat() * 4f)) + 1f).coerceAtLeast(1f)
                Timber.d("scale x: $xScale")
                invalidateAndDrawLabels()
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
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    private abstract class AnimatorEndListener : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) = Unit
        abstract override fun onAnimationEnd(animation: Animator?)
        override fun onAnimationCancel(animation: Animator?) = Unit
        override fun onAnimationStart(animation: Animator?) = Unit
    }

    private fun List<LabelWithLineCoordinates>.shiftXOffset() = map { (label, coordinates) ->
        val start = coordinates.first
        val end = coordinates.second
        val startShifted = start.copy(start.first + HORIZONTAL_LABEL_OFFSET)
        val endShifted = end.copy(end.first + HORIZONTAL_LABEL_OFFSET)
        label to (startShifted to endShifted)
    }

    private fun XyValue.shiftXOffset() = (first + HORIZONTAL_LABEL_OFFSET) to second

}
