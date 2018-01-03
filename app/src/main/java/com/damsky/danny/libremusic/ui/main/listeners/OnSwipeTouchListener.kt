package com.damsky.danny.libremusic.ui.main.listeners

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener

/**
 * An OnTouch + OnSwipe(Custom functions) listener for any View that can use OnTouchListener.
 * The class is abstract and the onSwipe functions must be overridden when setting it to a view.
 *
 * @param context The context of the activity which implements this listener.
 *
 * @author Danny Damsky
 * @since 2018-01-03
 */
abstract class OnSwipeTouchListener(context: Context) : OnTouchListener {

    private val gestureDetector: GestureDetector

    companion object {
        private const val SWIPE_THRESHOLD = 500
        private const val SWIPE_VELOCITY_THRESHOLD = 200
    }

    init {
        gestureDetector = GestureDetector(context, GestureListener())
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean = gestureDetector.onTouchEvent(event)

    private inner class GestureListener : SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent?) = true

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            var result = false
            try {
                val diffY = e2!!.y - e1!!.y
                val diffX = e2.x - e1.x
                val absX = Math.abs(diffX)

                if (absX > Math.abs(diffY))
                    if (absX > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0)
                            onSwipeRight()
                        else
                            onSwipeLeft()
                        result = true
                    }
            } finally {
                return result
            }
        }
    }

    abstract fun onSwipeRight()

    abstract fun onSwipeLeft()
}
