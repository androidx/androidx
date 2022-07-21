package androidx.metrics.performance.test

import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import androidx.metrics.performance.PerformanceMetricsState

class DelayedView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    var delayMs: Long = 0
    var repetitions: Int = 0
    var maxReps: Int = 0
    var perFrameStateData: List<JankStatsTest.FrameStateInputData> = listOf()

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onDraw(canvas: Canvas?) {
        repetitions++
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs)
            } catch (_: Exception) {
            }
        }
        val randomColor: Int = 0xff000000.toInt() or
            (((Math.random() * 127) + 128).toInt() shl 16) or
            (((Math.random() * 127) + 128).toInt() shl 8) or
            ((Math.random() * 127) + 128).toInt()

        canvas!!.drawColor(randomColor)
        if (perFrameStateData.isNotEmpty()) {
            val metricsState = PerformanceMetricsState.getHolderForHierarchy(this).state!!
            val stateData = perFrameStateData[repetitions - 1]
            for (state in stateData.addSFStates) {
                metricsState.putSingleFrameState(state.first, state.second)
            }
            for (state in stateData.addStates) {
                metricsState.putState(state.first, state.second)
            }
            for (stateName in stateData.removeStates) {
                metricsState.removeState(stateName)
            }
        }
        if (repetitions < maxReps) {
            if (Build.VERSION.SDK_INT >= 16) {
                postInvalidateOnAnimation()
            } else {
                postInvalidate()
            }
        }
    }
}