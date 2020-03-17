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

package androidx.ui.layout.test

import androidx.test.filters.SmallTest
import androidx.ui.core.TestTag
import androidx.ui.core.globalPosition
import androidx.ui.core.onPositioned
import androidx.ui.layout.LayoutAlign
import androidx.ui.layout.LayoutDirectionModifier
import androidx.ui.layout.LayoutOffset
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Stack
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.round
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class LayoutOffsetTest : LayoutTest() {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun positionIsModified() = with(density) {
        val offsetX = 10.dp
        val offsetY = 20.dp
        var positionX = 0.ipx
        var positionY = 0.ipx
        composeTestRule.setContent {
            TestTag("stack") {
                Stack(LayoutAlign.TopStart + LayoutOffset(offsetX, offsetY) +
                        onPositioned { coordinates ->
                            positionX = coordinates.globalPosition.x.round()
                            positionY = coordinates.globalPosition.y.round()
                        }
                ) {
                }
            }
        }

        findByTag("stack").assertExists()
        composeTestRule.runOnIdleCompose {
            assertEquals(offsetX.toIntPx(), positionX)
            assertEquals(offsetY.toIntPx(), positionY)
        }
    }

    @Test
    fun positionIsModified_rtl() = with(density) {
        val containerWidth = 30.dp
        val offsetX = 10.dp
        val offsetY = 20.dp
        var positionX = 0.ipx
        var positionY = 0.ipx
        composeTestRule.setContent {
            TestTag("stack") {
                Stack(
                    LayoutDirectionModifier.Rtl +
                            LayoutAlign.TopEnd +
                            LayoutWidth(containerWidth) +
                            LayoutAlign.TopStart +
                            LayoutOffset(offsetX, offsetY) +
                            onPositioned { coordinates ->
                                positionX = coordinates.globalPosition.x.round()
                                positionY = coordinates.globalPosition.y.round()
                            }
                ) {
                }
            }
        }

        findByTag("stack").assertExists()
        composeTestRule.runOnIdleCompose {
            assertEquals(containerWidth.toIntPx() - offsetX.toIntPx(), positionX)
            assertEquals(offsetY.toIntPx(), positionY)
        }
    }
}
