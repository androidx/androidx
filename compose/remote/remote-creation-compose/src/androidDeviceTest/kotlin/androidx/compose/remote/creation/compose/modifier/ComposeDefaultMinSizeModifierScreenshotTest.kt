/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot tests for Jetpack Compose [Modifier.defaultMinSize], testing the exact same 11
 * permutations as [DefaultMinSizeModifierScreenshotTest] to enable visual side-by-side comparison
 * between Remote Compose and Jetpack Compose sizing behavior.
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class ComposeDefaultMinSizeModifierScreenshotTest {
    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun grid() {
        val tests =
            listOf<Pair<String, @Composable () -> Unit>>(
                // 1. Content sizing vs default minimum (unconstrained outer container)
                "child 20x20 < min 50\n-> clamps to 50x50" to
                    {
                        Box(
                            modifier =
                                Modifier.defaultMinSize(minWidth = 50.dp, minHeight = 50.dp)
                                    .background(Color.Red)
                        ) {
                            Box(modifier = Modifier.size(20.dp).background(Color.Blue))
                        }
                    },
                "child 80x80 > min 50\n-> wraps to 80x80" to
                    {
                        Box(
                            modifier =
                                Modifier.defaultMinSize(minWidth = 50.dp, minHeight = 50.dp)
                                    .background(Color.Red)
                        ) {
                            Box(modifier = Modifier.size(80.dp).background(Color.Blue))
                        }
                    },

                // 2. Explicit size modifier BEFORE defaultMinSize (creation time override)
                "size(80) before min(50)\n-> explicit 80x80 wins" to
                    {
                        Box(
                            modifier =
                                Modifier.size(80.dp)
                                    .defaultMinSize(minWidth = 50.dp, minHeight = 50.dp)
                                    .background(Color.Red)
                        ) {
                            Box(modifier = Modifier.size(20.dp).background(Color.Blue))
                        }
                    },
                "size(20) before min(50)\n-> explicit 20x20 wins" to
                    {
                        Box(
                            modifier =
                                Modifier.size(20.dp)
                                    .defaultMinSize(minWidth = 50.dp, minHeight = 50.dp)
                                    .background(Color.Red)
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(Color.Blue))
                        }
                    },
                "width(20) before min(50)\n-> 20x50 (width fixed)" to
                    {
                        Box(
                            modifier =
                                Modifier.width(20.dp)
                                    .defaultMinSize(minWidth = 50.dp, minHeight = 50.dp)
                                    .background(Color.Red)
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(Color.Blue))
                        }
                    },
                "height(20) before min(50)\n-> 50x20 (height fixed)" to
                    {
                        Box(
                            modifier =
                                Modifier.height(20.dp)
                                    .defaultMinSize(minWidth = 50.dp, minHeight = 50.dp)
                                    .background(Color.Red)
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(Color.Blue))
                        }
                    },

                // 3. Explicit size modifier AFTER defaultMinSize (player time interaction)
                "min(50) then size(80)\n-> player renders 80x80" to
                    {
                        Box(
                            modifier =
                                Modifier.defaultMinSize(minWidth = 50.dp, minHeight = 50.dp)
                                    .size(80.dp)
                                    .background(Color.Red)
                        ) {
                            Box(modifier = Modifier.size(20.dp).background(Color.Blue))
                        }
                    },
                "min(50) then size(20)\n-> player clamps 50x50" to
                    {
                        Box(
                            modifier =
                                Modifier.defaultMinSize(minWidth = 50.dp, minHeight = 50.dp)
                                    .size(20.dp)
                                    .background(Color.Red)
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(Color.Blue))
                        }
                    },

                // 4. Stacked / multiple defaultMinSize modifiers
                "min(40) then min(70)\n-> 1st modifier (40) wins" to
                    {
                        Box(
                            modifier =
                                Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                                    .defaultMinSize(minWidth = 70.dp, minHeight = 70.dp)
                                    .background(Color.Red)
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(Color.Blue))
                        }
                    },
                "min(40) pad min(70)\n-> 40x40 (1st min wins)" to
                    {
                        Box(
                            modifier =
                                Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                                    .background(Color.Gray)
                                    .padding(10.dp)
                                    .defaultMinSize(minWidth = 70.dp, minHeight = 70.dp)
                                    .background(Color.Red)
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(Color.Blue))
                        }
                    },

                // 5. Independent single-axis minimum constraints
                "minWidth(50) only\n-> 50x20 (wraps height)" to
                    {
                        Box(
                            modifier =
                                Modifier.defaultMinSize(minWidth = 50.dp).background(Color.Red)
                        ) {
                            Box(modifier = Modifier.size(20.dp).background(Color.Blue))
                        }
                    },
                "minHeight(50) only\n-> 20x50 (wraps width)" to
                    {
                        Box(
                            modifier =
                                Modifier.defaultMinSize(minHeight = 50.dp).background(Color.Red)
                        ) {
                            Box(modifier = Modifier.size(20.dp).background(Color.Blue))
                        }
                    },
            )

        composeTestRule.setContent {
            Box(modifier = Modifier.testTag(TEST_TAG)) { ComposeGridContent(tests) }
        }

        composeTestRule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "ComposeDefaultMinSizeModifierScreenshotTest_grid",
                MSSIMMatcher(threshold = 0.999),
            )
    }

    @Composable
    private fun ComposeGridContent(
        innerContentList: List<Pair<String, @Composable () -> Unit>>,
        itemsPerRow: Int = 3,
        padding: Dp = 24.dp,
        containerSize: Dp = 100.dp,
    ) {
        val chunkedContents = innerContentList.chunked(itemsPerRow)
        Column {
            for (row in chunkedContents) {
                Row {
                    for ((label, content) in row) {
                        Column(modifier = Modifier.width(containerSize)) {
                            BasicText(
                                text = label,
                                modifier = Modifier.width(containerSize).height(20.dp),
                                style = TextStyle(fontSize = 8.sp),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2,
                            )
                            Box(
                                modifier =
                                    Modifier.size(containerSize).background(Color(0xFFCFD8DC)),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                content()
                            }
                        }
                        Box(modifier = Modifier.width(padding))
                    }
                }
                Box(modifier = Modifier.height(padding))
            }
        }
    }

    companion object {
        const val TEST_TAG = "ComposeGridRoot"
    }
}
