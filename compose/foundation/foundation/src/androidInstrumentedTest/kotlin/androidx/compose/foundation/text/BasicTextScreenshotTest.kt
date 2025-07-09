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

import androidx.compose.foundation.GOLDEN_FOUNDATION
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class BasicTextScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_FOUNDATION)

    private val textTag = "text"

    @Test
    fun basicTextEllipsisCentered_leadingMarginCorrect_doesntMarch_b389707025() {
        val padding = mutableStateOf(0.dp)
        rule.setContent {
            Box(
                modifier =
                    Modifier.padding(top = padding.value).fillMaxSize().border(1.dp, Color.Blue),
                contentAlignment = Alignment.Center,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BasicText(
                        text = "I will definitely fill the screen!".repeat(10),
                        modifier = Modifier.border(1.dp, Color.Red).testTag(textTag),
                        style =
                            TextStyle(
                                textAlign = TextAlign.Center,
                                letterSpacing = 1.sp,
                                lineHeight = 24.sp,
                                lineHeightStyle =
                                    LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.None,
                                        mode = LineHeightStyle.Mode.Fixed,
                                    ),
                            ),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        color = { Color.Black },
                    )
                }
            }
        }

        repeat(2) {
            // these repaints cause the reproduction of b/389707025
            rule.waitForIdle()
            padding.value = padding.value + 1.dp
        }
        rule
            .onNodeWithTag(textTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "leadingMarginForEllipsis")
    }

    @Test
    fun multiStyleText_setFontWeight() {
        rule.setContent {
            BasicText(
                text =
                    buildAnnotatedString {
                        append("Hello ")
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append("World")
                        pop()
                    },
                modifier = Modifier.testTag(textTag),
                style =
                    TextStyle(
                        fontSize = 24.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Monospace,
                    ),
            )
        }
        rule
            .onNodeWithTag(textTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "multiStyleText_setFontWeight")
    }

    @Test
    fun multiStyleText_setFontFamily() {
        rule.setContent {
            BasicText(
                text =
                    buildAnnotatedString {
                        append("Hello ")
                        pushStyle(SpanStyle(fontFamily = FontFamily.SansSerif))
                        append("World")
                        pop()
                    },
                modifier = Modifier.testTag(textTag),
                style =
                    TextStyle(
                        fontSize = 24.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
            )
        }
        rule
            .onNodeWithTag(textTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "multiStyleText_setFontFamily")
    }

    @Test
    fun multiStyleText_setFontStyle() {
        rule.setContent {
            BasicText(
                text =
                    buildAnnotatedString {
                        append("Hello ")
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append("World")
                        pop()
                    },
                modifier = Modifier.testTag(textTag),
                style =
                    TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
            )
        }
        rule
            .onNodeWithTag(textTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "multiStyleText_setFontStyle")
    }
}
