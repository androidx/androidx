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

import androidx.activity.ComponentActivity
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.foundation.Box
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSize
import androidx.ui.test.android.AndroidComposeTestRule
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.ipx
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class BitmapCapturingTest(val config: TestConfig) {
    data class TestConfig(
        val activityClass: Class<out ComponentActivity>
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> = listOf(
            TestConfig(ComponentActivity::class.java),
            TestConfig(ActivityWithActionBar::class.java)
        )
    }

    @get:Rule
    val composeTestRule = AndroidComposeTestRule(ActivityTestRule(config.activityClass))

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
                Box(Modifier.fillMaxSize(), backgroundColor = colorBg) {
                    Box(Modifier.padding(top = 20.ipx.toDp()), backgroundColor = colorBg) {
                        Column(Modifier.testTag(rootTag)) {
                            Row {
                                Box(Modifier
                                    .testTag(tag11)
                                    .preferredSize(100.ipx.toDp(), 50.ipx.toDp())
                                    .drawBackground(color11)
                                )
                                Box(Modifier
                                    .testTag(tag12)
                                    .preferredSize(100.ipx.toDp(), 50.ipx.toDp())
                                    .drawBackground(color12)
                                )
                            }
                            Row {
                                Box(Modifier
                                    .testTag(tag21)
                                    .preferredSize(100.ipx.toDp(), 50.ipx.toDp())
                                    .drawBackground(color21)
                                )
                                Box(Modifier
                                    .testTag(tag22)
                                    .preferredSize(100.ipx.toDp(), 50.ipx.toDp())
                                    .drawBackground(color22)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}