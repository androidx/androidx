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

package androidx.compose.remote.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.testing.util.GoldenScreenshotNameTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** These tests validate the LayoutDirection */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RemoteContentTestRuleLayoutDirectionTest {
    @get:Rule val remoteContentTestRule: RemoteContentTestRule = RemoteContentTestRule()
    @get:Rule val screenshotTestRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @get:Rule val goldenScreenshotNameTestRule = GoldenScreenshotNameTestRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val creationDisplayInfo = createCreationDisplayInfo(context = context)

    @Test
    fun creationLtr_playLtr() {
        remoteContentTestRule.setContent(remoteCreationDisplayInfo = creationDisplayInfo) {
            SimpleContent()
        }

        val screenshot = remoteContentTestRule.captureRootToImage()
        screenshot.assertAgainstGolden(screenshotTestRule, goldenScreenshotNameTestRule.getName())
    }

    @Test
    fun creationLtr_playRtl() {
        remoteContentTestRule.setContent(
            remoteCreationDisplayInfo = creationDisplayInfo,
            playComposableWrapper = rtlContent(),
        ) {
            SimpleContent()
        }

        val screenshot = remoteContentTestRule.captureRootToImage()
        screenshot.assertAgainstGolden(screenshotTestRule, goldenScreenshotNameTestRule.getName())
    }

    @Test
    fun creationRtl_playLtr() {
        remoteContentTestRule.setContent(
            remoteCreationDisplayInfo = creationDisplayInfo,
            creationComposableWrapper = rtlContent(),
        ) {
            SimpleContent()
        }

        val screenshot = remoteContentTestRule.captureRootToImage()
        screenshot.assertAgainstGolden(screenshotTestRule, goldenScreenshotNameTestRule.getName())
    }

    @Test
    fun creationRtl_playRtl() {
        remoteContentTestRule.setContent(
            remoteCreationDisplayInfo = creationDisplayInfo,
            creationComposableWrapper = rtlContent(),
            playComposableWrapper = rtlContent(),
        ) {
            SimpleContent()
        }

        val screenshot = remoteContentTestRule.captureRootToImage()
        screenshot.assertAgainstGolden(screenshotTestRule, goldenScreenshotNameTestRule.getName())
    }

    @Test
    fun complex_creationLtr_playLtr() {
        remoteContentTestRule.setContent(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComplexContent()
        }

        val screenshot = remoteContentTestRule.captureRootToImage()
        screenshot.assertAgainstGolden(screenshotTestRule, goldenScreenshotNameTestRule.getName())
    }

    @Test
    fun complex_creationLtr_playRtl() {
        remoteContentTestRule.setContent(
            remoteCreationDisplayInfo = creationDisplayInfo,
            playComposableWrapper = rtlContent(),
        ) {
            ComplexContent()
        }

        val screenshot = remoteContentTestRule.captureRootToImage()
        screenshot.assertAgainstGolden(screenshotTestRule, goldenScreenshotNameTestRule.getName())
    }

    @Test
    fun complex_creationRtl_playLtr() {
        remoteContentTestRule.setContent(
            remoteCreationDisplayInfo = creationDisplayInfo,
            creationComposableWrapper = rtlContent(),
        ) {
            ComplexContent()
        }

        val screenshot = remoteContentTestRule.captureRootToImage()
        screenshot.assertAgainstGolden(screenshotTestRule, goldenScreenshotNameTestRule.getName())
    }

    @Test
    fun complex_creationRtl_playRtl() {
        remoteContentTestRule.setContent(
            remoteCreationDisplayInfo = creationDisplayInfo,
            creationComposableWrapper = rtlContent(),
            playComposableWrapper = rtlContent(),
        ) {
            ComplexContent()
        }

        val screenshot = remoteContentTestRule.captureRootToImage()
        screenshot.assertAgainstGolden(screenshotTestRule, goldenScreenshotNameTestRule.getName())
    }

    @Composable
    @RemoteComposable
    private fun SimpleContent(modifier: RemoteModifier = RemoteModifier) {
        RemoteBox(modifier = modifier.fillMaxSize(), contentAlignment = RemoteAlignment.CenterEnd) {
            RemoteBox(modifier = RemoteModifier.size(100.rdp).background(Color.Red))
        }
    }

    @Composable
    @RemoteComposable
    private fun ComplexContent(modifier: RemoteModifier = RemoteModifier) {
        RemoteBox(modifier = modifier.fillMaxSize(), contentAlignment = RemoteAlignment.CenterEnd) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                RemoteBox(
                    modifier = RemoteModifier.size(300.rdp).background(Color.Red),
                    contentAlignment = RemoteAlignment.CenterStart,
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        RemoteBox(
                            modifier = RemoteModifier.size(200.rdp).background(Color.Green),
                            contentAlignment = RemoteAlignment.CenterEnd,
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(100.rdp).background(Color.Blue),
                                contentAlignment = RemoteAlignment.CenterEnd,
                            ) {}
                        }
                    }
                }
            }
        }
    }

    private fun rtlContent(): (@Composable (composable: @Composable () -> Unit) -> Unit) =
        { content ->
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                content = content,
            )
        }

    @Composable
    private fun Player(
        modifier: Modifier = Modifier,
        coreDocument: CoreDocument,
        creationDisplayInfo: RemoteCreationDisplayInfo,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            RemoteDocumentPlayer(
                document = coreDocument,
                documentWidth = creationDisplayInfo.size.width.toInt(),
                documentHeight = creationDisplayInfo.size.height.toInt(),
            )
        }
    }
}
