/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.viewpager2.widget.swipe

import android.os.SystemClock
import android.view.animation.AccelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.isHorizontal
import androidx.viewpager2.widget.isRtl
import kotlin.math.max
import kotlin.math.roundToInt

class PageSwiperFakeDrag(private val viewPager: ViewPager2, private val pageSize: () -> Int) :
    PageSwiper {
    companion object {
        // 60 fps
        private const val FRAME_LENGTH_MS = 1000L / 60
        private const val FLING_DURATION_MS = 100L
        // VelocityTracker only considers evens in the last 100 ms
        private const val COOL_DOWN_TIME_MS = 100L
    }

    private val needsRtlModifier get() = viewPager.isHorizontal && viewPager.isRtl

    var isInterrupted: Boolean = false
        private set

    override fun swipeNext() {
        postFakeDrag(.5f, FLING_DURATION_MS, interpolator = AccelerateInterpolator())
    }

    override fun swipePrevious() {
        postFakeDrag(-.5f, FLING_DURATION_MS, interpolator = AccelerateInterpolator())
    }

    fun postFakeDrag(
        relativeDragDistance: Float,
        duration: Long,
        interpolator: Interpolator = LinearInterpolator(),
        suppressFling: Boolean = false
    ) {
        viewPager.post { fakeDrag(relativeDragDistance, duration, interpolator, suppressFling) }
    }

    fun fakeDrag(
        relativeDragDistance: Float,
        duration: Long,
        interpolator: Interpolator = LinearInterpolator(),
        suppressFling: Boolean = false
    ) {
        // Generate the deltas to feed to fakeDragBy()
        val rtlModifier = if (needsRtlModifier) -1 else 1
        val steps = max(1, (duration / FRAME_LENGTH_MS.toFloat()).roundToInt())
        val distancePx = pageSize() * -relativeDragDistance * rtlModifier
        val deltas = List(steps) { i ->
            val currDistance = interpolator.getInterpolation((i + 1f) / steps) * distancePx
            val prevDistance = interpolator.getInterpolation((i + 0f) / steps) * distancePx
            currDistance - prevDistance
        }

        if (isInterrupted) {
            throw IllegalStateException(
                "${javaClass.simpleName} was not reset after it was " +
                    "interrupted"
            )
        }

        // Send the fakeDrag events
        if (!viewPager.beginFakeDrag()) {
            markAsInterrupted()
            return
        }
        viewPager.postDelayed(FakeDragExecutor(deltas, suppressFling), FRAME_LENGTH_MS)
    }

    fun resetIsInterrupted() {
        isInterrupted = false
    }

    private fun markAsInterrupted() {
        isInterrupted = true
    }

    private inner class FakeDragExecutor(
        private val deltas: List<Float>,
        private val suppressFling: Boolean
    ) : Runnable {
        private var nextStep = 0
        private val stepsLeft get() = nextStep < deltas.size
        // If suppressFling, end with cool down period to make sure VelocityTracker has 0 velocity
        private var coolingDown = false
        private var coolDownStart = 0L

        override fun run() {
            if (coolingDown) {
                doCoolDownStep()
            } else {
                doFakeDragStep()
            }
        }

        private fun doFakeDragStep() {
            if (!viewPager.fakeDragBy(deltas[nextStep])) {
                markAsInterrupted()
                return
            }
            nextStep++

            when {
                stepsLeft -> viewPager.postDelayed(this, FRAME_LENGTH_MS)
                suppressFling -> startCoolDown()
                else -> endFakeDrag()
            }
        }

        private fun startCoolDown() {
            coolingDown = true
            coolDownStart = SystemClock.uptimeMillis()
            viewPager.postDelayed(this, FRAME_LENGTH_MS)
        }

        private fun doCoolDownStep() {
            if (!viewPager.fakeDragBy(0f)) {
                markAsInterrupted()
                return
            }
            if (SystemClock.uptimeMillis() <= coolDownStart + COOL_DOWN_TIME_MS) {
                viewPager.postDelayed(this, FRAME_LENGTH_MS)
            } else {
                endFakeDrag()
            }
        }

        private fun endFakeDrag() {
            if (!viewPager.endFakeDrag()) {
                markAsInterrupted()
            }
        }
    }
}
