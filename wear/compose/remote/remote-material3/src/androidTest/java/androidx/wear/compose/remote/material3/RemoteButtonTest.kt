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

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.player.compose.test.utils.screenshot.TargetPlayer
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@SuppressLint("UnrememberedMutableState")
@RunWith(JUnit4::class)
class RemoteButtonTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = TargetPlayer.View,
        )
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val size = Size(500f, 500f)

    @Test
    fun button_enabled() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black, size = size) {
            RemoteButton(enabled = RemoteBoolean(true)) {
                RemoteText(
                    RemoteString("button_enabled"),
                    color = RemoteButtonDefaults.buttonColors().contentColor(),
                )
            }
        }
    }

    @Test
    fun button_disabled() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black, size = size) {
            RemoteButton(enabled = RemoteBoolean(false)) {
                RemoteText(
                    RemoteString("button_disabled"),
                    color =
                        RemoteButtonDefaults.buttonColors()
                            .contentColor(enabled = RemoteBoolean(false)),
                )
            }
        }
    }

    @Test
    fun button_overrides_colors() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black, size = size) {
            val colors =
                RemoteButtonColors(
                    containerColor = RemoteColor(Color.Yellow),
                    contentColor = RemoteColor(Color.Cyan),
                    secondaryContentColor = RemoteColor(Color.Black),
                    iconColor = RemoteColor(Color.Black),
                    disabledContainerColor = RemoteColor(Color.Black),
                    disabledContentColor = RemoteColor(Color.Black),
                    disabledSecondaryContentColor = RemoteColor(Color.Black),
                    disabledIconColor = RemoteColor(Color.Black),
                )
            RemoteButton(contentPadding = RemotePaddingValues(40.rdp), colors = colors) {
                RemoteText(RemoteString("button_overrides_colors"), color = colors.contentColor())
            }
        }
    }

    @Test
    fun button_overrides_padding() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black, size = size) {
            RemoteButton(contentPadding = RemotePaddingValues(150.rdp)) {
                RemoteText(
                    RemoteString("button_overrides_padding"),
                    color = RemoteButtonDefaults.buttonColors().contentColor(),
                )
            }
        }
    }

    @Test
    fun button_overrides_size() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black, size = size) {
            RemoteButton(
                modifier = RemoteModifier.size(180.rdp, 100.rdp),
                contentPadding = RemotePaddingValues(0.rdp),
            ) {
                RemoteText(
                    RemoteString("button_overrides_size"),
                    color = RemoteButtonDefaults.buttonColors().contentColor(),
                )
            }
        }
    }

    @Test
    fun button_with_border() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black, size = size) {
            RemoteButton(
                modifier = RemoteModifier,
                border = 8.rdp,
                borderColor = RemoteColor(Color.Green),
            ) {
                RemoteText(
                    RemoteString("button_with_border"),
                    color = RemoteButtonDefaults.buttonColors().contentColor(),
                )
            }
        }
    }

    @Test
    fun button_enabled_click_modifier_is_added() {
        val expectedContent =
            """
DATA_TEXT<42> = "button_enabled"
ROOT [-2:-1] = [0.0, 0.0, 0.0, 0.0] VISIBLE
  ROW [-3:-1] = [0.0, 0.0, 28.0, 12.0] VISIBLE
    MODIFIERS
      HEIGHT_IN = [52.0, 3.4028235E38]
      WIDTH_IN = [12.0, 3.4028235E38]
      DRAW_CONTENT
      CLICK_MODIFIER
      SEMANTICS = SEMANTICS BUTTON
      PADDING = [14.0, 6.0, 14.0, 6.0]
    TEXT_LAYOUT [-5:-1] = [0.0, 0.0, 0.0, 0.0] VISIBLE (42:"null")
      MODIFIERS"""
                .trimIndent()
        runBlocking {
            val document =
                remoteComposeTestRule.captureDocument(context = context) {
                    RemoteButton(enabled = RemoteBoolean(true)) {
                        RemoteText(
                            RemoteString("button_enabled"),
                            color = RemoteButtonDefaults.buttonColors().contentColor(),
                        )
                    }
                }
            val actualContent = document.displayHierarchy()

            assertEquals(expectedContent.normalizeWhiteSpace(), actualContent.normalizeWhiteSpace())
        }
    }

    @Test
    fun button_disabled_click_modifier_is_not_added() {
        val expectedContent =
            """
DATA_TEXT<42> = "button_disabled"
ROOT [-2:-1] = [0.0, 0.0, 0.0, 0.0] VISIBLE
  ROW [-3:-1] = [0.0, 0.0, 28.0, 12.0] VISIBLE
    MODIFIERS
      HEIGHT_IN = [52.0, 3.4028235E38]
      WIDTH_IN = [12.0, 3.4028235E38]
      DRAW_CONTENT
      SEMANTICS = SEMANTICS BUTTON disabled
      PADDING = [14.0, 6.0, 14.0, 6.0]
    TEXT_LAYOUT [-5:-1] = [0.0, 0.0, 0.0, 0.0] VISIBLE (42:"null")
      MODIFIERS"""
                .trimIndent()

        runBlocking {
            val document =
                remoteComposeTestRule.captureDocument(context = context) {
                    RemoteButton(enabled = RemoteBoolean(false)) {
                        RemoteText(
                            RemoteString("button_disabled"),
                            color = RemoteButtonDefaults.buttonColors().contentColor(),
                        )
                    }
                }
            val actualContent = document.displayHierarchy()

            assertEquals(expectedContent.normalizeWhiteSpace(), actualContent.normalizeWhiteSpace())
        }
    }

    // Replace all sequences of whitespace (including newlines, tabs) with a single space. Then
    // trim leading/trailing spaces from the whole string
    private fun String.normalizeWhiteSpace() = this.replace(Regex("``s+"), " ").trim()
}
