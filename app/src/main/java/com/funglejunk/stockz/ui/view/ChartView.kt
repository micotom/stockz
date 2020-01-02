package com.funglejunk.stockz.ui.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.DrawableHistoricData
import timber.log.Timber
import java.time.LocalDate

class ChartView : View {

    private companion object {
        const val HORIZONTAL_LABEL_OFFSET = 72f
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

    private val horizontalLabelLinePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.primaryLightColor)
        style = Paint.Style.FILL
        pathEffect = DashPathEffect(floatArrayOf(3f, 6f), 50f)
    }

    private val textLabelPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.primaryLightColor)
        style = Paint.Style.FILL
        textSize = resources.displayMetrics.density * 8
    }

    private val verticalPrimaryLinePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.cardBorderColor)
        style = Paint.Style.FILL
    }

    private val verticalSecondaryLinePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.cardBorderColor)
        style = Paint.Style.FILL
    }

    private var path = Path()
    private var animator: ValueAnimator? = null
    private var drawLabels = false
    private val presenter = ChartViewPresenter()
    private val labels = mutableListOf<Pair<LocalDate, Float>>()
    private val horizontalLines = mutableListOf<Pair<String, Float>>()
    private val textBound = Rect()

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

            if (yValues.isEmpty()) {
                return@post
            }

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
                addListener(object : AnimatorEndListener() {
                    override fun onAnimationEnd(animation: Animator?) {
                        drawLabels = true
                        invalidate()
                    }
                })
                start()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, chartPaint)

        if (drawLabels) {
            drawLabels = false

            horizontalLines.forEachIndexed { index, (label, value) ->
                if (index != 0 && index != horizontalLines.size - 1) {
                    canvas.drawText(
                        label, 0f, value + (textLabelPaint.textSize / 2.5f), textLabelPaint
                    )
                }
                if (index != horizontalLines.size - 1) {
                    canvas.drawLine(
                        HORIZONTAL_LABEL_OFFSET,
                        height - value,
                        width.toFloat(),
                        height - value,
                        horizontalLabelLinePaint
                    )
                }
            }

            presenter.getYearMarkers(labels).forEach { (label, x) ->
                textLabelPaint.getTextBounds(label, 0, label.length, textBound)
                val xOffset = textBound.width() / 2f
                canvas.drawText(
                    label, x - xOffset, 0f + textLabelPaint.textSize, textLabelPaint
                )
                canvas.drawLine(
                    x,
                    textLabelPaint.textSize * 2,
                    x,
                    height.toFloat() - textLabelPaint.textSize,
                    verticalPrimaryLinePaint
                )
                canvas.drawCircle(
                    x, textLabelPaint.textSize * 2, 4f, textLabelPaint
                )
            }

            presenter.getMonthMarkers(labels).filterIndexed { index, _ ->
                index % 3 == 0
            }.forEach { (label, x) ->
                if (x > HORIZONTAL_LABEL_OFFSET) {
                    textLabelPaint.getTextBounds(label, 0, label.length, textBound)
                    val xOffset = textBound.width() / 2f
                    canvas.drawText(
                        label, x - xOffset, height.toFloat(), textLabelPaint
                    )
                    canvas.drawLine(
                        x,
                        textLabelPaint.textSize,
                        x,
                        height.toFloat() - (textLabelPaint.textSize * 2),
                        verticalSecondaryLinePaint
                    )
                    canvas.drawCircle(
                        x, height.toFloat() - (textLabelPaint.textSize * 2), 4f, textLabelPaint
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
}
