package com.funglejunk.stockz.ui

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.DrawableHistoricData
import timber.log.Timber
import java.util.*
import android.view.MotionEvent


class ChartView : View {

    constructor(context: Context?) : super(context) {
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
    }

    private companion object {
        private const val INVALID_POINTER_ID = -1
    }

    private var mPosX: Float = 0.toFloat()
    private var mPosY: Float = 0.toFloat()

    private var mLastTouchX: Float = 0.toFloat()
    private var mLastTouchY: Float = 0.toFloat()
    private var mActivePointerId = INVALID_POINTER_ID

    private val mScaleDetector: ScaleGestureDetector
    private var mScaleFactor = 1f

    private val yPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.primaryColor)
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2
    }

    private val xPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.cardBorderColor)
        style = Paint.Style.FILL
        textSize = resources.displayMetrics.density * 12
    }

    private var path = Path()
    private var animator: ValueAnimator? = null
    private var drawLabels = false
    private val presenter = ChartViewPresenter()
    private val labels = mutableListOf<Pair<Date, Float>>()

    fun draw(data: DrawableHistoricData) {

        post {
            Timber.d("drawing width: $width, drawing height: $height")

            labels.also {
                it.clear()
                it.addAll(presenter.calculateLabels(data, width))
            }

            val yValues = presenter.calculateChartValues(data, height)

            path.also {
                it.reset()
                it.moveTo(0.0f, height - yValues[0])
            }

            animator = ValueAnimator.ofInt(1, yValues.size - 1).apply {
                duration = 1500
                val distanceYValues = width.toFloat() / data.size
                addUpdateListener {
                    val animatedIndex = it.animatedValue as Int
                    path.lineTo(
                        distanceYValues * animatedIndex,
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

        // canvas.save()
        // canvas.translate(mPosX, mPosY)
        // canvas.scale(mScaleFactor, mScaleFactor)

        canvas.drawPath(path, yPaint)
        if (drawLabels) {
            Timber.d("drawing labels")
            drawLabels = false

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

        }

        // canvas.restore()
    }

    override fun onDetachedFromWindow() {
        Timber.d("onDetachedFromWindow()")
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    /*
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(ev)

        val action = ev.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                val x = ev.x
                val y = ev.y

                mLastTouchX = x
                mLastTouchY = y
                mActivePointerId = ev.getPointerId(0)
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = ev.findPointerIndex(mActivePointerId)
                val x = ev.getX(pointerIndex)
                val y = ev.getY(pointerIndex)

                // Only move if the ScaleGestureDetector isn't processing a gesture.
                if (!mScaleDetector.isInProgress) {
                    val dx = x - mLastTouchX
                    val dy = y - mLastTouchY

                    mPosX += dx
                    mPosY += dy

                    invalidate()
                }

                mLastTouchX = x
                mLastTouchY = y
            }

            MotionEvent.ACTION_UP -> {
                mActivePointerId = INVALID_POINTER_ID
            }

            MotionEvent.ACTION_CANCEL -> {
                mActivePointerId = INVALID_POINTER_ID
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex =
                    ev.action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = ev.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    mLastTouchX = ev.getX(newPointerIndex)
                    mLastTouchY = ev.getY(newPointerIndex)
                    mActivePointerId = ev.getPointerId(newPointerIndex)
                }
            }
        }

        Timber.d("mPosX: $mPosX, mPosY: $mPosY")

        return true
    }

     */

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            Timber.d("scale: $mScaleFactor")

            mScaleFactor *= detector.scaleFactor

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(1.0f, Math.min(mScaleFactor, 10.0f))

            invalidate()
            return true
        }
    }

}