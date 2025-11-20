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

package androidx.xr.glimmer

import android.app.Instrumentation
import android.os.Build.VERSION.SDK_INT
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource

internal fun createGlimmerRule(): GlimmerRule = GlimmerRule()

/**
 * Enters non-touch mode for tests so that Glimmer's focusables can be focused. This also enables
 * the [ComposeUiFlags.isInitialFocusOnFocusableAvailable] flag to allow initial focus on a
 * focusable item.
 */
@OptIn(ExperimentalComposeUiApi::class)
class GlimmerRule() : ExternalResource() {

    // Save the original flag value right before the test runs.
    private val savedInitialFocusAvailability = ComposeUiFlags.isInitialFocusOnFocusableAvailable

    override fun before() {
        ComposeUiFlags.isInitialFocusOnFocusableAvailable = true
        InstrumentationRegistry.getInstrumentation().setInTouchModeCompat(false)
    }

    override fun after() {
        // Restore the flag to its original value after the test finishes.
        ComposeUiFlags.isInitialFocusOnFocusableAvailable = savedInitialFocusAvailability
        // TODO(b/267253920): Add a compose test API to set/reset InputMode.
        InstrumentationRegistry.getInstrumentation().resetInTouchModeCompat()
    }

    private fun Instrumentation.setInTouchModeCompat(touchMode: Boolean) {
        if (touchMode) {
            setInTouchMode(true)
        } else {
            // setInTouchMode(false) is flaky, so we press a key to put the system in non-touch
            // mode.
            sendKeyDownUpSync(Key.Grave.nativeKeyCode)
        }
    }

    private fun Instrumentation.resetInTouchModeCompat() {
        if (SDK_INT < 33) setInTouchMode(true) else resetInTouchMode()
    }
}
