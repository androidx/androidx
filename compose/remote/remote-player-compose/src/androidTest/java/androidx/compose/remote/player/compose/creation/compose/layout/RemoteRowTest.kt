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

import androidx.compose.remote.creation.compose.layout.Alignment
import androidx.compose.remote.creation.compose.layout.Arrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.player.compose.ExperimentalRemoteComposePlayerApi
import androidx.compose.remote.player.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.test.screenshot.TargetPlayer
import androidx.compose.remote.test.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
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
            matcher = MSSIMMatcher(threshold = 0.999),
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
            matcher = MSSIMMatcher(threshold = 0.999),
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
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Content()
                }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteRow(
                    modifier = RemoteModifier.size(ContainerSize),
                    verticalAlignment = Alignment.Bottom,
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
                    horizontalArrangement = Arrangement.CenterHorizontally,
                ) {
                    Content()
                }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteRow(
                    modifier = RemoteModifier.size(ContainerSize),
                    horizontalArrangement = Arrangement.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Content()
                }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteRow(
                    modifier = RemoteModifier.size(ContainerSize),
                    horizontalArrangement = Arrangement.CenterHorizontally,
                    verticalAlignment = Alignment.Bottom,
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
                    horizontalArrangement = Arrangement.End,
                ) {
                    Content()
                }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteRow(
                    modifier = RemoteModifier.size(ContainerSize),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Content()
                }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteRow(
                    modifier = RemoteModifier.size(ContainerSize),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom,
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
        horizontalAlignment = Alignment.Start,
        content = content,
    )
}

@Composable
@RemoteComposable
private fun Content(modifier: RemoteModifier = RemoteModifier) {
    RemoteBox(modifier = modifier.size(48.dp).background(Color(0xFF6200EE)))
    RemoteBox(modifier = modifier.size(24.dp).background(Color(0xFF03DAC6)))
}

private val Padding = 24.dp
private val ContainerSize = 100.dp
