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

@file:OptIn(ExperimentalRemoteComposePlayerApi::class)

package androidx.compose.remote.player.compose.creation.compose.layout

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.player.compose.ExperimentalRemoteComposePlayerApi
import androidx.compose.remote.player.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.player.compose.test.utils.screenshot.TargetPlayer
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteRowComposeTest {
    @get:Rule
    val composeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = TargetPlayer.Compose,
        )

    @Test
    fun simpleLayout() {
        composeTestRule.simpleLayout()
    }
}

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteRowViewTest {
    @get:Rule
    val composeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = TargetPlayer.View,
        )

    @Test
    fun simpleLayout() {
        composeTestRule.simpleLayout()
    }
}

private fun RemoteComposeScreenshotTestRule.simpleLayout() = runScreenshotTest {
    RemoteRow {
        RemoteColumn {
            Container {
                // TODO(b/447100988): replace size by fillMaxSize in all those RemoteRows
                RemoteRow(modifier = RemoteModifier.size(ContainerSize)) { Content() }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteRow(
                    modifier = RemoteModifier.size(ContainerSize),
                    verticalAlignment = RemoteAlignment.CenterVertically,
                ) {
                    Content()
                }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteRow(
                    modifier = RemoteModifier.size(ContainerSize),
                    verticalAlignment = RemoteAlignment.Bottom,
                ) {
                    Content()
                }
            }
        }
        RemoteBox(modifier = RemoteModifier.width(Padding))
        RemoteColumn {
            Container {
                RemoteRow(
                    modifier = RemoteModifier.size(ContainerSize),
                    horizontalArrangement = RemoteArrangement.CenterHorizontally,
                ) {
                    Content()
                }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteRow(
                    modifier = RemoteModifier.size(ContainerSize),
                    horizontalArrangement = RemoteArrangement.CenterHorizontally,
                    verticalAlignment = RemoteAlignment.CenterVertically,
                ) {
                    Content()
                }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteRow(
                    modifier = RemoteModifier.size(ContainerSize),
                    horizontalArrangement = RemoteArrangement.CenterHorizontally,
                    verticalAlignment = RemoteAlignment.Bottom,
                ) {
                    Content()
                }
            }
        }
        RemoteBox(modifier = RemoteModifier.width(Padding))
        RemoteColumn {
            Container {
                RemoteRow(
                    modifier = RemoteModifier.size(ContainerSize),
                    horizontalArrangement = RemoteArrangement.End,
                ) {
                    Content()
                }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteRow(
                    modifier = RemoteModifier.size(ContainerSize),
                    horizontalArrangement = RemoteArrangement.End,
                    verticalAlignment = RemoteAlignment.CenterVertically,
                ) {
                    Content()
                }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteRow(
                    modifier = RemoteModifier.size(ContainerSize),
                    horizontalArrangement = RemoteArrangement.End,
                    verticalAlignment = RemoteAlignment.Bottom,
                ) {
                    Content()
                }
            }
        }
    }
}

@Composable
@RemoteComposable
private fun Container(modifier: RemoteModifier = RemoteModifier, content: @Composable () -> Unit) {
    RemoteBox(
        modifier = modifier.width(ContainerSize).background(Color(0xFFCFD8DC)),
        horizontalAlignment = RemoteAlignment.Start,
        verticalArrangement = RemoteArrangement.Center,
        content = content,
    )
}

@Composable
@RemoteComposable
private fun Content(modifier: RemoteModifier = RemoteModifier) {
    RemoteBox(modifier = modifier.size(48.rdp).background(Color(0xFF6200EE)))
    RemoteBox(modifier = modifier.size(24.rdp).background(Color(0xFF03DAC6)))
}

private val Padding = 24.rdp
private val ContainerSize = 100.rdp
