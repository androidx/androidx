package androidx.metrics.performance.test

import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi

public class DelayedView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    public var delayMs: Long = 0
    public var repetitions: Int = 0

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onDraw(canvas: Canvas?) {
        if (repetitions > 0) {
            repetitions--
        }
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs)
            } catch (e: Exception) {
            }
        }
        val randomColor: Int = 0xff000000.toInt() or
            (((Math.random() * 127) + 128).toInt() shl 16) or
            (((Math.random() * 127) + 128).toInt() shl 8) or
            ((Math.random() * 127) + 128).toInt()

        canvas!!.drawColor(randomColor)
        if (repetitions > 0) {
            postInvalidateOnAnimation()
        }
    }
}