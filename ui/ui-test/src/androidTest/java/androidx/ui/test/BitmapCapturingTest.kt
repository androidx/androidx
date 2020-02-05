/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test

import android.os.Build
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.Alignment
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.core.TestTag
import androidx.ui.unit.ipx
import androidx.ui.foundation.ColoredRect
import androidx.ui.foundation.shape.DrawShape
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.SolidColor
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.Row
import androidx.ui.semantics.Semantics
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class BitmapCapturingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val rootTag = "Root"
    private val tag11 = "Rect11"
    private val tag12 = "Rect12"
    private val tag21 = "Rect21"
    private val tag22 = "Rect22"

    private val color11 = Color.Red
    private val color12 = Color.Blue
    private val color21 = Color.Green
    private val color22 = Color.Yellow
    private val colorBg = Color.Black

    @Test
    fun captureIndividualRects_checkSizeAndColors() {
        composeCheckerboard()

        var calledCount = 0
        findByTag(tag11)
            .captureToBitmap()
            .assertPixels(expectedSize = IntPxSize(100.ipx, 50.ipx)) {
                calledCount++
                color11
            }
        assertThat(calledCount).isEqualTo(100 * 50)

        findByTag(tag12)
            .captureToBitmap()
            .assertPixels(expectedSize = IntPxSize(100.ipx, 50.ipx)) {
                color12
            }
        findByTag(tag21)
            .captureToBitmap()
            .assertPixels(expectedSize = IntPxSize(100.ipx, 50.ipx)) {
                color21
            }
        findByTag(tag22)
            .captureToBitmap()
            .assertPixels(expectedSize = IntPxSize(100.ipx, 50.ipx)) {
                color22
            }
    }

    @Test
    fun captureRootContainer_checkSizeAndColors() {
        composeCheckerboard()

        findByTag(rootTag)
            .captureToBitmap()
            .assertPixels(expectedSize = IntPxSize(200.ipx, 100.ipx)) {
                if (it.y >= 100.ipx || it.x >= 200.ipx) {
                    throw AssertionError("$it is out of range!")
                }
                expectedColorProvider(it)
            }
    }

    @Test
    fun captureWholeWindow_checkSizeAndColors() {
        composeCheckerboard()

        composeTestRule
            .captureScreenOnIdle()
            .assertPixels() {
                expectedColorProvider(it)
            }
    }

    @Test(expected = AssertionError::class)
    fun assertWrongColor_expectException() {
        composeCheckerboard()

        findByTag(tag11)
            .captureToBitmap()
            .assertPixels(expectedSize = IntPxSize(100.ipx, 50.ipx)) {
                color22 // Assuming wrong color
            }
    }

    @Test(expected = AssertionError::class)
    fun assertWrongSize_expectException() {
        composeCheckerboard()

        findByTag(tag11)
            .captureToBitmap()
            .assertPixels(expectedSize = IntPxSize(10.ipx, 10.ipx)) {
                color21
            }
    }

    private fun expectedColorProvider(pos: IntPxPosition): Color {
        if (pos.y < 50.ipx) {
            if (pos.x < 100.ipx) {
                return color11
            } else if (pos.x < 200.ipx) {
                return color12
            }
        } else if (pos.y < 100.ipx) {
            if (pos.x < 100.ipx) {
                return color21
            } else if (pos.x < 200.ipx) {
                return color22
            }
        }
        return colorBg
    }

    private fun composeCheckerboard() {
        with(composeTestRule.density) {
            composeTestRule.setContent {
                Container(alignment = Alignment.TopLeft) {

                    DrawShape(RectangleShape, SolidColor(colorBg))

                    TestTag(rootTag) {
                        Semantics(container = true) {
                            Column {
                                Row {
                                    TestTag(tag11) {
                                        Semantics(container = true) {
                                            ColoredRect(
                                                color = color11,
                                                width = 100.ipx.toDp(),
                                                height = 50.ipx.toDp()
                                            )
                                        }
                                    }
                                    TestTag(tag12) {
                                        Semantics(container = true) {
                                            ColoredRect(
                                                color = color12,
                                                width = 100.ipx.toDp(),
                                                height = 50.ipx.toDp()
                                            )
                                        }
                                    }
                                }
                                Row {
                                    TestTag(tag21) {
                                        Semantics(container = true) {
                                            ColoredRect(
                                                color = color21,
                                                width = 100.ipx.toDp(),
                                                height = 50.ipx.toDp()
                                            )
                                        }
                                    }
                                    TestTag(tag22) {
                                        Semantics(container = true) {
                                            ColoredRect(
                                                color = color22,
                                                width = 100.ipx.toDp(),
                                                height = 50.ipx.toDp()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}