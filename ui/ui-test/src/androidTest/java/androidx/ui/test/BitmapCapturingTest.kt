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
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.compose.foundation.Box
import androidx.compose.foundation.background
import androidx.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredSize
import androidx.ui.test.android.AndroidComposeTestRule
import androidx.ui.unit.IntOffset
import androidx.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
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
    val composeTestRule = AndroidComposeTestRule(
        ActivityScenarioRule(config.activityClass)
    )

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
        onNodeWithTag(tag11)
            .captureToBitmap()
            .assertPixels(expectedSize = IntSize(100, 50)) {
                calledCount++
                color11
            }
        assertThat(calledCount).isEqualTo(100 * 50)

        onNodeWithTag(tag12)
            .captureToBitmap()
            .assertPixels(expectedSize = IntSize(100, 50)) {
                color12
            }
        onNodeWithTag(tag21)
            .captureToBitmap()
            .assertPixels(expectedSize = IntSize(100, 50)) {
                color21
            }
        onNodeWithTag(tag22)
            .captureToBitmap()
            .assertPixels(expectedSize = IntSize(100, 50)) {
                color22
            }
    }

    @Test
    fun captureRootContainer_checkSizeAndColors() {
        composeCheckerboard()

        onNodeWithTag(rootTag)
            .captureToBitmap()
            .assertPixels(expectedSize = IntSize(200, 100)) {
                if (it.y >= 100 || it.x >= 200) {
                    throw AssertionError("$it is out of range!")
                }
                expectedColorProvider(it)
            }
    }

    @Test(expected = AssertionError::class)
    fun assertWrongColor_expectException() {
        composeCheckerboard()

        onNodeWithTag(tag11)
            .captureToBitmap()
            .assertPixels(expectedSize = IntSize(100, 50)) {
                color22 // Assuming wrong color
            }
    }

    @Test(expected = AssertionError::class)
    fun assertWrongSize_expectException() {
        composeCheckerboard()

        onNodeWithTag(tag11)
            .captureToBitmap()
            .assertPixels(expectedSize = IntSize(10, 10)) {
                color21
            }
    }

    private fun expectedColorProvider(pos: IntOffset): Color {
        if (pos.y < 50) {
            if (pos.x < 100) {
                return color11
            } else if (pos.x < 200) {
                return color12
            }
        } else if (pos.y < 100) {
            if (pos.x < 100) {
                return color21
            } else if (pos.x < 200) {
                return color22
            }
        }
        return colorBg
    }

    private fun composeCheckerboard() {
        with(composeTestRule.density) {
            composeTestRule.setContent {
                Box(Modifier.fillMaxSize(), backgroundColor = colorBg) {
                    Box(Modifier.padding(top = 20.toDp()), backgroundColor = colorBg) {
                        Column(Modifier.testTag(rootTag)) {
                            Row {
                                Box(
                                    Modifier
                                        .testTag(tag11)
                                        .preferredSize(100.toDp(), 50.toDp())
                                        .background(color = color11)
                                )
                                Box(Modifier
                                    .testTag(tag12)
                                    .preferredSize(100.toDp(), 50.toDp())
                                    .background(color12)
                                )
                            }
                            Row {
                                Box(Modifier
                                    .testTag(tag21)
                                    .preferredSize(100.toDp(), 50.toDp())
                                    .background(color21)
                                )
                                Box(Modifier
                                    .testTag(tag22)
                                    .preferredSize(100.toDp(), 50.toDp())
                                    .background(color22)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}