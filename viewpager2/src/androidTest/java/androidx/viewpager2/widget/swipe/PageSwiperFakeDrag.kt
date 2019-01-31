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
import androidx.core.view.ViewCompat
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import kotlin.math.max
import kotlin.math.roundToInt

class PageSwiperFakeDrag(private val viewPager: ViewPager2) : PageSwiper {
    companion object {
        // 60 fps
        private const val FRAME_LENGTH_MS = 1000L / 60
        private const val FLING_DURATION_MS = 100L
    }

    private val ViewPager2.pageSize: Int
        get() {
            return if (orientation == ORIENTATION_HORIZONTAL) {
                measuredWidth - paddingLeft - paddingRight
            } else {
                measuredHeight - paddingTop - paddingBottom
            }
        }

    private val needsRtlModifier
        get() = viewPager.orientation == ORIENTATION_HORIZONTAL &&
                ViewCompat.getLayoutDirection(viewPager) == ViewCompat.LAYOUT_DIRECTION_RTL

    override fun swipeNext() {
        fakeDrag(.5f, interpolator = AccelerateInterpolator())
    }

    override fun swipePrevious() {
        fakeDrag(-.5f, interpolator = AccelerateInterpolator())
    }

    fun fakeDrag(
        relativeDragDistance: Float,
        duration: Long = FLING_DURATION_MS,
        interpolator: Interpolator = LinearInterpolator()
    ) {
        // Generate the deltas to feed to fakeDragBy()
        val rtlModifier = if (needsRtlModifier) -1 else 1
        val steps = max(1, (duration / FRAME_LENGTH_MS.toFloat()).roundToInt())
        val distancePx = viewPager.pageSize * -relativeDragDistance * rtlModifier
        val deltas = List(steps) { i ->
            val currDistance = interpolator.getInterpolation((i + 1f) / steps) * distancePx
            val prevDistance = interpolator.getInterpolation((i + 0f) / steps) * distancePx
            currDistance - prevDistance
        }

        // Send the fakeDrag events
        var eventTime = SystemClock.uptimeMillis()
        val delayMs = { eventTime - SystemClock.uptimeMillis() }
        viewPager.post { viewPager.beginFakeDrag() }
        for (delta in deltas) {
            eventTime += FRAME_LENGTH_MS
            viewPager.postDelayed({ viewPager.fakeDragBy(delta) }, delayMs())
        }
        eventTime++
        viewPager.postDelayed({ viewPager.endFakeDrag() }, delayMs())
    }
}