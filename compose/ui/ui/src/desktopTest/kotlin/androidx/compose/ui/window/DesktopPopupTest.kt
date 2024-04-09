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

package androidx.compose.ui.window

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test

class DesktopPopupTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `pass composition locals to popup`() {
        val compositionLocal = staticCompositionLocalOf<Int> {
            error("not set")
        }

        var actualLocalValue = 0

        rule.setContent {
            CompositionLocalProvider(compositionLocal provides 3) {
                Popup {
                    actualLocalValue = compositionLocal.current
                }
            }
        }

        Truth.assertThat(actualLocalValue).isEqualTo(3)
    }

    @Test
    fun `onDispose inside popup`() {
        var isPopupShowing by mutableStateOf(true)
        var isDisposed = false

        rule.setContent {
            if (isPopupShowing) {
                Popup {
                    DisposableEffect(Unit) {
                        onDispose {
                            isDisposed = true
                        }
                    }
                }
            }
        }

        isPopupShowing = false
        rule.waitForIdle()

        Truth.assertThat(isDisposed).isEqualTo(true)
    }

    @Test
    fun `use density inside popup`() {
        var density by mutableStateOf(Density(2f, 1f))
        var densityInsidePopup = 0f

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                Popup {
                    densityInsidePopup = LocalDensity.current.density
                }
            }
        }

        Truth.assertThat(densityInsidePopup).isEqualTo(2f)

        density = Density(3f, 1f)
        rule.waitForIdle()
        Truth.assertThat(densityInsidePopup).isEqualTo(3f)
    }

    @Test(timeout = 5000) // TODO(demin): why, when an error has occurred, this test never ends?
    fun `(Bug) after open popup use derivedStateOf inside main window draw`() {
        var showPopup by mutableStateOf(false)

        rule.setContent {
            val isPressed = derivedStateOf { false }

            Canvas(Modifier.size(100.dp)) {
                isPressed.value
            }

            if (showPopup) {
                Popup {
                    Box(Modifier)
                }
            }
        }

        rule.waitForIdle()

        showPopup = true

        rule.waitForIdle()
    }

    @Test(timeout = 5000)
    fun `(Bug) after open popup use sendApplyNotifications inside main window draw`() {
        var showPopup by mutableStateOf(false)

        rule.setContent {
            Canvas(Modifier.size(100.dp)) {
                if (showPopup) {
                    Snapshot.sendApplyNotifications()
                }
            }

            if (showPopup) {
                Popup {
                    Box(Modifier)
                }
            }
        }

        rule.waitForIdle()

        showPopup = true

        rule.waitForIdle()
    }

    @Test(timeout = 5000)
    fun `(Bug) after open popup use sendApplyNotifications inside popup layout`() {
        var showPopup by mutableStateOf(false)
        var state by mutableStateOf(0)
        var applyState by mutableStateOf(false)
        var lastCompositionState = 0

        rule.setContent {
            Canvas(Modifier.size(100.dp)) {
                lastCompositionState = state
            }

            if (showPopup) {
                Popup {
                    Layout(
                        content = {},
                        measurePolicy = { _, _ ->
                            layout(10, 10) {
                                if (applyState && state == 0) {
                                    state++
                                    Snapshot.sendApplyNotifications()
                                }
                            }
                        }
                    )
                }
            }
        }

        rule.waitForIdle()

        showPopup = true

        rule.waitForIdle()

        applyState = true

        rule.waitForIdle()

        Truth.assertThat(lastCompositionState).isEqualTo(1)
    }

    @Test(timeout = 5000)
    fun `(Bug) use Popup inside LazyColumn`() {
        rule.setContent {
            var count by remember { mutableStateOf(0) }
            LazyColumn {
                items(count) {
                    Popup { }
                }
            }
            LaunchedEffect(Unit) {
                withFrameNanos {
                    count++
                }
                withFrameNanos {
                    count++
                }
            }
        }

        rule.waitForIdle()
    }
}
