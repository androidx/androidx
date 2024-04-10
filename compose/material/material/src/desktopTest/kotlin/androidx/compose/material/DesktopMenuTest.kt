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

package androidx.compose.material

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.internal.keyEvent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.size
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalTestApi::class)
class DesktopMenuTest {


    private val windowSize = IntSize(200, 200)

    // Standard case: enough room to position below the anchor and align left
    @Test
    fun menu_positioning_alignLeft_belowAnchor() {
        val anchorBounds = IntRect(
            offset = IntOffset(10, 50),
            size = IntSize(50, 20)
        )
        val popupSize = IntSize(70, 70)

        val position = DropdownMenuPositionProvider(
            DpOffset.Zero,
            Density(1f)
        ).calculatePosition(
            anchorBounds,
            windowSize,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(position).isEqualTo(anchorBounds.bottomLeft)
    }

    // Standard RTL case: enough room to position below the anchor and align right
    @Test
    fun menu_positioning_rtl_alignRight_belowAnchor() {
        val anchorBounds = IntRect(
            offset = IntOffset(30, 50),
            size = IntSize(50, 20)
        )
        val popupSize = IntSize(70, 70)

        val position = DropdownMenuPositionProvider(
            DpOffset.Zero,
            Density(1f)
        ).calculatePosition(
            anchorBounds,
            windowSize,
            LayoutDirection.Rtl,
            popupSize
        )

        assertThat(position).isEqualTo(
            IntOffset(
                x = anchorBounds.right - popupSize.width,
                y = anchorBounds.bottom
            )
        )
    }

    // Not enough room to position the popup below the anchor, but enough room above
    @Test
    fun menu_positioning_alignLeft_aboveAnchor() {
        val anchorBounds = IntRect(
            offset = IntOffset(10, 150),
            size = IntSize(50, 30)
        )
        val popupSize = IntSize(70, 30)

        val position = DropdownMenuPositionProvider(
            DpOffset.Zero,
            Density(1f)
        ).calculatePosition(
            anchorBounds,
            windowSize,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(position).isEqualTo(
            IntOffset(
                x = anchorBounds.left,
                y = anchorBounds.top - popupSize.height
            )
        )
    }

    // Anchor left is at negative coordinates, so align popup to the left of the window
    @Test
    fun menu_positioning_windowLeft_belowAnchor() {
        val anchorBounds = IntRect(
            offset = IntOffset(-10, 50),
            size = IntSize(50, 20)
        )
        val popupSize = IntSize(70, 50)

        val position = DropdownMenuPositionProvider(
            DpOffset.Zero,
            Density(1f)
        ).calculatePosition(
            anchorBounds = anchorBounds,
            windowSize,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(position).isEqualTo(IntOffset(0, anchorBounds.bottom))
    }

    // (RTL) Anchor right is beyond the right of the window, so align popup to the window right
    @Test
    fun menu_positioning_rtl_windowRight_belowAnchor() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.fillMaxSize().testTag("background")) {
                    Box(Modifier.offset(x = (-10).dp).size(50.dp)) {
                        DropdownMenu(true, onDismissRequest = {}) {
                            Box(Modifier.size(50.dp).testTag("box"))
                        }
                    }
                }
            }
        }
        val windowSize = onNodeWithTag("background").getBoundsInRoot().size
        onNodeWithTag("box")
            .assertLeftPositionInRootIsEqualTo(windowSize.width - 50.dp)
    }

    @Test
    fun `pressing ESC button invokes onDismissRequest`() = runComposeUiTest {
        var dismissCount = 0
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                DropdownMenu(true, onDismissRequest = {
                    dismissCount++
                }, modifier = Modifier.testTag("dropDownMenu")) {
                    DropdownMenuItem({}) { Text("item1") }
                }
            }
        }

        onNodeWithTag("dropDownMenu")
            .performKeyPress(keyEvent(Key.Escape, KeyEventType.KeyDown))

        assertEquals(1, dismissCount)

        onNodeWithTag("dropDownMenu")
            .performKeyPress(keyEvent(Key.Escape, KeyEventType.KeyUp))

        assertEquals(1, dismissCount)
    }

    @Test
    fun `navigate DropDownMenu using arrows`() = runComposeUiTest {
        var item1Clicked = 0
        var item2Clicked = 0
        var item3Clicked = 0

        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                DropdownMenu(true, onDismissRequest = {},
                    modifier = Modifier.testTag("dropDownMenu")) {
                    DropdownMenuItem({
                        item1Clicked++
                    }) { Text("item1") }
                    DropdownMenuItem({
                        item2Clicked++
                    }) { Text("item2") }
                    DropdownMenuItem({
                        item3Clicked++
                    }) { Text("item3") }
                }
            }
        }

        fun performKeyDownAndUp(key: Key) {
            onNodeWithTag("dropDownMenu").apply {
                performKeyPress(keyEvent(key, KeyEventType.KeyDown))
                performKeyPress(keyEvent(key, KeyEventType.KeyUp))
            }
        }

        fun assertClicksCount(i1: Int, i2: Int, i3: Int) {
            runOnIdle {
                assertThat(item1Clicked).isEqualTo(i1)
                assertThat(item2Clicked).isEqualTo(i2)
                assertThat(item3Clicked).isEqualTo(i3)
            }
        }

        performKeyDownAndUp(Key.DirectionDown)
        performKeyDownAndUp(Key.Enter)
        assertClicksCount(1, 0, 0)

        performKeyDownAndUp(Key.DirectionUp)
        performKeyDownAndUp(Key.Enter)
        assertClicksCount(1, 0, 1)

        performKeyDownAndUp(Key.DirectionUp)
        performKeyDownAndUp(Key.Enter)
        assertClicksCount(1, 1, 1)

        performKeyDownAndUp(Key.DirectionDown)
        performKeyDownAndUp(Key.Enter)
        assertClicksCount(1, 1, 2)

        performKeyDownAndUp(Key.DirectionDown)
        performKeyDownAndUp(Key.Enter)
        assertClicksCount(2, 1, 2)

        performKeyDownAndUp(Key.DirectionDown)
        performKeyDownAndUp(Key.Enter)
        assertClicksCount(2, 2, 2)
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Test
    fun `right click opens DropdownMenuState`() = runComposeUiTest {
        val state = DropdownMenuState(DropdownMenuState.Status.Closed)
        setContent {
            Box(
                modifier = Modifier
                    .testTag("box")
                    .size(100.dp, 100.dp)
                    .contextMenuOpenDetector(
                        state = state
                    )
            )
        }

        onNodeWithTag("box").performMouseInput {
            rightClick(Offset(10f, 10f))
        }

        assertThat(state.status == DropdownMenuState.Status.Open(Offset(10f, 10f)))
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Test
    fun `right doesn't open DropdownMenuState when disabled`() = runComposeUiTest {
        val state = DropdownMenuState(DropdownMenuState.Status.Closed)
        setContent {
            Box(
                modifier = Modifier
                    .testTag("box")
                    .size(100.dp, 100.dp)
                    .contextMenuOpenDetector(
                        state = state,
                        enabled = false
                    )
            )
        }

        onNodeWithTag("box").performMouseInput {
            rightClick(Offset(10f, 10f))
        }

        assertThat(state.status == DropdownMenuState.Status.Closed)
    }

    @Test
    fun `pass scroll state`() = runComposeUiTest {
        val scrollState = ScrollState(0)
        setContent {
            DropdownMenu(
                true,
                onDismissRequest = {},
                modifier = Modifier.testTag("menu"),
                scrollState = scrollState
            ) {
                Box(Modifier.testTag("box").size(10000.dp, 10000.dp))
                Box(Modifier.size(10000.dp, 10000.dp))
            }
        }

        val initialPosition = onNodeWithTag("box").getUnclippedBoundsInRoot().top

        runBlocking {
            scrollState.scroll {
                scrollBy(10000f)
            }
        }
        assertThat(
            onNodeWithTag("box").getUnclippedBoundsInRoot().top
        ).isLessThan(initialPosition)

        onNodeWithTag("menu").performMouseInput {
            enter(center)
            scroll(-10000f)
        }
        assertThat(
            onNodeWithTag("box").getUnclippedBoundsInRoot().top
        ).isEqualTo(initialPosition)
    }
}
