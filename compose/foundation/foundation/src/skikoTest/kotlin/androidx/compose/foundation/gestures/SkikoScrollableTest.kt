/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.skiko.KotlinBackend
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import org.jetbrains.skiko.kotlinBackend

@OptIn(ExperimentalTestApi::class)
class SkikoScrollableTest {
    @Test
    fun proper_default_fling_behavior() = runComposeUiTest {
        val state by mutableStateOf(LazyListState())

        setContent {
            LazyColumn(state = state, modifier = Modifier.testTag("list").fillMaxSize()) {
                items(1000) {
                    Box(Modifier.size(50.dp))
                }
            }
        }

        runOnIdle { assertEquals(0, state.firstVisibleItemIndex) }

        onNodeWithTag("list").performTouchInput {
            swipe(Offset(30f, 500f), Offset(30f, 10f))
        }

        when (hostOs) {
            OS.Ios -> runOnIdle { assertEquals(33, state.firstVisibleItemIndex) }
            else -> runOnIdle { assertEquals(27, state.firstVisibleItemIndex) }
        }
    }

    // bug https://github.com/JetBrains/compose-multiplatform/issues/3551 (mouse didn't work)
    @Test
    fun recreating_list_state_shouldn_t_break_mouse_scrolling() = runComposeUiTest {
        var state by mutableStateOf(LazyListState())
        setContent {
            LazyColumn(state = state, modifier = Modifier.testTag("list").fillMaxSize()) {
                items(1000) {
                    Box(Modifier.size(50.dp))
                }
            }
        }

        runOnIdle { assertTrue(state.firstVisibleItemIndex == 0) }

        onNodeWithTag("list").performMouseInput {
            scroll(1000f, ScrollWheel.Vertical)
        }
        runOnIdle { assertTrue(state.firstVisibleItemIndex > 0) }

        state = LazyListState()
        runOnIdle { assertTrue(state.firstVisibleItemIndex == 0) }

        onNodeWithTag("list").performMouseInput {
            scroll(1000f, ScrollWheel.Vertical)
        }
        runOnIdle { assertTrue(state.firstVisibleItemIndex > 0) }
    }

    // bug https://github.com/JetBrains/compose-multiplatform/issues/3551 (touch always worked)
    @Test
    fun recreating_list_state_shouldn_t_break_touch_scrolling() = runComposeUiTest {
        if (kotlinBackend == KotlinBackend.Native) return@runComposeUiTest

        var state by mutableStateOf(LazyListState())
        setContent {
            LazyColumn(state = state, modifier = Modifier.testTag("list").fillMaxSize()) {
                items(1000) {
                    Box(Modifier.size(50.dp))
                }
            }
        }

        runOnIdle { assertTrue(state.firstVisibleItemIndex == 0) }

        onNodeWithTag("list").performTouchInput {
            swipe(Offset(30f, 30f), Offset(30f, 10f))
        }
        runOnIdle { assertTrue(state.firstVisibleItemIndex > 0) }

        state = LazyListState()
        runOnIdle { assertTrue(state.firstVisibleItemIndex == 0) }

        onNodeWithTag("list").performTouchInput {
            swipe(Offset(30f, 30f), Offset(30f, 10f))
        }
        runOnIdle { assertTrue(state.firstVisibleItemIndex > 0) }
    }

    @OptIn(InternalComposeUiApi::class)
    @Test
    fun closeSceneDuringScrollAnimation() = runSkikoComposeUiTest(
        size = Size(100f, 100f),
    ) {
        setContent {
            val state = rememberScrollState()
            Column(Modifier.size(10.dp).verticalScroll(state)) {
                Box(Modifier.size(10.dp, 210.dp))
            }
        }
        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = Offset(0f, 0f),
            scrollDelta = Offset(0f, 10000f)
        )
        /*
         * This test doesn't contain any real assertions - its primary function is to ensure
         * it doesn't freeze. This was an issue previously, as inside the scroll animation, there
         * was a handler for the CancellationException that used to stop the scene from closing.
         */
    }
}