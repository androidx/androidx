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
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteColor
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontVariation.Setting
import androidx.compose.ui.text.font.FontVariation.Settings
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.wear.compose.remote.material3.util.TestProfiles
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
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    @Test
    fun text_withDefaultColor() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val text = "text_withDefaultColor".rs
            RemoteText(text, fontSize = 32.rsp)
        }
    }

    @Test
    fun text_withColorAndTextAlign_rtl() {
        remoteComposeTestRule.runScreenshotTestCustomProfile(
            layoutDirection = LayoutDirection.Rtl
        ) {
            val left = "LEFT".rs
            val center = "CENTER".rs
            val right = "RIGHT".rs
            val start = "START".rs
            val end = "END".rs
            val color = rememberNamedRemoteColor("TestColor5_rtl", Color.Green)

            RemoteColumn(RemoteModifier.fillMaxSize()) {
                RemoteText(
                    text = left,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.Left,
                )
                RemoteText(
                    text = center,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.Center,
                )
                RemoteText(
                    text = right,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.Right,
                )
                RemoteText(
                    text = start,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.Start,
                )
                RemoteText(
                    text = center,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.Center,
                )
                RemoteText(
                    text = end,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.End,
                )
            }
        }
    }

    @Test
    fun text_withStyle() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val text = "textWithStyle".rs
            RemoteText(
                text,
                style =
                    LocalRemoteTextStyle.current.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = 32.rsp,
                    ),
            )
        }
    }

    @Test
    fun text_withColor() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val text = "text_withColor".rs
            val color = rememberNamedRemoteColor("TestColor2", Color.Green)
            RemoteText(text, color = color, fontSize = 32.rsp)
        }
    }

    @Test
    fun text_withOverridingColor() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val text = "text_withOverridingColor".rs
            val color = rememberNamedRemoteColor("TestColor3", Color.Green)

            RemoteText(
                text,
                color = color, // text color should be green
                fontSize = 32.rsp,
                style =
                    LocalRemoteTextStyle.current.copy(
                        color = Color.Red.rc
                    ), // style color should be ignored
            )
        }
    }

    @Test
    fun text_withParamAndStyle_paramIsPreserved() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val text = "text_withParamAndStyle".rs
            val color = rememberNamedRemoteColor("TestColor4", Color.Green)

            RemoteText(
                text,
                color = color,
                fontStyle = FontStyle.Italic,
                style = LocalRemoteTextStyle.current.copy(fontSize = 32.rsp),
            )
        }
    }

    @Test
    fun text_withColorAndTextAlign() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val left = "LEFT".rs
            val center = "CENTER".rs
            val right = "RIGHT".rs
            val color = rememberNamedRemoteColor("TestColor5", Color.Green)

            RemoteColumn(RemoteModifier.fillMaxSize()) {
                RemoteText(
                    text = left,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.Left,
                )
                RemoteText(
                    text = center,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.Center,
                )
                RemoteText(
                    text = right,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.Right,
                )
            }
        }
    }

    @Test
    fun text_withWeight() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                VariantText(FontVariation.weight(100))
                VariantText(FontVariation.weight(500))
                VariantText(FontVariation.weight(900))
            }
        }
    }

    @Test
    fun text_withWidth() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                VariantText(FontVariation.width(10f))
                VariantText(FontVariation.width(50f))
                VariantText(FontVariation.width(90f))
            }
        }
    }

    @Test
    fun text_withGrade() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                VariantText(FontVariation.grade(0))
                VariantText(FontVariation.grade(100))
                VariantText(FontVariation.grade(200))
            }
        }
    }

    @Test
    @Ignore("No flex font in CI")
    fun text_withTnum() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                RemoteText(
                    text = RemoteString("WWWiii 012345679"),
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    fontVariationSettings = Settings(Setting("tnum", 1f)),
                )
                RemoteText(
                    text = RemoteString("WWWiii 012345679"),
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                )
            }
        }
    }

    @Test
    @Ignore("No flex font in CI")
    fun text_withRoundness() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                VariantText(Setting("ROND", 0f))
                VariantText(Setting("ROND", 50f))
                VariantText(Setting("ROND", 100f))
            }
        }
    }

    @Test
    fun text_withDecoration() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                RemoteText(
                    text = "None".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                )
                RemoteText(
                    text = "Underline".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    style =
                        LocalRemoteTextStyle.current.copy(textDecoration = TextDecoration.Underline),
                )
                RemoteText(
                    text = "LineThrough".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    style =
                        LocalRemoteTextStyle.current.copy(
                            textDecoration = TextDecoration.LineThrough
                        ),
                )
            }
        }
    }

    @Test
    fun text_withSpacing() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                RemoteText(
                    text = "Standard\nParagraph".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                )
                RemoteText(
                    text = "Double Line Height\nParagraph\nAnd one more".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    style = LocalRemoteTextStyle.current.copy(lineHeight = 64.rsp),
                )
                RemoteText(
                    text = "Letter Spacing\nParagraph".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    style = LocalRemoteTextStyle.current.copy(letterSpacing = 64.rsp),
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
            fontSize = 32.rsp,
            fontFamily = RemoteFontFamily.Named("RobotoFlex"),
            fontVariationSettings = Settings(setting),
        )
    }

    @Test
    fun longText_overflow() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val text =
                "a piece of writing in which the expression of feelings and ideas is given intensity by particular attention to diction (sometimes involving rhyme), rhythm, and imagery."
                    .rs
            val color = RemoteColor(Color.Green)

            RemoteColumn(RemoteModifier.fillMaxSize()) {
                // Default
                RemoteText(text = text, fontSize = 18.rsp, color = color)
                RemoteText(
                    text = text,
                    fontSize = 18.rsp,
                    color = color,
                    overflow = TextOverflow.Clip,
                    maxLines = 1,
                )
                RemoteText(
                    text = text,
                    fontSize = 18.rsp,
                    color = color,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                RemoteText(
                    text = text,
                    fontSize = 18.rsp,
                    color = color,
                    overflow = TextOverflow.Visible,
                    maxLines = 1,
                )
                RemoteText(
                    text = text,
                    fontSize = 18.rsp,
                    color = color,
                    overflow = TextOverflow.MiddleEllipsis,
                    maxLines = 1,
                )
                RemoteText(
                    text = text,
                    fontSize = 18.rsp,
                    color = color,
                    overflow = TextOverflow.StartEllipsis,
                    maxLines = 1,
                )
            }
        }
    }

    private fun RemoteScreenshotTestRule.runScreenshotTestCustomProfile(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        composable: @Composable @RemoteComposable () -> Unit,
    ) {
        this.runScreenshotTest(
            profile = TestProfiles.androidXWithCoreText,
            creationComposableWrapper = ComposableWrappers.layoutDirection(layoutDirection),
            playComposableWrapper = ComposableWrappers.blackBackground,
            composable = composable,
        )
    }
}
