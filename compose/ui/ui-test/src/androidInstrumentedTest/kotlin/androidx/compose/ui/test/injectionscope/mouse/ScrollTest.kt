/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.test.injectionscope.mouse

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Scroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InputDispatcher
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.injectionscope.mouse.Common.PrimaryButton
import androidx.compose.ui.test.injectionscope.mouse.Common.runMouseInputInjectionTest
import androidx.compose.ui.test.injectionscope.mouse.Common.verifyMouseEvent
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.smoothScroll
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ScrollTest {
    companion object {
        // Used in the smoothScroll tests
        private val T = InputDispatcher.eventPeriodMillis
        private val steps = 4
        private val delta = 10f
        private val distance = delta * steps
        private val listModifier = Modifier.testTag("list")
    }

    @Test
    fun scrollVertically() =
        runMouseInputInjectionTest(
            mouseInput = {
                // scroll vertically
                scroll(10f, ScrollWheel.Vertical)
            },
            eventVerifiers =
                arrayOf(
                    { this.verifyMouseEvent(0, Enter, false, Offset.Zero) },
                    { this.verifyMouseEvent(0, Scroll, false, Offset.Zero, Offset(0f, 10f)) },
                )
        )

    @Test
    fun scrollHorizontally() =
        runMouseInputInjectionTest(
            mouseInput = {
                // scroll horizontally
                scroll(10f, ScrollWheel.Horizontal)
            },
            eventVerifiers =
                arrayOf(
                    { this.verifyMouseEvent(0, Enter, false, Offset.Zero) },
                    { this.verifyMouseEvent(0, Scroll, false, Offset.Zero, Offset(10f, 0f)) },
                )
        )

    @Test
    fun scrollWithPrimaryDown() =
        runMouseInputInjectionTest(
            mouseInput = {
                // press primary button
                press(MouseButton.Primary)
                // scroll
                scroll(10f)
            },
            eventVerifiers =
                arrayOf(
                    { this.verifyMouseEvent(0, Enter, false, Offset.Zero) },
                    { this.verifyMouseEvent(0, Press, true, Offset.Zero, PrimaryButton) },
                    {
                        this.verifyMouseEvent(
                            0,
                            Scroll,
                            true,
                            Offset.Zero,
                            Offset(0f, 10f),
                            PrimaryButton
                        )
                    },
                )
        )

    @Test
    fun smoothScrollVertically() =
        runMouseInputInjectionTest(
            mouseInput = { smoothScroll(distance, steps * T, ScrollWheel.Vertical) },
            eventVerifiers =
                arrayOf(
                    { this.verifyMouseEvent(1 * T, Enter, false, Offset.Zero) },
                    { this.verifyMouseEvent(1 * T, Scroll, false, Offset.Zero, Offset(0f, delta)) },
                    { this.verifyMouseEvent(2 * T, Scroll, false, Offset.Zero, Offset(0f, delta)) },
                    { this.verifyMouseEvent(3 * T, Scroll, false, Offset.Zero, Offset(0f, delta)) },
                    { this.verifyMouseEvent(4 * T, Scroll, false, Offset.Zero, Offset(0f, delta)) },
                )
        )

    @Test
    fun smoothScrollHorizontally() =
        runMouseInputInjectionTest(
            mouseInput = { smoothScroll(distance, steps * T, ScrollWheel.Horizontal) },
            eventVerifiers =
                arrayOf(
                    { this.verifyMouseEvent(1 * T, Enter, false, Offset.Zero) },
                    { this.verifyMouseEvent(1 * T, Scroll, false, Offset.Zero, Offset(delta, 0f)) },
                    { this.verifyMouseEvent(2 * T, Scroll, false, Offset.Zero, Offset(delta, 0f)) },
                    { this.verifyMouseEvent(3 * T, Scroll, false, Offset.Zero, Offset(delta, 0f)) },
                    { this.verifyMouseEvent(4 * T, Scroll, false, Offset.Zero, Offset(delta, 0f)) },
                )
        )

    /**
     * Integration test: checks if we are actually seeing lazy column respond to vertical scroll.
     */
    @Test
    fun smoothScrollLazyColumn() = runComposeUiTest {
        val items = 200
        setContent {
            LazyColumn(listModifier.width(50.dp)) {
                items(items) { TestItem(it, items, Modifier.fillParentMaxWidth()) }
            }
        }

        onNodeWithTag("list").performMouseInput { smoothScroll(100f, 500L, ScrollWheel.Vertical) }
        onNodeWithTag("item-${items - 1}").assertIsDisplayed()
    }

    /**
     * Integration test: checks if we are actually seeing a scrollable column respond to vertical
     * scroll.
     */
    @Test
    fun smoothScrollColumn() = runComposeUiTest {
        val items = 200
        setContent {
            Column(listModifier.width(50.dp).verticalScroll(rememberScrollState())) {
                repeat(items) { TestItem(it, items, Modifier.fillMaxWidth()) }
            }
        }

        onNodeWithTag("list").performMouseInput { smoothScroll(100f, 500L, ScrollWheel.Vertical) }
        onNodeWithTag("item-${items - 1}").assertIsDisplayed()
    }

    /** Integration test: checks if we are actually seeing lazy row respond to horizontal scroll. */
    @Test
    fun smoothScrollLazyRow() = runComposeUiTest {
        val items = 200
        setContent {
            LazyRow(listModifier.height(50.dp)) {
                items(items) { TestItem(it, items, Modifier.fillParentMaxHeight()) }
            }
        }

        onNodeWithTag("list").performMouseInput { smoothScroll(100f, 500L, ScrollWheel.Horizontal) }
        onNodeWithTag("item-${items - 1}").assertIsDisplayed()
    }

    /**
     * Integration test: checks if we are actually seeing a scrollable row respond to horizontal
     * scroll.
     */
    @Test
    fun smoothScrollRow() = runComposeUiTest {
        val items = 200
        setContent {
            Row(listModifier.height(50.dp).horizontalScroll(rememberScrollState())) {
                repeat(items) { TestItem(it, items, Modifier.fillMaxHeight()) }
            }
        }

        onNodeWithTag("list").performMouseInput { smoothScroll(100f, 500L, ScrollWheel.Horizontal) }
        onNodeWithTag("item-${items - 1}").assertIsDisplayed()
    }

    @Composable
    fun TestItem(i: Int, n: Int, modifier: Modifier = Modifier) {
        Box(
            modifier
                .testTag("item-$i")
                .defaultMinSize(10.dp, 10.dp)
                .background(Color(1f, i / n.toFloat(), 0f))
        )
    }
}
