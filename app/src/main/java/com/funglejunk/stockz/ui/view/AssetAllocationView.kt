package com.funglejunk.stockz.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.funglejunk.stockz.R
import com.funglejunk.stockz.model.portfolio.PortfolioSummary
import com.funglejunk.stockz.textStringPercent
import java.math.BigDecimal

class AssetAllocationView : View {

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

    private val textLabelPaint = Paint().apply {
        color = ContextCompat.getColor(context, android.R.color.white)
        style = Paint.Style.FILL
        textSize = resources.displayMetrics.density * 8
    }

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

    private val textBound = Rect()

    fun applyData(data: PortfolioSummary) {
        post {
            val allocData = data.assets.map {
                val share =
                    (it.currentTotalValueWE / data.currentValueEuroWE)
                Triple(it.isin, it.targetAllocationPercent, share)
            }
            rectDrawFunc = rectDrawBindFunc.invoke(allocData)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rectDrawFunc?.invoke(canvas)
    }

    private val rectDrawBindFunc: (List<Triple<String, Double, BigDecimal>>) -> (Canvas) -> Unit =
        { allocData ->
            { canvas ->
                allocData.forEachIndexed { index, (isin, targetAlloc, alloc) ->
                    val shiftX = allocData.foldIndexed(0.toBigDecimal()) { i, acc, (_, _, allocT) ->
                        when (i < index) {
                            true -> acc + width.toBigDecimal() * allocT
                            false -> acc
                        }
                    }
                    val endX = width.toBigDecimal() * alloc + shiftX
                    val rect = Rect(shiftX.toInt(), 0, endX.toInt(), height)
                    canvas.drawRect(rect, rectPaints[index % rectPaints.size])
                    textLabelPaint.getTextBounds(isin, 0, isin.length, textBound)
                    canvas.drawText(
                        isin,
                        shiftX.toFloat() + 12,
                        (height / 2f) - (textBound.height() / 2f),
                        textLabelPaint
                    )
                    val isinHeight = textBound.height()
                    val allocPercString = (alloc * 100.0.toBigDecimal()).textStringPercent()
                    textLabelPaint.getTextBounds(allocPercString, 0, allocPercString.length, textBound)
                    canvas.drawText(
                        allocPercString,
                        shiftX.toFloat() + 12,
                        (height / 2f) - (textBound.height() / 2f) + isinHeight + 12,
                        textLabelPaint
                    )
                }
            }
        }

    private var rectDrawFunc: ((Canvas) -> Unit)? = null

}