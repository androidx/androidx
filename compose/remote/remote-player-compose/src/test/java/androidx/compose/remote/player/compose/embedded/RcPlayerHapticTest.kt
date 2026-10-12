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
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import org.robolectric.annotation.Config

/** Haptic feedback in the embedded player, using a fake engine in place of the host view. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerHapticTest {

    @get:Rule val rule = RcPlayerTestRule()

    private val performed = mutableListOf<Int>()
    private var engineView: View? = null
    private val factory = RcHapticEngineFactory { view ->
        engineView = view
        CoreDocument.HapticEngine { type -> performed += type }
    }

    @Test
    fun hapticInAnImpulseIsPerformedOnce() {
        rule.mainClock.autoAdvance = false
        var hostView: View? = null
        play(onView = { hostView = it }) {
            RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                document.impulse(10f, 0f) { document.performHaptic(3) }
            }
        }
        repeat(3) {
            rule.mainClock.advanceTimeBy(32)
            rule.onRoot().captureToImage()
        }

        assertThat(performed).containsExactly(3)
        assertThat(engineView).isSameInstanceAs(hostView)
    }

    @Test
    fun documentTypesMapToPlatformFeedbackConstants() {
        assertThat(hapticFeedbackConstant(0)).isEqualTo(HapticFeedbackConstantsCompat.NO_HAPTICS)
        assertThat(hapticFeedbackConstant(1)).isEqualTo(HapticFeedbackConstantsCompat.LONG_PRESS)
        assertThat(hapticFeedbackConstant(12)).isEqualTo(HapticFeedbackConstantsCompat.CONFIRM)
        // Out-of-range types wrap rather than throw.
        assertThat(hapticFeedbackConstant(-1))
            .isEqualTo(HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK)
    }

    private fun play(
        onView: (View) -> Unit = {},
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        rule.setRemoteContent(
            playComposableWrapper = { composable ->
                onView(LocalView.current)
                CompositionLocalProvider(LocalRcHapticEngineFactory provides factory) {
                    Box(modifier = Modifier.size(100.dp)) { composable() }
                }
            },
            content = content,
        )
        rule.onRoot().captureToImage()
    }
}
