/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.wear.compose.foundation

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class CurvedScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    @Test
    fun curved_row_and_column() = verify_screenshot {
        curvedColumn(CurvedModifier.background(Color.Red)) {
            basicCurvedText("A")
            basicCurvedText("B")
            basicCurvedText("C")
        }
        basicCurvedText("D", CurvedModifier.background(Color.Green))
        curvedColumn(CurvedModifier.background(Color.Blue)) {
            basicCurvedText("E")
            curvedRow {
                basicCurvedText("F")
                basicCurvedText("G")
                basicCurvedText("H")
            }
            basicCurvedText("I")
        }
    }

    @Test
    fun curved_backgrounds() = verify_screenshot {
        val size = 0.15f
        val rgb = listOf(Color.Red, Color.Green, Color.Blue)
        curvedColumn(
            CurvedModifier.size(sweepDegrees = 20f, thickness = 30.dp)
                .radialGradientBackground(rgb)
        ) { }
        curvedColumn(
            CurvedModifier.size(sweepDegrees = 20f, thickness = 30.dp)
                .radialGradientBackground(
                    (0.5f - size) to Color.Red,
                    0.5f to Color.Blue,
                    (0.5f + size) to Color.Red,
                )
        ) { }
        curvedColumn(
            CurvedModifier.size(sweepDegrees = 20f, thickness = 30.dp)
                .angularGradientBackground(rgb)
        ) { }
        curvedColumn(
            CurvedModifier.size(sweepDegrees = 20f, thickness = 30.dp)
                .angularGradientBackground(
                    (0.5f - size) to Color.Red,
                    0.5f to Color.Blue,
                    (0.5f + size) to Color.Red,
                )
        ) { }
    }

    @Test
    fun curved_padding() = verify_screenshot {
        basicCurvedText(
            "Text",
            CurvedModifier.background(Color.Red).padding(3.dp)
                .background(Color.Green).padding(angular = 5.dp)
                .background(Color.Blue).padding(radial = 4.dp)
        )
    }

    @Test
    fun curved_plus_composables() = verify_screenshot {
        curvedRow(CurvedModifier.background(Color.Green)) {
            curvedComposable {
                Column {
                    Box(Modifier.size(15.dp).background(Color.Red))
                    @Suppress("DEPRECATION")
                    BasicText(
                        text = "Text",
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = true)
                        )
                    )
                    Box(Modifier.size(15.dp).background(Color.Red))
                }
            }
            curvedComposable {
                Box(Modifier.size(15.dp).background(Color.Blue))
            }
        }
    }

    @Test
    fun curved_alignment() = verify_screenshot {
        listOf(
            CurvedAlignment.Angular.Start,
            CurvedAlignment.Angular.Center,
            CurvedAlignment.Angular.End
        ).forEachIndexed { ix, align ->
            curvedColumn(
                CurvedModifier.angularSize(45f)
                    .angularGradientBackground(listOf(Color.Red, Color.Green)),
                angularAlignment = align
            ) {
                curvedComposable { Box(Modifier.size(15.dp).background(Color.Blue)) }
                basicCurvedText(listOf("Start", "Center", "End")[ix])
            }
        }
        curvedColumn(
            CurvedModifier.angularSize(45f)
        ) {
            listOf(
                CurvedAlignment.Radial.Inner,
                CurvedAlignment.Radial.Center,
                CurvedAlignment.Radial.Outer,
            ).forEachIndexed { ix, align ->
                curvedRow(
                    CurvedModifier.size(45f, 20.dp)
                        .radialGradientBackground(listOf(Color.Red, Color.Green)),
                    radialAlignment = align
                ) {
                    curvedComposable { Box(Modifier.size(10.dp).background(Color.Blue)) }
                    basicCurvedText(
                        listOf("Inner", "Center", "Outer")[ix],
                        style = CurvedTextStyle(fontSize = 10.sp)
                    )
                }
            }
        }
    }

    @Test
    public fun layout_direction_rtl() = layout_direction(LayoutDirection.Rtl)

    @Test
    public fun layout_direction_ltr() = layout_direction(LayoutDirection.Ltr)

    private fun layout_direction(layoutDirection: LayoutDirection) = verify_composable_screenshot {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            CurvedLayout(
                Modifier.fillMaxSize(),
                angularDirection = CurvedDirection.Angular.Clockwise
            ) {
                curvedRow(
                    CurvedModifier.background(Color.Green),
                    angularDirection = CurvedDirection.Angular.Normal
                ) {
                    layout_direction_block()
                }
                curvedComposable { Spacer(Modifier.size(10.dp)) }
                curvedRow(
                    CurvedModifier.background(Color.Red),
                    angularDirection = CurvedDirection.Angular.Clockwise
                ) {
                    layout_direction_block()
                }
            }

            CurvedLayout(
                Modifier.fillMaxSize(),
                anchor = 90f,
                angularDirection = CurvedDirection.Angular.CounterClockwise
            ) {
                curvedRow(
                    CurvedModifier.background(Color.Green),
                    angularDirection = CurvedDirection.Angular.Reversed
                ) {
                    layout_direction_block()
                }
                curvedComposable { Spacer(Modifier.size(10.dp)) }
                curvedRow(
                    CurvedModifier.background(Color.Red),
                    angularDirection = CurvedDirection.Angular.CounterClockwise
                ) {
                    layout_direction_block()
                }
            }
        }
    }

    private fun CurvedScope.layout_direction_block() {
        basicCurvedText("A")
        curvedColumn {
            basicCurvedText("B")
            basicCurvedText("C")
        }
        curvedColumn(radialDirection = CurvedDirection.Radial.OutsideIn) {
            basicCurvedText("D")
            basicCurvedText("E")
        }
        curvedColumn(radialDirection = CurvedDirection.Radial.InsideOut) {
            basicCurvedText("F")
            basicCurvedText("G")
        }
        basicCurvedText("H")
    }

    private fun verify_screenshot(contentBuilder: CurvedScope.() -> Unit) =
        verify_composable_screenshot(content = {
            CurvedLayout(
                modifier = Modifier.fillMaxSize(),
                contentBuilder = contentBuilder
            )
        })

    private fun verify_composable_screenshot(content: @Composable BoxScope.() -> Unit) {
        rule.setContent {
            Box(
                modifier = Modifier.size(200.dp).background(Color.White).testTag(TEST_TAG),
                content = content
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}

internal const val SCREENSHOT_GOLDEN_PATH = "wear/compose/foundation"
