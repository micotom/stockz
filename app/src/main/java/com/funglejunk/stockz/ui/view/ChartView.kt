package com.funglejunk.stockz.ui.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.DrawableHistoricData
import java.time.LocalDate
import timber.log.Timber

class ChartView : View {

    private companion object {
        const val HORIZONTAL_LABEL_OFFSET = 72f
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    )

    private val yPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.primaryColor)
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2
    }

    private val xPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.cardBorderColor)
        style = Paint.Style.FILL
        pathEffect = DashPathEffect(floatArrayOf(10f, 20f), 0f)
    }

    private val textLabelPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.primaryLightColor)
        style = Paint.Style.FILL
        textSize = resources.displayMetrics.density * 8
    }

    private var path = Path()
    private var animator: ValueAnimator? = null
    private var drawLabels = false
    private val presenter = ChartViewPresenter()
    private val labels = mutableListOf<Pair<LocalDate, Float>>()
    private val horizontalLines = mutableListOf<Pair<String, Float>>()

    fun draw(data: DrawableHistoricData) {
        post {
            Timber.d("drawing width: $width, drawing height: $height")

            horizontalLines.addAll(
                presenter.calculateHorizontalLines(data, height)
            )

            labels.also {
                it.clear()
                it.addAll(presenter.calculateLabels(data, width))
            }

            val yValues = presenter.calculateChartValues(data, height)

            path.also {
                it.reset()
                it.moveTo(HORIZONTAL_LABEL_OFFSET, height - yValues[0])
            }

            val widthAsFloat = width.toFloat() - HORIZONTAL_LABEL_OFFSET
            animator = ValueAnimator.ofInt(1, yValues.size - 1).apply {
                duration = 1500
                val distanceYValues = widthAsFloat / data.size
                addUpdateListener {
                    val animatedIndex = it.animatedValue as Int
                    path.lineTo(
                        distanceYValues * animatedIndex + HORIZONTAL_LABEL_OFFSET,
                        height - yValues[animatedIndex]
                    )
                    invalidate()
                }
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(animation: Animator?) {
                        drawLabels = true
                        invalidate()
                    }
                    override fun onAnimationRepeat(animation: Animator?) = Unit
                    override fun onAnimationCancel(animation: Animator?) = Unit
                    override fun onAnimationStart(animation: Animator?) = Unit
                })
                start()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, yPaint)

        if (drawLabels) {
            drawLabels = false

            horizontalLines.forEachIndexed { index, (label, value) ->
                if (index != 0 && index != horizontalLines.size - 1) {
                    canvas.drawText(
                        label, 0f, value + (textLabelPaint.textSize / 2.5f), textLabelPaint
                    )
                }
                canvas.drawLine(
                    HORIZONTAL_LABEL_OFFSET, height - value, width.toFloat(), height - value, xPaint
                )
            }

            /*
            val yearMarkers = presenter.getYearMarkers(labels)
            val monthMarkers = presenter.getMonthMarkers(labels)

            val markersToDraw = when (monthMarkers.size <= 6) {
                true -> monthMarkers
                false -> yearMarkers
            }

            val twoDp = resources.displayMetrics.density * 2

            markersToDraw.forEachIndexed { index, (dateStr, x) ->
                val textHeight = when (index % 2 == 0) {
                    true -> xPaint.textSize + twoDp
                    false -> height.toFloat() - twoDp
                }
                canvas.drawText(
                    dateStr, x, textHeight, xPaint
                )
                when (index % 2 == 0) {
                    true -> canvas.drawLine(x, textHeight + twoDp, x, height.toFloat(), xPaint)
                    false -> canvas.drawLine(x, 0.0f, x, height.toFloat() - (textHeight + twoDp), xPaint)
                }
            }
             */
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
}
