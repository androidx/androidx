/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.ui.FrameRateCategory
import platform.QuartzCore.CADisplayLink
import platform.QuartzCore.CAFrameRateRangeDefault
import platform.darwin.NSInteger

/**
 * Stores a pending frame-rate vote for a [CADisplayLink].
 *
 * [voteFrameRate] resolves exact and category votes into a concrete frame rate and keeps the
 * highest resolved value. [updateFrameRateIfNeeded] applies the pending value to
 * [CADisplayLink.preferredFramesPerSecond] and clears it.
 */
internal class DisplayLinkFrameRate(
    private val caDisplayLink: CADisplayLink,
) {
    var frameRateVote: Float = Float.NaN
    var maximumFramesPerSecond: NSInteger = 0

    var preferredFramesPerSecond: NSInteger
        get() = caDisplayLink.preferredFramesPerSecond
        set(value) {
            if (caDisplayLink.preferredFramesPerSecond == value) return
            caDisplayLink.preferredFramesPerSecond = value
        }

    private val isFrameRateVoteSet: Boolean get() = !frameRateVote.isNaN()

    fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
        val frameRateCategoryValue = when (frameRateCategory) {
            FrameRateCategory.Default.value -> CAFrameRateRangeDefault.preferred
            FrameRateCategory.Normal.value -> 60f
            FrameRateCategory.High.value -> maximumFramesPerSecond.toFloat()
            else -> Float.NaN
        }

        val resolvedFrameRate = when {
            !frameRate.isNaN() && !frameRateCategoryValue.isNaN() -> maxOf(frameRate, frameRateCategoryValue)
            !frameRate.isNaN() -> frameRate
            !frameRateCategoryValue.isNaN() -> frameRateCategoryValue
            else -> return
        }

        if (!isFrameRateVoteSet || resolvedFrameRate > frameRateVote) {
            frameRateVote = resolvedFrameRate
        }
    }

    fun updateFrameRateIfNeeded() {
        if (isFrameRateVoteSet) {
            preferredFramesPerSecond = frameRateVote.toLong()
            frameRateVote = Float.NaN
        }
    }
}