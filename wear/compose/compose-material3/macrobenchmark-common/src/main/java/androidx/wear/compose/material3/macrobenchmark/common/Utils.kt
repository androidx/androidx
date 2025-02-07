/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.macrobenchmark.common

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import androidx.test.core.app.ApplicationProvider
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2

fun getRotaryInputDevice(): Int {
    val context = ApplicationProvider.getApplicationContext<Context>()
    with(context.getSystemService(Context.INPUT_SERVICE) as InputManager) {
        inputDeviceIds.forEach { deviceId ->
            // To validate that we have a valid rotary device we need to:
            // 1) check that we have a rotary device.
            // 2) check that getScaledMaximumFlingVelocity method returns us a valid fling speed
            if (
                getInputDevice(deviceId)?.motionRanges?.find {
                    it.source == InputDevice.SOURCE_ROTARY_ENCODER
                } != null
            )
                return deviceId
        }
    }
    return 0
}

fun hasRotaryInputDevice(): Boolean = getRotaryInputDevice() != 0

internal const val CONTENT_DESCRIPTION = "find-me"

internal fun numberedContentDescription(n: Int) = "$CONTENT_DESCRIPTION-$n"

internal fun UiDevice.scrollDown() {
    swipe(
        displayWidth / 2,
        displayHeight - displayHeight / 10,
        displayWidth / 2,
        displayHeight / 10,
        10
    )
}

internal fun retryIfStale(block: () -> UiObject2): UiObject2 =
    block().let {
        try {
            it.also { it.toString() }
        } catch (e: StaleObjectException) {
            block()
        }
    }

internal const val FIND_OBJECT_TIMEOUT_MS = 10_000L
