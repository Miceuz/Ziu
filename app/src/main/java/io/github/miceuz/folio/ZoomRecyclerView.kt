package io.github.miceuz.folio

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.recyclerview.widget.RecyclerView

/**
 * Vertical RecyclerView drawn scaled by [scale] and shifted by [tx].
 * Vertical scrolling stays RecyclerView's own (touches are mapped into unscaled
 * coordinates); horizontal pan and pinch/double-tap zoom are handled here.
 */
class ZoomRecyclerView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {

    var scale = 1f
        private set
    private var tx = 0f
    private var dyRemainder = 0f
    private var forwarding = true
    private var insetBottom = 0
    private var animator: ValueAnimator? = null

    /** Called when a zoom gesture ends, with the new scale. */
    var onZoomSettled: ((Float) -> Unit)? = null

    /** Called from onSizeChanged, before children are laid out at the new size. */
    var onWidthChanged: ((Int) -> Unit)? = null

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean {
                zoomTo(scale * d.scaleFactor, d.focusX, d.focusY)
                return true
            }

            override fun onScaleEnd(d: ScaleGestureDetector) {
                onZoomSettled?.invoke(scale)
            }
        })

    private val gestures = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
                if (scale > 1f) {
                    tx -= dx
                    clampTx()
                    invalidate()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                animateZoom(if (scale > 1.05f) 1f else DOUBLE_TAP_SCALE, e.x, e.y)
                return true
            }
        })

    fun setInsets(top: Int, bottom: Int) {
        insetBottom = bottom
        setPadding(0, top, 0, zoomPadding())
    }

    fun resetZoom() {
        animator?.cancel()
        scale = 1f
        tx = 0f
        setPadding(0, paddingTop, 0, zoomPadding())
        invalidate()
    }

    private fun zoomTo(target: Float, fx: Float, fy: Float) {
        val s = target.coerceIn(1f, MAX_SCALE)
        if (s == scale) return
        val vx = (fx - tx) / scale
        dyRemainder += fy / scale - fy / s
        scale = s
        tx = fx - vx * s
        clampTx()
        setPadding(0, paddingTop, 0, zoomPadding())
        val dy = dyRemainder.toInt()
        dyRemainder -= dy
        scrollBy(0, dy)
        invalidate()
    }

    private fun animateZoom(target: Float, fx: Float, fy: Float) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(scale, target).apply {
            duration = 220
            addUpdateListener { zoomTo(it.animatedValue as Float, fx, fy) }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: android.animation.Animator) {
                    onZoomSettled?.invoke(scale)
                }
            })
            start()
        }
    }

    // Extra bottom padding so the end of the list can scroll into the visible
    // (height / scale) part of the view.
    private fun zoomPadding() = insetBottom + (height * (1f - 1f / scale)).toInt()

    private fun clampTx() {
        tx = tx.coerceIn(width - width * scale, 0f)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw) onWidthChanged?.invoke(w)
        setPadding(0, paddingTop, 0, zoomPadding())
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(tx, 0f)
        canvas.scale(scale, scale)
        super.dispatchDraw(canvas)
        canvas.restore()
    }

    override fun onInterceptTouchEvent(e: MotionEvent) = true

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN) {
            animator?.cancel()
            forwarding = true
        }
        scaleDetector.onTouchEvent(e)
        gestures.onTouchEvent(e)

        if (forwarding && (e.pointerCount > 1 || scaleDetector.isInProgress)) {
            val cancel = MotionEvent.obtain(e)
            cancel.action = MotionEvent.ACTION_CANCEL
            super.onTouchEvent(cancel)
            cancel.recycle()
            forwarding = false
        }
        if (forwarding) {
            val m = Matrix()
            m.setTranslate(-tx, 0f)
            m.postScale(1f / scale, 1f / scale)
            val mapped = MotionEvent.obtain(e)
            mapped.transform(m)
            super.onTouchEvent(mapped)
            mapped.recycle()
        }
        return true
    }

    private companion object {
        const val MAX_SCALE = 5f
        const val DOUBLE_TAP_SCALE = 2.5f
    }
}
