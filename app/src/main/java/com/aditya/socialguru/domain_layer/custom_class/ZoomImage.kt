package com.aditya.socialguru.domain_layer.custom_class

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.annotation.Nullable
import androidx.appcompat.widget.AppCompatImageView

class ZoomImage @JvmOverloads constructor(
    context: Context,  attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), View.OnTouchListener,
    GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    private val mScaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val mGestureDetector = GestureDetector(context, this)
    private val mMatrix = Matrix()
    private val mMatrixValues = FloatArray(9)
    private var mode = NONE

    private var mSaveScale = 1f
    private var mMinScale = 1f
    private var mMaxScale = 4f

    private var origWidth = 0f
    private var origHeight = 0f
    private var viewWidth = 0
    private var viewHeight = 0

    private val mLast = PointF()
    private val mStart = PointF()

    init {
        isClickable = true
        imageMatrix = mMatrix
        scaleType = ScaleType.MATRIX
        setOnTouchListener(this)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            mSaveScale = (mSaveScale * scaleFactor).coerceIn(mMinScale, mMaxScale)

            val focusX = if (origWidth * mSaveScale <= viewWidth || origHeight * mSaveScale <= viewHeight) {
                viewWidth / 2f
            } else {
                detector.focusX
            }
            val focusY = if (origWidth * mSaveScale <= viewWidth || origHeight * mSaveScale <= viewHeight) {
                viewHeight / 2f
            } else {
                detector.focusY
            }

            mMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
            fixTranslation()
            return true
        }
    }

    private fun fitToScreen() {
        mSaveScale = 1f
        val drawable = drawable ?: return
        val imageWidth = drawable.intrinsicWidth
        val imageHeight = drawable.intrinsicHeight

        val scale = (viewWidth.toFloat() / imageWidth).coerceAtMost(viewHeight.toFloat() / imageHeight)
        mMatrix.setScale(scale, scale)

        val redundantYSpace = (viewHeight - scale * imageHeight) / 2f
        val redundantXSpace = (viewWidth - scale * imageWidth) / 2f
        mMatrix.postTranslate(redundantXSpace, redundantYSpace)

        origWidth = viewWidth - 2 * redundantXSpace
        origHeight = viewHeight - 2 * redundantYSpace
        imageMatrix = mMatrix
    }

    private fun fixTranslation() {
        mMatrix.getValues(mMatrixValues)
        val transX = mMatrixValues[Matrix.MTRANS_X]
        val transY = mMatrixValues[Matrix.MTRANS_Y]

        val fixTransX = getFixTranslation(transX, viewWidth.toFloat(), origWidth * mSaveScale)
        val fixTransY = getFixTranslation(transY, viewHeight.toFloat(), origHeight * mSaveScale)

        if (fixTransX != 0f || fixTransY != 0f) {
            mMatrix.postTranslate(fixTransX, fixTransY)
        }
    }

    private fun getFixTranslation(trans: Float, viewSize: Float, contentSize: Float): Float {
        return when {
            contentSize <= viewSize -> 0f
            trans < viewSize - contentSize -> viewSize - contentSize - trans
            trans > 0 -> -trans
            else -> 0f
        }
    }

    private fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        return if (contentSize <= viewSize) 0f else delta
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)

        if (mSaveScale == 1f) {
            fitToScreen()
        }
    }

    override fun onTouch(view: View?, event: MotionEvent): Boolean {
        mScaleDetector.onTouchEvent(event)
        mGestureDetector.onTouchEvent(event)

        val currentPoint = PointF(event.x, event.y)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mLast.set(currentPoint)
                mStart.set(mLast)
                mode = DRAG
            }
            MotionEvent.ACTION_MOVE -> if (mode == DRAG) {
                val dx = currentPoint.x - mLast.x
                val dy = currentPoint.y - mLast.y
                val fixTransX = getFixDragTrans(dx, viewWidth.toFloat(), origWidth * mSaveScale)
                val fixTransY = getFixDragTrans(dy, viewHeight.toFloat(), origHeight * mSaveScale)
                mMatrix.postTranslate(fixTransX, fixTransY)
                fixTranslation()
                mLast.set(currentPoint)
            }
            MotionEvent.ACTION_POINTER_UP -> mode = NONE
        }

        imageMatrix = mMatrix
        return false
    }

    override fun onDown(event: MotionEvent) = false
    override fun onShowPress(event: MotionEvent) {}
    override fun onSingleTapUp(event: MotionEvent) = false
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float) = false
    override fun onLongPress(event: MotionEvent) {}
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float) = false
    override fun onSingleTapConfirmed(event: MotionEvent) = false

    override fun onDoubleTap(event: MotionEvent): Boolean {
        fitToScreen()
        return true
    }

    override fun onDoubleTapEvent(event: MotionEvent) = false

    companion object {
        const val NONE = 0
        const val DRAG = 1
        const val ZOOM = 2
    }
}

