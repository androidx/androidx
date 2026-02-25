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

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RemoteTypographyTest {

    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)

    private val creationDisplayInfo =
        CreationDisplayInfo(
            500,
            500,
            ApplicationProvider.getApplicationContext<Context>().resources.displayMetrics.densityDpi,
        )

    @Test
    fun typography_uses_mono_font_family() {
        val monoTypography = RemoteTypography(defaultFontFamily = FontFamily.Monospace)

        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = Color.Black,
        ) {
            RemoteMaterialTheme(typography = monoTypography) {
                RemoteColumn {
                    RemoteText("Mono default".rs)
                    RemoteText(
                        "Mono bodyLarge".rs,
                        style = RemoteMaterialTheme.typography.bodyLarge,
                    )
                    RemoteText(
                        "Mono bodyMedium".rs,
                        style = RemoteMaterialTheme.typography.bodyMedium,
                    )
                    RemoteText(
                        "Mono bodySmall".rs,
                        style = RemoteMaterialTheme.typography.bodySmall,
                    )
                    RemoteText(
                        "Mono labelLarge".rs,
                        style = RemoteMaterialTheme.typography.labelLarge,
                    )
                    RemoteText(
                        "Mono labelMedium".rs,
                        style = RemoteMaterialTheme.typography.labelMedium,
                    )
                    RemoteText(
                        "Mono labelSmall".rs,
                        style = RemoteMaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }

    @Test
    fun typography_override_style() {
        val myTypography =
            RemoteTypography(bodyLarge = RemoteTextStyle(fontSize = 40.rsp, color = Color.Red.rc))

        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = Color.Black,
        ) {
            RemoteMaterialTheme(typography = myTypography) {
                // Should use the overridden bodyLarge which is Red and 40sp
                ProvideRemoteTextStyle(value = RemoteMaterialTheme.typography.bodyLarge) {
                    RemoteColumn {
                        RemoteText(
                            "bodyMedium".rs,
                            style = RemoteMaterialTheme.typography.bodyMedium,
                        )
                        RemoteText(
                            "red bodyLarge".rs,
                            style = RemoteMaterialTheme.typography.bodyLarge,
                        )
                        RemoteText("bodySmall".rs, style = RemoteMaterialTheme.typography.bodySmall)
                        RemoteText(
                            "labelLarge".rs,
                            style = RemoteMaterialTheme.typography.labelLarge,
                        )
                        RemoteText(
                            "labelMedium".rs,
                            style = RemoteMaterialTheme.typography.labelMedium,
                        )
                        RemoteText(
                            "labelSmall".rs,
                            style = RemoteMaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}
