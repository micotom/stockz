package com.funglejunk.stockz.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.funglejunk.stockz.R
import com.funglejunk.stockz.textStringPercent

class VerticalBarView : View {

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

    private val rectPaints = listOf(
        Paint().apply {
            color = ContextCompat.getColor(context, R.color.primaryLightColor)
            style = Paint.Style.FILL
        },
        Paint().apply {
            color = ContextCompat.getColor(context, R.color.secondaryDarkColor)
            style = Paint.Style.FILL
        },
        Paint().apply {
            color = ContextCompat.getColor(context, R.color.secondaryColor)
            style = Paint.Style.FILL
        },
        Paint().apply {
            color = ContextCompat.getColor(context, R.color.primaryColor)
            style = Paint.Style.FILL
        }
    )

    private val textLabelPaint = Paint().apply {
        color = ContextCompat.getColor(context, android.R.color.white)
        style = Paint.Style.FILL
        textSize = resources.displayMetrics.density * 8
    }

    private val textBound = Rect()

    fun draw(vararg values: Pair<String, Float>) {
        post {
            drawVal = drawBindVal.invoke(values.toList())
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let { safeCanvas ->
            drawVal?.invoke(safeCanvas)
        }
    }

    private val drawBindVal: (List<Pair<String, Float>>) -> (Canvas) -> Unit = { values ->
        { canvas ->
            val barWidth = width.toFloat() / values.size
            val ySpread = height.toFloat() / (values.maxBy { it.second }?.second ?: 1.0f)
            values.forEachIndexed { index, (label, value) ->

                val startX = (index * barWidth).toInt()
                val endX = ((index + 1) * barWidth).toInt()
                val topY = (height - (value * ySpread)).toInt()
                val rect = Rect(startX, topY, endX, height)
                canvas.drawRect(rect, rectPaints[index % rectPaints.size])

                textLabelPaint.getTextBounds(label, 0, label.length, textBound)
                var textX = (endX - (barWidth / 2)) - textBound.width() / 2f
                canvas.drawText(
                    label,
                    textX,
                    height - (textBound.height().toFloat() * 2),
                    textLabelPaint
                )

                textLabelPaint.getTextBounds(value.toString(), 0, value.toString().length, textBound)
                textX = (endX - (barWidth / 2)) - textBound.width() / 2f
                canvas.drawText(
                    value.toString(),
                    textX,
                    height - textBound.height().toFloat(),
                    textLabelPaint
                )

            }
        }
    }

    private var drawVal: ((Canvas) -> Unit)? = null

}