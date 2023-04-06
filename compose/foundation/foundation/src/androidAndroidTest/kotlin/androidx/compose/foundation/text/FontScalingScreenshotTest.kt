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

package androidx.compose.foundation.text

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.GOLDEN_UI
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.testutils.AndroidFontScaleHelper
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
class FontScalingScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_UI)

    private val containerTag = "container"

    @After
    fun teardown() {
        AndroidFontScaleHelper.resetSystemFontScale(rule.activityRule.scenario)
    }

    @Test
    fun fontScaling1x_lineHeightDoubleSp() {
        AndroidFontScaleHelper.setSystemFontScale(1f, rule.activityRule.scenario)
        rule.waitForIdle()

        rule.setContent {
            TestLayout(lineHeight = 28.sp)
        }
        rule.onNodeWithTag(containerTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fontScaling1x_lineHeightDoubleSp")
    }

    @Test
    fun fontScaling2x_lineHeightDoubleSp() {
        AndroidFontScaleHelper.setSystemFontScale(2f, rule.activityRule.scenario)
        rule.waitForIdle()

        rule.setContent {
            TestLayout(lineHeight = 28.sp)
        }
        rule.onNodeWithTag(containerTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fontScaling2x_lineHeightDoubleSp")
    }

    @Test
    fun fontScaling1x_lineHeightDoubleEm() {
        AndroidFontScaleHelper.setSystemFontScale(1f, rule.activityRule.scenario)
        rule.waitForIdle()

        rule.setContent {
            TestLayout(lineHeight = 2.em)
        }
        rule.onNodeWithTag(containerTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fontScaling1x_lineHeightDoubleEm")
    }

    @Test
    fun fontScaling2x_lineHeightDoubleEm() {
        AndroidFontScaleHelper.setSystemFontScale(2f, rule.activityRule.scenario)
        rule.waitForIdle()

        rule.setContent {
            TestLayout(lineHeight = 2.em)
        }
        rule.onNodeWithTag(containerTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fontScaling2x_lineHeightDoubleEm")
    }

    @Test
    fun fontScaling1x_drawText() {
        AndroidFontScaleHelper.setSystemFontScale(2f, rule.activityRule.scenario)
        rule.waitForIdle()

        rule.setContent {
            TestDrawTextLayout()
        }
        rule.onNodeWithTag(containerTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fontScaling1x_drawText")
    }

    @Test
    fun fontScaling2x_drawText() {
        AndroidFontScaleHelper.setSystemFontScale(2f, rule.activityRule.scenario)
        rule.waitForIdle()

        rule.setContent {
            TestDrawTextLayout()
        }
        rule.onNodeWithTag(containerTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fontScaling2x_drawText")
    }

    @Composable
    private fun TestLayout(lineHeight: TextUnit) {
        Column(
            modifier = Modifier.testTag(containerTag),
        ) {
            BasicText(
                text = buildAnnotatedString {
                    append("Hello ")
                    pushStyle(SpanStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    ))
                    append("Accessibility")
                    pop()
                },
                style = TextStyle(
                    fontSize = 36.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Monospace
                )
            )
            BasicText(
                text = "Here's a subtitle",
                style = TextStyle(
                    fontSize = 20.sp
                )
            )
            BasicText(
                text = sampleText,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = lineHeight
                )
            )
        }
    }

    @Composable
    private fun TestDrawTextLayout() {
        val textMeasurer = rememberTextMeasurer()

        Column(
            modifier = Modifier.testTag(containerTag),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                 drawText(
                     textMeasurer = textMeasurer,
                     style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 28.sp
                     ),
                     text = sampleText
                )
            }
        }
    }

    companion object {
        private val sampleText = """
Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore
et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut
aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse
cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in
culpa qui officia deserunt mollit anim id est laborum.

Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium,
totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae
dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit,
sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro
quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non
numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim
ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid
ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse
quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?
    """.trimIndent()
    }
}