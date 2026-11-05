package com.zak.lastfreq

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View

/** One view that draws the whole game and forwards multi-touch into the design space (360 units tall). */
class GameView(context: Context, val app: App) : View(context) {
    private var last = 0L
    private var scale = 1f

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        keepScreenOn = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            scale = h / 360f
            app.resize(w, h)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 0.016f else ((now - last) / 1_000_000_000.0).toFloat()
        last = now
        app.update(dt)
        canvas.save()
        canvas.scale(scale, scale)
        app.draw(canvas)
        canvas.restore()
        postInvalidateOnAnimation()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                app.touch(0, e.getPointerId(i), e.getX(i) / scale, e.getY(i) / scale)
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    app.touch(1, e.getPointerId(i), e.getX(i) / scale, e.getY(i) / scale)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val i = e.actionIndex
                app.touch(2, e.getPointerId(i), e.getX(i) / scale, e.getY(i) / scale)
            }
            MotionEvent.ACTION_CANCEL -> {
                for (i in 0 until e.pointerCount) {
                    app.touch(2, e.getPointerId(i), -1000f, -1000f)
                }
            }
        }
        return true
    }
}
