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

import androidx.collection.buildObjectIntMap
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.remote.material3.util.TestImageVectors
import kotlin.test.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteMaterialThemeTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = "" // Not needed, this is not a screenshot test.
        )

    @Test
    fun sets_theme_color() {
        val expectedTint = ColorScheme().onSurface

        remoteComposeTestRule.runTest {
            RemoteMaterialTheme {
                val iconTint = RemoteMaterialTheme.colorScheme.onSurface
                RemoteIcon(TestImageVectors.VolumeUp, contentDescription = null, tint = iconTint)
            }
        }

        remoteComposeTestRule.assertRootNodeContainsColor(expectedTint)
    }

    @Test
    fun named_color_can_be_overridden() {
        val expectedTint = Color.Yellow

        val colorOverrides = buildObjectIntMap { put("WearM3.onSurface", Color.Yellow.toArgb()) }
        remoteComposeTestRule.runTest(colorOverrides = colorOverrides) {
            RemoteMaterialTheme {
                val iconTint = RemoteMaterialTheme.colorScheme.onSurface
                RemoteIcon(TestImageVectors.VolumeUp, contentDescription = null, tint = iconTint)
            }
        }

        remoteComposeTestRule.assertRootNodeContainsColor(expectedTint)
    }

    @Test
    @Ignore("Fails because of b/502878815")
    fun button_named_color_can_be_overridden() {
        val expectedTint = Color.Yellow

        val colorOverrides = buildObjectIntMap { put("WearM3.primary", Color.Yellow.toArgb()) }
        remoteComposeTestRule.runTest(colorOverrides = colorOverrides) {
            RemoteMaterialTheme {
                RemoteButton(onClick = Action.Empty) { RemoteText("button_enabled".rs) }
            }
        }

        remoteComposeTestRule.assertRootNodeContainsColor(expectedTint)
    }

    @Test
    fun theme_color_can_be_overridden_explicitly() {
        val expectedTint = Color.Yellow
        val remoteColorScheme =
            RemoteColorScheme(colorScheme = ColorScheme(onSurface = Color.Yellow))

        remoteComposeTestRule.runTest {
            RemoteMaterialTheme(colorScheme = remoteColorScheme) {
                RemoteIcon(
                    TestImageVectors.VolumeUp,
                    contentDescription = null,
                    tint = RemoteMaterialTheme.colorScheme.onSurface,
                )
            }
        }

        remoteComposeTestRule.assertRootNodeContainsColor(expectedTint)
    }

    @Test
    fun color_scheme_can_be_copied_and_overridden() {
        val expectedTint = Color.Yellow
        val remoteColorScheme = RemoteColorScheme().copy(onSurface = Color.Yellow.rc)

        remoteComposeTestRule.runTest {
            RemoteMaterialTheme(colorScheme = remoteColorScheme) {
                RemoteIcon(
                    TestImageVectors.VolumeUp,
                    contentDescription = null,
                    tint = RemoteMaterialTheme.colorScheme.onSurface,
                )
            }
        }

        remoteComposeTestRule.assertRootNodeContainsColor(expectedTint)
    }

    @Test
    fun custom_color_scheme_propagates_colors() {
        val customColorScheme = ColorScheme(primary = Color.Magenta, onSurface = Color.Cyan)
        val remoteColorScheme = RemoteColorScheme(customColorScheme)
        val expectedTint = Color.Cyan

        remoteComposeTestRule.runTest {
            RemoteMaterialTheme(colorScheme = remoteColorScheme) {
                val iconTint = RemoteMaterialTheme.colorScheme.onSurface
                RemoteIcon(TestImageVectors.VolumeUp, contentDescription = null, tint = iconTint)
            }
        }

        remoteComposeTestRule.assertRootNodeContainsColor(expectedTint)
    }
}
