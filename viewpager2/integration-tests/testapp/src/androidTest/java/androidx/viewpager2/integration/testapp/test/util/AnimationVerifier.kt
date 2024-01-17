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

package androidx.viewpager2.integration.testapp.test.util

import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.viewpager2.widget.ViewPager2
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import org.hamcrest.CoreMatchers.equalTo

/**
 * Verifies if animation happened in the given [ViewPager2]. It listens to
 * [ViewPager2.OnPageChangeCallback.onPageScrolled] to wait for a frame where the ViewPager2 is not
 * snapped and assumes that if animation should occur, it will occur in that frame.
 */
class AnimationVerifier(private val viewPager: ViewPager2) {
    private val epsilon = 0.00001f
    private val timeout = 2L
    private val timeoutUnit = TimeUnit.SECONDS

    private lateinit var recordAnimationLatch: CountDownLatch
    private var foundAnimatedFrame = false
    private var hasRotation = false
    private var hasTranslation = false
    private var hasScale = false

    private val isAnimationRecorded get() = recordAnimationLatch.count == 0L

    private val callback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(position: Int, offset: Float, offsetPx: Int) {
            if (!foundAnimatedFrame && offsetPx != 0) {
                foundAnimatedFrame = true
                // Page transformations are done *after* OnPageChangeCallbacks are called,
                // so postpone the actual verification
                viewPager.post {
                    recordAnimationProperties(position)
                }
            }
        }
    }

    init {
        reset()
    }

    fun awaitAnimation() {
        assertThat(
            "Couldn't get hold of an animated frame, so can't verify if it worked",
            recordAnimationLatch.await(timeout, timeoutUnit),
            equalTo(true)
        )
    }

    fun verify(expectRotation: Boolean, expectTranslation: Boolean, expectScale: Boolean) {
        assertThat(isAnimationRecorded, equalTo(true))
        assertThat(hasRotation, equalTo(expectRotation))
        assertThat(hasTranslation, equalTo(expectTranslation))
        assertThat(hasScale, equalTo(expectScale))
    }

    fun reset() {
        // Unregister potentially lingering callback
        viewPager.unregisterOnPageChangeCallback(callback)
        viewPager.registerOnPageChangeCallback(callback)
        // Reset recording mechanism
        recordAnimationLatch = CountDownLatch(1)
        foundAnimatedFrame = false
        // Actual values don't need to be reset, they will be overwritten in the next recording
    }

    private fun recordAnimationProperties(position: Int) {
        // Get hold of the page at the specified position
        val recyclerView = viewPager.getChildAt(0) as RecyclerView
        val lm = recyclerView.layoutManager as LinearLayoutManager
        val page = lm.findViewByPosition(position)

        // Get the animation values to verify
        hasRotation = !isZero(page!!.rotation) || !isZero(page.rotationX) ||
            !isZero(page.rotationY)
        hasTranslation = !isZero(page.translationX) || !isZero(page.translationY) ||
            !isZero(ViewCompat.getTranslationZ(page))
        hasScale = !isOne(page.scaleX) || !isOne(page.scaleY)

        // Mark verification as done
        recordAnimationLatch.countDown()
        viewPager.unregisterOnPageChangeCallback(callback)
    }

    private fun isZero(f: Float): Boolean {
        return abs(f) < epsilon
    }

    private fun isOne(f: Float): Boolean {
        return isZero(f - 1)
    }
}
