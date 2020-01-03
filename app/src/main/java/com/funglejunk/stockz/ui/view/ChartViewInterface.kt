package com.funglejunk.stockz.ui.view

import android.animation.ValueAnimator

interface ChartViewInterface {

    fun drawText(text: String, x: Float, y: Float)

    fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float)

    fun drawCircle(centerX: Float, centerY: Float, radius: Float)

}