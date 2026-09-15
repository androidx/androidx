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

package androidx.compose.remote.creation.compose.capture

import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** These tests validate the LayoutDirection using [RemoteScreenshotTestRule]. */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class LayoutDirectionTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )

    @Test
    fun creationLtr_playLtr() {
        remoteComposeTestRule.runScreenshotTest { SimpleContent() }
    }

    @Test
    fun creationLtr_playRtl() {
        remoteComposeTestRule.runScreenshotTest(playComposableWrapper = ComposableWrappers.rtl) {
            SimpleContent()
        }
    }

    @Test
    fun creationRtl_playLtr() {
        remoteComposeTestRule.runScreenshotTest(
            creationComposableWrapper = ComposableWrappers.rtl
        ) {
            SimpleContent()
        }
    }

    @Test
    fun creationRtl_playRtl() {
        remoteComposeTestRule.runScreenshotTest(
            creationComposableWrapper = ComposableWrappers.rtl,
            playComposableWrapper = ComposableWrappers.rtl,
        ) {
            SimpleContent()
        }
    }

    @Test
    fun complex_creationLtr_playLtr() {
        remoteComposeTestRule.runScreenshotTest { ComplexContent() }
    }

    @Test
    fun complex_creationLtr_playRtl() {
        remoteComposeTestRule.runScreenshotTest(playComposableWrapper = ComposableWrappers.rtl) {
            ComplexContent()
        }
    }

    @Test
    fun complex_creationRtl_playLtr() {
        remoteComposeTestRule.runScreenshotTest(
            creationComposableWrapper = ComposableWrappers.rtl
        ) {
            ComplexContent()
        }
    }

    @Test
    fun complex_creationRtl_playRtl() {
        remoteComposeTestRule.runScreenshotTest(
            creationComposableWrapper = ComposableWrappers.rtl,
            playComposableWrapper = ComposableWrappers.rtl,
        ) {
            ComplexContent()
        }
    }

    @Composable
    @RemoteComposable
    private fun SimpleContent(modifier: RemoteModifier = RemoteModifier) {
        RemoteBox(modifier = modifier.fillMaxSize(), contentAlignment = RemoteAlignment.CenterEnd) {
            RemoteBox(modifier = RemoteModifier.size(100.rdp).background(Color.Red.rc))
        }
    }

    @Composable
    @RemoteComposable
    private fun ComplexContent(modifier: RemoteModifier = RemoteModifier) {
        RemoteBox(modifier = modifier.fillMaxSize(), contentAlignment = RemoteAlignment.CenterEnd) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                RemoteBox(
                    modifier = RemoteModifier.size(300.rdp).background(Color.Red.rc),
                    contentAlignment = RemoteAlignment.CenterStart,
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        RemoteBox(
                            modifier = RemoteModifier.size(200.rdp).background(Color.Green.rc),
                            contentAlignment = RemoteAlignment.CenterEnd,
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(100.rdp).background(Color.Blue.rc),
                                contentAlignment = RemoteAlignment.CenterEnd,
                            ) {}
                        }
                    }
                }
            }
        }
    }
}
