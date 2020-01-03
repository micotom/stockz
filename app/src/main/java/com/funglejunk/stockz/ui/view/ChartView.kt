package com.funglejunk.stockz.ui.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import arrow.core.extensions.list.applicative.map
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.DrawableHistoricData
import timber.log.Timber

class ChartView : View {

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

    private var path = Path()
    private var animator: ValueAnimator? = null
    private var drawLabels = false
    private val textBound = Rect()

    private lateinit var funcRegister: ChartInteractor.DrawFuncRegister

    fun draw(data: DrawableHistoricData) {
        post {
            funcRegister = ChartInteractor().prepareDrawing(
                data,
                width.toFloat(),
                height.toFloat(),
                pathResetFunc,
                animInitFunc,
                drawMonthMarkersFunc,
                drawYearMarkersFunc,
                drawHorizontalBarsFunc
            )
            funcRegister.pathResetFunc.invoke(path)
            animator = funcRegister.animatorInitFunc.invoke()
            animator?.let {
                it.also {
                    it.start()
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (drawLabels) {
            drawLabels = false
            funcRegister.horizontalBarsDrawFunc.invoke(canvas)
            funcRegister.yearMarkersDrawFunc.invoke(canvas)
            funcRegister.monthMarkersDrawFunc.invoke(canvas)
        }
        canvas.drawPath(path, chartPaint)
    }

    private val pathResetFunc: PathResetFunc = { startY ->
        { path ->
            path.reset()
            path.moveTo(HORIZONTAL_LABEL_OFFSET, height - startY)
        }
    }

    private val animInitFunc: AnimatorInitFunc =
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
                        invalidate()
                    }
                    addListener(object : AnimatorEndListener() {
                        override fun onAnimationEnd(animation: Animator?) {
                            drawLabels = true
                            invalidate()
                        }
                    })
                }
            }
        }

    private val drawMonthMarkersFunc: MonthMarkersDrawFunc =
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

    private val drawYearMarkersFunc: YearMarkersDrawFunc = { markers ->
        { canvas ->
            val yearLinesPaint = Paint().apply {
                color = ContextCompat.getColor(context, R.color.primaryLightColor) //R.color.cardBorderColor)
                style = Paint.Style.FILL
                strokeWidth = resources.displayMetrics.density * 0.25f
            }
            markers.shiftXOffset().forEach { (label, coordinates) ->
                val startPoint = coordinates.first
                val endPoint = coordinates.second
                textLabelPaint.getTextBounds(label, 0, label.length, textBound)

                val textXPosMin = (startPoint.first - (textBound.width() / 2f)).coerceAtLeast(0f)
                val textXPos = textXPosMin.coerceAtMost(width.toFloat() - textBound.width())
                val circleXPos = startPoint.first.coerceAtMost(width.toFloat() - CIRCLE_RADIUS)
                canvas.drawText(
                    label,
                    textXPos,
                    startPoint.second + textBound.height(),
                    textLabelPaint
                )
                canvas.drawLine(
                    startPoint.first,
                    startPoint.second + (textBound.height() * 3f),
                    endPoint.first,
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

    private val drawHorizontalBarsFunc: HorizontalBarsDrawFunc = { lines ->
        { canvas ->
            val horizontalLabelLinePaint = Paint().apply {
                color = ContextCompat.getColor(context, R.color.primaryLightColor)
                style = Paint.Style.FILL
                pathEffect = DashPathEffect(floatArrayOf(3f, 6f), 50f)
            }
            lines.forEachIndexed { index, (label, coordinates) ->
                val startPoint = coordinates.first
                val endPoint = coordinates.second
                val isFirstOrLastLine = index == 0 || index == lines.size -1
                if (!isFirstOrLastLine) {
                    canvas.drawText(
                        label,
                        startPoint.first,
                        startPoint.second,
                        textLabelPaint
                    )
                    canvas.drawLine(
                        startPoint.first,
                        startPoint.second,
                        endPoint.first,
                        endPoint.second,
                        horizontalLabelLinePaint
                    )
                }
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

}
