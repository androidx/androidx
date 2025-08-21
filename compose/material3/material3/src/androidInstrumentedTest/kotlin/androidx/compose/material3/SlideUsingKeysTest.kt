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

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
                modifier = Modifier.onFocusChanged { sliderFocused = it.isFocused },
            )
        }

        // Press tab to focus on Slider
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyUp))
        runOnIdle { assertTrue(sliderFocused) }

        var currentValue = 0.50f

        // Right key should increase value.
        onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyUp))
        runOnIdle {
            currentValue = currentValue + 1f / 100f
            assertEquals(currentValue.round2decPlaces(), (state.value).round2decPlaces())
        }

        // Left key should decrease value.
        onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyUp))
        runOnIdle {
            currentValue = currentValue - 1f / 100f
            assertEquals(currentValue.round2decPlaces(), (state.value).round2decPlaces())
        }

        // Page down should decrease value.
        onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyUp))
        runOnIdle {
            currentValue = currentValue - 1f / 10f
            assertEquals(currentValue.round2decPlaces(), (state.value).round2decPlaces())
        }

        // Page up should increase value.
        onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyUp))
        runOnIdle {
            currentValue = currentValue + 1f / 10f
            assertEquals(currentValue.round2decPlaces(), (state.value).round2decPlaces())
        }

        // Up key should increase value.
        onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyUp))
        runOnIdle {
            currentValue = currentValue + 1f / 100f
            assertEquals(currentValue.round2decPlaces(), (state.value).round2decPlaces())
        }

        // Down key should decrease value.
        onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyUp))
        runOnIdle {
            currentValue = currentValue - 1f / 100f
            assertEquals(currentValue.round2decPlaces(), (state.value).round2decPlaces())
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
                    modifier = Modifier.onFocusChanged { sliderFocused = it.isFocused },
                )
            }
        }

        // Press tab to focus on Slider
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyUp))
        runOnIdle { assertTrue(sliderFocused) }

        var currentValue = 0.50f

        // Right key should decrease value.
        onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyUp))
        runOnIdle {
            currentValue = currentValue - 1f / 100f
            assertEquals(currentValue.round2decPlaces(), (state.value).round2decPlaces())
        }

        // Left key should increase value.
        onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyUp))
        runOnIdle {
            currentValue = currentValue + 1f / 100f
            assertEquals(currentValue.round2decPlaces(), (state.value).round2decPlaces())
        }

        // Page down should decrease value.
        onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyUp))
        runOnIdle {
            currentValue = currentValue - 1f / 10f
            assertEquals(currentValue.round2decPlaces(), (state.value).round2decPlaces())
        }

        // Page up should increase value.
        onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyUp))
        runOnIdle {
            currentValue = currentValue + 1f / 10f
            assertEquals(currentValue.round2decPlaces(), (state.value).round2decPlaces())
        }

        // Up key should increase value.
        onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyUp))
        runOnIdle {
            currentValue = currentValue + 1f / 100f
            assertEquals(currentValue.round2decPlaces(), (state.value).round2decPlaces())
        }

        // Down key should decrease value.
        onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyUp))
        runOnIdle {
            currentValue = currentValue - 1f / 100f
            assertEquals(currentValue.round2decPlaces(), (state.value).round2decPlaces())
        }

        onRoot().performKeyPress(KeyEvent(Key.MoveEnd, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.MoveEnd, KeyEventType.KeyUp))
        runOnIdle { assertEquals(1f, state.value) }

        onRoot().performKeyPress(KeyEvent(Key.MoveHome, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.MoveHome, KeyEventType.KeyUp))
        runOnIdle { assertEquals(0f, state.value) }
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
                modifier = Modifier.onFocusChanged { sliderFocused = it.isFocused },
            )
        }

        // Press tab to focus on Slider
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyUp))
        runOnIdle { assertTrue(sliderFocused) }

        var currentValue = state.value

        // Right key should increase value.
        onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyUp))
        currentValue += 1f
        runOnIdle { assertEquals(currentValue, (state.value)) }

        // Left key should decrease value.
        onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyUp))
        currentValue -= 1f
        runOnIdle { assertEquals(currentValue, state.value) }

        // Page down should decrease value by 3.
        onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyUp))
        currentValue -= 3f
        runOnIdle { assertEquals(currentValue, state.value) }

        // Page up should increase value by 3.
        onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyUp))
        currentValue += 3f
        runOnIdle { assertEquals(currentValue, state.value) }

        // Up key should increase value.
        onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyUp))
        currentValue += 1f
        runOnIdle { assertEquals(currentValue, state.value) }

        // Down key should decrease value.
        onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyUp))
        currentValue -= 1f
        runOnIdle { assertEquals(currentValue, state.value) }

        onRoot().performKeyPress(KeyEvent(Key.MoveEnd, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.MoveEnd, KeyEventType.KeyUp))
        runOnIdle { assertEquals(30f, state.value) }

        onRoot().performKeyPress(KeyEvent(Key.MoveHome, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.MoveHome, KeyEventType.KeyUp))
        runOnIdle { assertEquals(0f, state.value) }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun slider_vertical_keyboardNavigation() = runComposeUiTest {
        var sliderFocused = false
        lateinit var currentValue: MutableFloatState

        setContent {
            val state =
                rememberSliderState(
                    // Only allow multiples of 10. Excluding the endpoints of `valueRange`,
                    // there are 9 steps (10, 20, ..., 90).
                    steps = 9,
                    valueRange = 0f..100f,
                )
            currentValue = rememberSaveable { mutableFloatStateOf(state.value) }
            state.onValueChange = { newValue -> currentValue.floatValue = newValue }

            VerticalSlider(
                state = state,
                modifier =
                    Modifier.testTag("Slider").height(300.dp).onFocusChanged {
                        sliderFocused = it.isFocused
                    },
                track = {
                    SliderDefaults.Track(
                        sliderState = state,
                        modifier = Modifier.width(36.dp),
                        trackCornerSize = 12.dp,
                    )
                },
            )
        }

        // Press tab to focus on Slider
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyUp))
        runOnIdle { assertTrue(sliderFocused) }

        // Press arrow key down.
        onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyUp))
        // Assert went down.
        runOnIdle { assertEquals(10f, currentValue.floatValue) }

        // Press arrow key up.
        onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyUp))
        // Assert went up.
        runOnIdle { assertEquals(0f, currentValue.floatValue) }

        // Press arrow key right.
        onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyUp))
        // Assert went down.
        runOnIdle { assertEquals(10f, currentValue.floatValue) }

        // Press arrow key left.
        onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyUp))
        // Assert went up.
        runOnIdle { assertEquals(0f, currentValue.floatValue) }

        // Press page down.
        onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyUp))
        // Assert went down.
        runOnIdle { assertEquals(10f, currentValue.floatValue) }

        // Press page up.
        onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyUp))
        // Assert went up.
        runOnIdle { assertEquals(0f, currentValue.floatValue) }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun slider_vertical_reverseDirectionTrue_keyboardNavigation() = runComposeUiTest {
        var sliderFocused = false
        lateinit var currentValue: MutableFloatState

        setContent {
            val state =
                rememberSliderState(
                    // Only allow multiples of 10. Excluding the endpoints of `valueRange`,
                    // there are 9 steps (10, 20, ..., 90).
                    steps = 9,
                    valueRange = 0f..100f,
                )
            currentValue = rememberSaveable { mutableFloatStateOf(state.value) }
            state.onValueChange = { newValue -> currentValue.floatValue = newValue }

            VerticalSlider(
                state = state,
                modifier =
                    Modifier.testTag("Slider").height(300.dp).onFocusChanged {
                        sliderFocused = it.isFocused
                    },
                track = {
                    SliderDefaults.Track(
                        sliderState = state,
                        modifier = Modifier.width(36.dp),
                        trackCornerSize = 12.dp,
                    )
                },
                reverseDirection = true,
            )
        }

        // Press tab to focus on Slider
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyUp))
        runOnIdle { assertTrue(sliderFocused) }

        // Press arrow key up.
        onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyUp))
        // Assert went up.
        runOnIdle { assertEquals(10f, currentValue.floatValue) }

        // Press arrow key down.
        onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyUp))
        // Assert went down.
        runOnIdle { assertEquals(0f, currentValue.floatValue) }

        // Press arrow key right.
        onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyUp))
        // Assert went up.
        runOnIdle { assertEquals(10f, currentValue.floatValue) }

        // Press arrow key left.
        onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyUp))
        // Assert went down.
        runOnIdle { assertEquals(0f, currentValue.floatValue) }

        // Press page up.
        onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyUp))
        // Assert went up.
        runOnIdle { assertEquals(10f, currentValue.floatValue) }

        // Press page down.
        onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyUp))
        // Assert went down.
        runOnIdle { assertEquals(0f, currentValue.floatValue) }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun slider_vertical_rtl_keyboardNavigation() = runComposeUiTest {
        var sliderFocused = false
        lateinit var currentValue: MutableFloatState

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                val state =
                    rememberSliderState(
                        // Only allow multiples of 10. Excluding the endpoints of `valueRange`,
                        // there are 9 steps (10, 20, ..., 90).
                        steps = 9,
                        valueRange = 0f..100f,
                    )
                currentValue = rememberSaveable { mutableFloatStateOf(state.value) }
                state.onValueChange = { newValue -> currentValue.floatValue = newValue }

                VerticalSlider(
                    state = state,
                    modifier =
                        Modifier.testTag("Slider").height(300.dp).onFocusChanged {
                            sliderFocused = it.isFocused
                        },
                    track = {
                        SliderDefaults.Track(
                            sliderState = state,
                            modifier = Modifier.width(36.dp),
                            trackCornerSize = 12.dp,
                        )
                    },
                )
            }
        }

        // Press tab to focus on Slider
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyUp))
        runOnIdle { assertTrue(sliderFocused) }

        // Press arrow key down.
        onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyUp))
        // Assert went down.
        runOnIdle { assertEquals(10f, currentValue.floatValue) }

        // Press arrow key up.
        onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyUp))
        // Assert went up.
        runOnIdle { assertEquals(0f, currentValue.floatValue) }

        // Press arrow key left.
        onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyUp))
        // Assert went down.
        runOnIdle { assertEquals(10f, currentValue.floatValue) }

        // Press arrow key right.
        onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyUp))
        // Assert went up.
        runOnIdle { assertEquals(0f, currentValue.floatValue) }

        // Press page down.
        onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyUp))
        // Assert went down.
        runOnIdle { assertEquals(10f, currentValue.floatValue) }

        // Press page up.
        onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyUp))
        // Assert went up.
        runOnIdle { assertEquals(0f, currentValue.floatValue) }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun slider_vertical_rtl_reverseDirectionTrue_keyboardNavigation() = runComposeUiTest {
        var sliderFocused = false
        lateinit var currentValue: MutableFloatState

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                val state =
                    rememberSliderState(
                        // Only allow multiples of 10. Excluding the endpoints of `valueRange`,
                        // there are 9 steps (10, 20, ..., 90).
                        steps = 9,
                        valueRange = 0f..100f,
                    )
                currentValue = rememberSaveable { mutableFloatStateOf(state.value) }
                state.onValueChange = { newValue -> currentValue.floatValue = newValue }

                VerticalSlider(
                    state = state,
                    modifier =
                        Modifier.testTag("Slider").height(300.dp).onFocusChanged {
                            sliderFocused = it.isFocused
                        },
                    track = {
                        SliderDefaults.Track(
                            sliderState = state,
                            modifier = Modifier.width(36.dp),
                            trackCornerSize = 12.dp,
                        )
                    },
                    reverseDirection = true,
                )
            }
        }

        // Press tab to focus on Slider
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyUp))
        runOnIdle { assertTrue(sliderFocused) }

        // Press arrow key up.
        onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionUp, KeyEventType.KeyUp))
        // Assert went up.
        runOnIdle { assertEquals(10f, currentValue.floatValue) }

        // Press arrow key down.
        onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionDown, KeyEventType.KeyUp))
        // Assert went down.
        runOnIdle { assertEquals(0f, currentValue.floatValue) }

        // Press arrow key left.
        onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionLeft, KeyEventType.KeyUp))
        // Assert went up.
        runOnIdle { assertEquals(10f, currentValue.floatValue) }

        // Press arrow key right.
        onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.DirectionRight, KeyEventType.KeyUp))
        // Assert went down.
        runOnIdle { assertEquals(0f, currentValue.floatValue) }

        // Press page up.
        onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.PageUp, KeyEventType.KeyUp))
        // Assert went up.
        runOnIdle { assertEquals(10f, currentValue.floatValue) }

        // Press page down.
        onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyDown))
        onRoot().performKeyPress(KeyEvent(Key.PageDown, KeyEventType.KeyUp))
        // Assert went down.
        runOnIdle { assertEquals(0f, currentValue.floatValue) }
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
