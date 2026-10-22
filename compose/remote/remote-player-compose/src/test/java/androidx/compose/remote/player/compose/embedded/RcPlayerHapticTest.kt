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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/** Haptic feedback in the embedded player, verified on the host [View]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerHapticTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun hapticInAnImpulseIsPerformedOnce() {
        rule.mainClock.autoAdvance = false
        var hostView: View? = null
        play(onView = { hostView = it }) {
            RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                document.impulse(10f, 0f) { document.performHaptic(3) }
            }
        }
        val view = requireNotNull(hostView)
        val shadowView = Shadows.shadowOf(view)
        assertThat(shadowView.lastHapticFeedbackPerformed())
            .isEqualTo(HapticFeedbackConstantsCompat.KEYBOARD_TAP)

        view.performHapticFeedback(HapticFeedbackConstantsCompat.CLOCK_TICK)
        assertThat(shadowView.lastHapticFeedbackPerformed())
            .isEqualTo(HapticFeedbackConstantsCompat.CLOCK_TICK)
        repeat(3) {
            rule.mainClock.advanceTimeBy(32)
            rule.onRoot().captureToImage()
        }

        assertThat(shadowView.lastHapticFeedbackPerformed())
            .isEqualTo(HapticFeedbackConstantsCompat.CLOCK_TICK)
    }

    @Test
    fun documentTypesMapToPlatformFeedbackConstants() {
        var hostView: View? = null
        var currentDocument by mutableStateOf(hapticDocument(0))
        rule.setContent {
            val view = LocalView.current
            view.isHapticFeedbackEnabled = true
            hostView = view
            Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = currentDocument) }
        }
        val view = requireNotNull(hostView)
        val shadowView = Shadows.shadowOf(view)

        // Type 0 maps to NO_HAPTICS, which ViewCompat skips without performing feedback.
        view.performHapticFeedback(HapticFeedbackConstantsCompat.CLOCK_TICK)
        rule.onRoot().captureToImage()
        assertThat(shadowView.lastHapticFeedbackPerformed())
            .isEqualTo(HapticFeedbackConstantsCompat.CLOCK_TICK)

        currentDocument = hapticDocument(1)
        rule.waitForIdle()
        rule.onRoot().captureToImage()
        assertThat(shadowView.lastHapticFeedbackPerformed())
            .isEqualTo(HapticFeedbackConstantsCompat.LONG_PRESS)

        currentDocument = hapticDocument(12)
        rule.waitForIdle()
        rule.onRoot().captureToImage()
        assertThat(shadowView.lastHapticFeedbackPerformed())
            .isEqualTo(HapticFeedbackConstantsCompat.CONFIRM)

        // Out-of-range types wrap rather than throw.
        currentDocument = hapticDocument(-1)
        rule.waitForIdle()
        rule.onRoot().captureToImage()
        assertThat(shadowView.lastHapticFeedbackPerformed())
            .isEqualTo(HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK)
    }

    private fun hapticDocument(type: Int) = buildDocument {
        performHaptic(type)
        drawCircle(1f, 1f, 1f)
    }

    private fun play(
        onView: (View) -> Unit = {},
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        rule.setRemoteContent(
            playComposableWrapper = { composable ->
                val view = LocalView.current
                view.isHapticFeedbackEnabled = true
                onView(view)
                Box(modifier = Modifier.size(100.dp)) { composable() }
            },
            content = content,
        )
        rule.onRoot().captureToImage()
    }
}
