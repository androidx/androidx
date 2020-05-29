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

package androidx.ui.material

import android.util.DisplayMetrics
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.test.filters.MediumTest
import androidx.ui.core.LayoutDirection
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.clickable
import androidx.ui.foundation.contentColor
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.preferredSize
import androidx.ui.layout.size
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.testTag
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.find
import androidx.ui.test.findByTag
import androidx.ui.test.hasAnyChildThat
import androidx.ui.test.hasTestTag
import androidx.ui.test.isPopup
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.waitForIdle
import androidx.ui.unit.Density
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.Position
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class MenuTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun menu_canBeTriggered() {
        var expanded by mutableStateOf(false)

        composeTestRule.clockTestRule.pauseClock()
        composeTestRule.setContent {
            DropdownMenu(
                expanded = expanded,
                toggle = {
                    Box(Modifier.size(20.dp).drawBackground(Color.Blue))
                },
                onDismissRequest = {}
            ) {
                DropdownMenuItem(modifier = Modifier.testTag("MenuContent"), onClick = {}) {
                    Text("Option 1")
                }
            }
        }
        findByTag("MenuContent").assertDoesNotExist()

        runOnIdleCompose { expanded = true }
        waitForIdle()
        composeTestRule.clockTestRule.advanceClock(InTransitionDuration.toLong())
        findByTag("MenuContent").assertExists()

        runOnIdleCompose { expanded = false }
        waitForIdle()
        composeTestRule.clockTestRule.advanceClock(OutTransitionDuration.toLong())
        findByTag("MenuContent").assertDoesNotExist()

        runOnIdleCompose { expanded = true }
        waitForIdle()
        composeTestRule.clockTestRule.advanceClock(InTransitionDuration.toLong())
        findByTag("MenuContent").assertExists()
    }

    @Test
    fun menu_hasExpectedSize() {
        composeTestRule.setContent {
            DropdownMenu(
                expanded = true,
                toggle = {
                    Box(Modifier.size(20.dp).drawBackground(Color.Blue))
                },
                onDismissRequest = {}
            ) {
                Semantics(properties = { testTag = "MenuContent1" }, container = true) {
                    Box(Modifier.preferredSize(70.dp))
                }
                Semantics(properties = { testTag = "MenuContent2" }, container = true) {
                    Box(Modifier.preferredSize(130.dp))
                }
            }
        }

        findByTag("MenuContent1").assertExists()
        findByTag("MenuContent2").assertExists()
        val node = find(
            isPopup() and hasAnyChildThat(hasTestTag("MenuContent1")) and
                    hasAnyChildThat(hasTestTag("MenuContent2"))
        ).assertExists().fetchSemanticsNode()
        with(composeTestRule.density) {
            assertThat(node.size.width).isEqualTo(130.dp.toIntPx() + MenuElevation.toIntPx() * 2)
            assertThat(node.size.height).isEqualTo(200.dp.toIntPx() +
                    DropdownMenuVerticalPadding.toIntPx() * 2 + MenuElevation.toIntPx() * 2
            )
        }
    }

    @Test
    fun menu_positioning_bottomEnd() {
        val screenWidth = 500
        val screenHeight = 1000
        val density = Density(1f)
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = screenWidth
            heightPixels = screenHeight
        }
        val anchorPosition = IntPxPosition(100.ipx, 200.ipx)
        val anchorSize = IntPxSize(10.ipx, 20.ipx)
        val inset = with(density) { MenuElevation.toIntPx() }
        val offsetX = 20
        val offsetY = 40
        val popupSize = IntPxSize(50.ipx, 80.ipx)

        val ltrPosition = DropdownMenuPositionProvider(
            Position(offsetX.dp, offsetY.dp),
            density,
            displayMetrics
        ).calculatePosition(
            anchorPosition,
            anchorSize,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(ltrPosition.x).isEqualTo(
            anchorPosition.x + anchorSize.width - inset + offsetX.ipx
        )
        assertThat(ltrPosition.y).isEqualTo(
            anchorPosition.y + anchorSize.height - inset + offsetY.ipx
        )

        val rtlPosition = DropdownMenuPositionProvider(
            Position(offsetX.dp, offsetY.dp),
            density,
            displayMetrics
        ).calculatePosition(
            anchorPosition,
            anchorSize,
            LayoutDirection.Rtl,
            popupSize
        )

        assertThat(rtlPosition.x).isEqualTo(
            anchorPosition.x - popupSize.width + inset - offsetX.ipx
        )
        assertThat(rtlPosition.y).isEqualTo(
            anchorPosition.y + anchorSize.height - inset + offsetY.ipx
        )
    }

    @Test
    fun menu_positioning_topStart() {
        val screenWidth = 500
        val screenHeight = 1000
        val density = Density(1f)
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = screenWidth
            heightPixels = screenHeight
        }
        val anchorPosition = IntPxPosition(450.ipx, 950.ipx)
        val anchorPositionRtl = IntPxPosition(50.ipx, 950.ipx)
        val anchorSize = IntPxSize(10.ipx, 20.ipx)
        val inset = with(density) { MenuElevation.toIntPx() }
        val offsetX = 20
        val offsetY = 40
        val popupSize = IntPxSize(150.ipx, 80.ipx)

        val ltrPosition = DropdownMenuPositionProvider(
            Position(offsetX.dp, offsetY.dp),
            density,
            displayMetrics
        ).calculatePosition(
            anchorPosition,
            anchorSize,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(ltrPosition.x).isEqualTo(
            anchorPosition.x - popupSize.width + inset - offsetX.ipx
        )
        assertThat(ltrPosition.y).isEqualTo(
            anchorPosition.y - popupSize.height + inset - offsetY.ipx
        )

        val rtlPosition = DropdownMenuPositionProvider(
            Position(offsetX.dp, offsetY.dp),
            density,
            displayMetrics
        ).calculatePosition(
            anchorPositionRtl,
            anchorSize,
            LayoutDirection.Rtl,
            popupSize
        )

        assertThat(rtlPosition.x).isEqualTo(
            anchorPositionRtl.x + anchorSize.width - inset + offsetX.ipx
        )
        assertThat(rtlPosition.y).isEqualTo(
            anchorPositionRtl.y - popupSize.height + inset - offsetY.ipx
        )
    }

    @Test
    fun dropdownMenuItem_emphasis() {
        var onSurface = Color.Unset
        var enabledContentColor = Color.Unset
        var disabledContentColor = Color.Unset
        lateinit var enabledEmphasis: Emphasis
        lateinit var disabledEmphasis: Emphasis

        composeTestRule.setContent {
            onSurface = MaterialTheme.colors.onSurface
            enabledEmphasis = EmphasisAmbient.current.high
            disabledEmphasis = EmphasisAmbient.current.disabled
            DropdownMenu(
                toggle = { Box(Modifier.size(20.dp)) },
                onDismissRequest = {},
                expanded = true
            ) {
                DropdownMenuItem(onClick = {}) {
                    enabledContentColor = contentColor()
                }
                DropdownMenuItem(enabled = false, onClick = {}) {
                    disabledContentColor = contentColor()
                }
            }
        }

        assertThat(enabledContentColor).isEqualTo(enabledEmphasis.applyEmphasis(onSurface))
        assertThat(disabledContentColor).isEqualTo(disabledEmphasis.applyEmphasis(onSurface))
    }

    @Test
    fun dropdownMenuItem_onClick() {
        var clicked = false
        val onClick: () -> Unit = { clicked = true }

        composeTestRule.setContent {
            DropdownMenuItem(
                onClick,
                modifier = Modifier.testTag("MenuItem").clickable(onClick = onClick)
            ) {
                Box(Modifier.size(40.dp))
            }
        }

        findByTag("MenuItem").doClick()

        runOnIdleCompose {
            assertThat(clicked).isTrue()
        }
    }
}
