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

package androidx.compose.material3

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class SlideUsingKeysTest {

    @Test
    fun slider_ltr_0steps_change_using_keys() = runComposeUiTest {
        val state = mutableStateOf(0.5f)
        var sliderFocused = false

        setContent {
            Slider(
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 0f..1f,
                modifier = Modifier.onFocusChanged { sliderFocused = it.isFocused }
            )
        }

        // Press tab to focus on Slider
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyUp))
        runOnIdle { assertTrue(sliderFocused) }

        repeat(3) {
            onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyDown))
            onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals(
                    (0.50f + (1 + it) / 100f).round2decPlaces(),
                    (state.value).round2decPlaces()
                )
            }
        }

        repeat(3) {
            onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyDown))
            onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals(
                    (0.53f - (1 + it) / 100f).round2decPlaces(),
                    (state.value).round2decPlaces()
                )
            }
        }

        repeat(3) {
            onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyDown))
            onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals(
                    (0.50f + (1 + it) / 10f).round2decPlaces(),
                    (state.value).round2decPlaces()
                )
            }
        }

        repeat(3) {
            onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyDown))
            onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals(
                    (0.80f - (1 + it) / 10f).round2decPlaces(),
                    (state.value).round2decPlaces()
                )
            }
        }

        repeat(3) {
            onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyDown))
            onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals(
                    (0.50f + (1 + it) / 100f).round2decPlaces(),
                    (state.value).round2decPlaces()
                )
            }
        }

        repeat(3) {
            onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyDown))
            onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals(
                    (0.53f - (1 + it) / 100f).round2decPlaces(),
                    (state.value).round2decPlaces()
                )
            }
        }

        onRoot().performKeyPress(KeyEvent(Key.MoveEnd, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.MoveEnd, KeyEventType.KeyUp))
        runOnIdle { assertEquals(1f, state.value) }

        onRoot().performKeyPress(KeyEvent(Key.MoveHome, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.MoveHome, KeyEventType.KeyUp))
        runOnIdle { assertEquals(0f, state.value) }
    }

    @Test
    fun slider_rtl_0steps_change_using_keys() = runComposeUiTest {
        val state = mutableStateOf(0.5f)
        var sliderFocused = false
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Slider(
                    value = state.value,
                    onValueChange = { state.value = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.onFocusChanged { sliderFocused = it.isFocused }
                )
            }
        }

        // Press tab to focus on Slider
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyUp))
        runOnIdle { assertTrue(sliderFocused) }

        repeat(3) {
            onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyDown))
            onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals(
                    (0.50f - (1 + it) / 100f).round2decPlaces(),
                    (state.value).round2decPlaces()
                )
            }
        }

        repeat(3) {
            onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyDown))
            onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyUp))
            runOnIdle {
                assertEquals(
                    (0.47f + (1 + it) / 100f).round2decPlaces(),
                    (state.value).round2decPlaces()
                )
            }
        }
    }

    @Test
    fun slider_ltr_29steps_using_keys() = runComposeUiTest {
        val state = mutableStateOf(15f)
        var sliderFocused = false
        setContent {
            Slider(
                value = state.value,
                steps = 29,
                onValueChange = { state.value = it },
                valueRange = 0f..30f,
                modifier = Modifier.onFocusChanged { sliderFocused = it.isFocused }
            )
        }

        // Press tab to focus on Slider
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyUp))
        runOnIdle { assertTrue(sliderFocused) }

        repeat(3) {
            onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyDown))
            onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyUp))
            runOnIdle { assertEquals((15f + (1f + it)), (state.value)) }
        }

        repeat(3) {
            onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyDown))
            onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyUp))
            runOnIdle { assertEquals((18f - (1 + it)), state.value) }
        }

        runOnIdle { state.value = 0f }

        val page = ((29 + 1) / 10).coerceIn(1, 10) // same logic as in Slider slideOnKeyEvents

        repeat(3) {
            onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyDown))
            onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyUp))
            runOnIdle { assertEquals((1f + it) * page, state.value) }
        }

        runOnIdle { state.value = 30f }

        repeat(3) {
            onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyDown))
            onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyUp))
            runOnIdle { assertEquals(30f - (1 + it) * page, state.value) }
        }

        runOnIdle { state.value = 0f }

        repeat(3) {
            onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyDown))
            onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyUp))
            runOnIdle { assertEquals(1f + it, state.value) }
        }

        repeat(3) {
            onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyDown))
            onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyUp))
            runOnIdle { assertEquals(3f - (1f + it), state.value) }
        }

        onRoot().performKeyPress(KeyEvent(Key.MoveEnd, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.MoveEnd, KeyEventType.KeyUp))
        runOnIdle { assertEquals(30f, state.value) }

        onRoot().performKeyPress(KeyEvent(Key.MoveHome, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.MoveHome, KeyEventType.KeyUp))
        runOnIdle { assertEquals(0f, state.value) }
    }
}

private fun KeyEventType.toNativeAction(): Int {
    return when (this) {
        KeyEventType.KeyUp -> NativeKeyEvent.ACTION_UP
        KeyEventType.KeyDown -> NativeKeyEvent.ACTION_DOWN
        else -> error("KeyEventType - $this")
    }
}

private fun KeyEvent(key: Key, type: KeyEventType): KeyEvent {
    return KeyEvent(NativeKeyEvent(type.toNativeAction(), key.nativeKeyCode))
}

private fun Float.round2decPlaces() = (this * 100).roundToInt() / 100f
