/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.uiwidgets.rotations

import android.app.Activity
import android.app.Instrumentation
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertWithMessage

/**
 * Defines how to rotate in the context of an activity with an unlocked orientation. It assumes the
 * test has been set up so that the device is starting in a natural orientation.
 */
sealed class RotationUnlocked(val shouldRotate: Boolean, val rotate: (UiDevice) -> Unit) {
    object Natural : RotationUnlocked(false, {})

    object Left : RotationUnlocked(true, UiDevice::setOrientationLeft)

    object Right : RotationUnlocked(true, UiDevice::setOrientationRight)
}

private const val ROTATION_TIMEOUT_MS = 20_000L

/**
 * Rotates [device] when [RotationUnlocked.shouldRotate] is true and waits until
 * [UnlockedOrientationActivity] has been recreated and [scenario] has transitioned off the
 * pre-rotation instance.
 */
internal fun RotationUnlocked.rotateAndWait(
    device: UiDevice,
    scenario: ActivityScenario<UnlockedOrientationActivity>,
) {
    if (!shouldRotate) {
        return
    }
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val previousActivity = scenario.withActivity { this }
    val monitor =
        Instrumentation.ActivityMonitor(UnlockedOrientationActivity::class.java.name, null, false)
    instrumentation.addMonitor(monitor)
    try {
        rotate(device)

        // Screen-freeze rotation on low-end devices regularly exceeds 2s. Use
        // monitor.waitForActivityWithTimeout directly so the monitor stays registered across
        // spurious wakeups or pre-recreation matches until finally removes it.
        val deadlineMs = SystemClock.elapsedRealtime() + ROTATION_TIMEOUT_MS
        var recreatedActivity: Activity? = null
        while (SystemClock.elapsedRealtime() < deadlineMs) {
            val remainingMs = (deadlineMs - SystemClock.elapsedRealtime()).coerceAtLeast(1L)
            val candidate = monitor.waitForActivityWithTimeout(remainingMs)
            if (candidate != null && candidate !== previousActivity) {
                recreatedActivity = candidate
                break
            }
        }
        assertWithMessage(
                "Timed out after ${ROTATION_TIMEOUT_MS}ms waiting for " +
                    "UnlockedOrientationActivity recreation after rotating " +
                    "${this::class.simpleName}"
            )
            .that(recreatedActivity)
            .isNotNull()
        instrumentation.waitForIdleSync()
        assertWithMessage("ActivityScenario did not transition to a recreated activity")
            .that(scenario.withActivity { this })
            .isNotSameInstanceAs(previousActivity)
    } finally {
        instrumentation.removeMonitor(monitor)
    }
}
