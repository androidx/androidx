/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rememberRemoteColor
import androidx.compose.remote.creation.compose.state.rememberRemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontVariation.Setting
import androidx.compose.ui.text.font.FontVariation.Settings
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.material3.LocalTextStyle
import java.text.DecimalFormat
import kotlin.test.Ignore
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteTextTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            profile = TestProfiles.androidXWithCoreText,
        )

    @Test
    fun text_withDefaultColor() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            val text = rememberRemoteString { "text_withDefaultColor" }
            RemoteText(text, fontSize = 32.sp)
        }
    }

    @Test
    fun text_withStyle() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            val text = rememberRemoteString { "textWithStyle" }
            RemoteText(
                text,
                style = LocalTextStyle.current.copy(fontStyle = FontStyle.Italic, fontSize = 32.sp),
            )
        }
    }

    @Test
    fun text_withColor() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            val text = rememberRemoteString { "text_withColor" }
            val color = rememberRemoteColor("TestColor2") { Color.Green }
            RemoteText(text, color = color, fontSize = 32.sp)
        }
    }

    @Test
    fun text_withOverridingColor() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            val text = rememberRemoteString { "text_withOverridingColor" }
            val color = rememberRemoteColor("TestColor3") { Color.Green }

            RemoteText(
                text,
                color = color, // text color should be green
                fontSize = 32.sp,
                style =
                    LocalTextStyle.current.copy(color = Color.Red), // style color should be ignored
            )
        }
    }

    @Test
    fun text_withParamAndStyle_paramIsPreserved() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            val text = rememberRemoteString { "text_withParamAndStyle" }
            val color = rememberRemoteColor("TestColor4") { Color.Green }

            RemoteText(
                text,
                color = color,
                fontStyle = FontStyle.Italic,
                style = LocalTextStyle.current.copy(fontSize = 32.sp),
            )
        }
    }

    @Test
    fun text_withColorAndTextAlign() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            val left = rememberRemoteString { "LEFT" }
            val center = rememberRemoteString { "CENTER" }
            val right = rememberRemoteString { "RIGHT" }
            val color = rememberRemoteColor("TestColor5") { Color.Green }

            RemoteColumn(RemoteModifier.fillMaxSize()) {
                RemoteText(
                    text = left,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.sp,
                    color = color,
                    textAlign = TextAlign.Left,
                )
                RemoteText(
                    text = center,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.sp,
                    color = color,
                    textAlign = TextAlign.Center,
                )
                RemoteText(
                    text = right,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.sp,
                    color = color,
                    textAlign = TextAlign.Right,
                )
            }
        }
    }

    @Test
    @Ignore("No flex font in CI")
    fun text_withWeight() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                VariantText(FontVariation.weight(100))
                VariantText(FontVariation.weight(500))
                VariantText(FontVariation.weight(900))
            }
        }
    }

    @Test
    @Ignore("No flex font in CI")
    fun text_withWidth() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                VariantText(FontVariation.width(10f))
                VariantText(FontVariation.width(50f))
                VariantText(FontVariation.width(90f))
            }
        }
    }

    @Test
    @Ignore("No flex font in CI")
    fun text_withTnum() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                RemoteText(
                    text = RemoteString("WWWiii 012345679"),
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.sp,
                    fontVariationSettings = Settings(Setting("tnum", 1f)),
                )
                RemoteText(
                    text = RemoteString("WWWiii 012345679"),
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.sp,
                )
            }
        }
    }

    @Test
    @Ignore("No flex font in CI")
    fun text_withRoundness() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                VariantText(Setting("ROND", 0f))
                VariantText(Setting("ROND", 50f))
                VariantText(Setting("ROND", 100f))
            }
        }
    }

    @Test
    fun text_withDecoration() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                RemoteText(
                    text = "None".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.sp,
                )
                RemoteText(
                    text = "Underline".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.sp,
                    style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
                )
                RemoteText(
                    text = "LineThrough".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.sp,
                    style = LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough),
                )
            }
        }
    }

    @Test
    fun text_withSpacing() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                RemoteText(
                    text = RemoteString("Standard\nParagraph"),
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.sp,
                )
                RemoteText(
                    text = RemoteString("Double Line Height\nParagraph\nAnd one more"),
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.sp,
                    style = LocalTextStyle.current.copy(lineHeight = 64.sp),
                )
                RemoteText(
                    text = RemoteString("Letter Spacing\nParagraph"),
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.sp,
                    style = LocalTextStyle.current.copy(letterSpacing = 64.sp),
                )
            }
        }
    }

    @Composable
    private fun VariantText(setting: Setting) {
        RemoteText(
            text =
                RemoteString(setting.axisName) +
                    RemoteString(" = ") +
                    setting.toVariationValue(null).rf.toRemoteString(DecimalFormat("0")),
            modifier = RemoteModifier.fillMaxWidth(),
            fontSize = 32.sp,
            fontVariationSettings = Settings(setting),
        )
    }

    @Test
    fun longText_overflow() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            val text = rememberRemoteString {
                "a piece of writing in which the expression of feelings and ideas is given intensity by particular attention to diction (sometimes involving rhyme), rhythm, and imagery."
            }
            val color = RemoteColor(Color.Green)

            RemoteColumn(RemoteModifier.fillMaxSize()) {
                // Default
                RemoteText(text = text, fontSize = 18.sp, color = color)
                RemoteText(
                    text = text,
                    fontSize = 18.sp,
                    color = color,
                    overflow = TextOverflow.Clip,
                    maxLines = 1,
                )
                RemoteText(
                    text = text,
                    fontSize = 18.sp,
                    color = color,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                RemoteText(
                    text = text,
                    fontSize = 18.sp,
                    color = color,
                    overflow = TextOverflow.Visible,
                    maxLines = 1,
                )
                RemoteText(
                    text = text,
                    fontSize = 18.sp,
                    color = color,
                    overflow = TextOverflow.MiddleEllipsis,
                    maxLines = 1,
                )
                RemoteText(
                    text = text,
                    fontSize = 18.sp,
                    color = color,
                    overflow = TextOverflow.StartEllipsis,
                    maxLines = 1,
                )
            }
        }
    }
}
