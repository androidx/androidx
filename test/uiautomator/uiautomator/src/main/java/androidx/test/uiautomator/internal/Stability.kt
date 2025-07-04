/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.test.uiautomator.internal

import android.graphics.Bitmap
import android.util.Log
import androidx.test.uiautomator.StableResult
import androidx.test.uiautomator.UiAutomatorTestScope.Companion.TAG
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal fun waitForStableInternal(
    stableTimeoutMs: Long,
    stableIntervalMs: Long,
    stablePollIntervalMs: Long,
    bitmapProvider: () -> (Bitmap?),
    rootElementNodeProvider: () -> (ElementNode),
): StableResult {

    // Setting up states pre-iterations
    var stableBitmap = bitmapProvider()
    var stableRootNode = rootElementNodeProvider()

    // This keeps updating the stable node either until the timeout
    // (in which case timeout = true) or until the stability has been achieved.
    var remainingMs = stableTimeoutMs
    var timeoutClock = TimeoutClock(timeoutMs = remainingMs, sleepIntervalMs = stablePollIntervalMs)
    fun newTimeoutClockForRemainingTimeout(): TimeoutClock {
        remainingMs -= timeoutClock.elapsedMs
        timeoutClock = TimeoutClock(timeoutMs = remainingMs, sleepIntervalMs = stablePollIntervalMs)
        return timeoutClock
    }

    while (true) {

        if (timeoutClock.elapsedMs >= stableIntervalMs) {
            val durationStr = timeoutClock.elapsedMs.toDuration(DurationUnit.MILLISECONDS)
            Log.d(TAG, "Ui was stable for $durationStr.")
            break
        }

        // Acquire another state and see if it's changed.
        val currentScreenshot = bitmapProvider()
        val currentRootNode = rootElementNodeProvider()

        val viewHierarchyEquals = stableRootNode == currentRootNode
        val bitmapEquals = stableBitmap?.sameAs(currentScreenshot) != false

        if (!viewHierarchyEquals || !bitmapEquals) {
            // If it changed, restart with another root node but reduce the remaining timeout.
            timeoutClock = newTimeoutClockForRemainingTimeout()
            stableRootNode = currentRootNode
            stableBitmap = currentScreenshot
        }

        // Wait a bit
        if (timeoutClock.isTimeoutOrSleep()) {
            Log.d(TAG, "Timeout while waiting for stable ui.")
            break
        }
    }

    return StableResult(
        node = stableRootNode.accessibilityNodeInfo,
        screenshot = stableBitmap,
        isTimeout = timeoutClock.timeout,
    )
}
