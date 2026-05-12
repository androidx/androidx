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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.GoldenScreenshotNameTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** These tests validate the LayoutDirection, without using the `remote-testing` module. */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class LayoutDirectionTest {
    @get:Rule
    val composeTestRule: ComposeContentTestRule = createComposeRule(StandardTestDispatcher())
    @get:Rule val screenshotTestRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @get:Rule val goldenScreenshotNameTestRule = GoldenScreenshotNameTestRule()

    @Test
    fun creationLtr_playLtr() {
        composeTestRule.setContent {
            val creationDisplayInfo = createCreationDisplayInfo()

            val coreDocument: CoreDocument? by
                rememberRemoteDocument(creationDisplayInfo = creationDisplayInfo) {
                    SimpleContent()
                }

            coreDocument?.let {
                Player(coreDocument = it, creationDisplayInfo = creationDisplayInfo)
            }
        }

        val screenshot = composeTestRule.onRoot().captureToImage()
        screenshot.assertAgainstGolden(
            screenshotTestRule,
            goldenScreenshotNameTestRule.getGoldenScreenshotName().getName(),
        )
    }

    @Test
    fun creationLtr_playRtl() {
        composeTestRule.setContent {
            val creationDisplayInfo = createCreationDisplayInfo()

            val coreDocument: CoreDocument? by
                rememberRemoteDocument(creationDisplayInfo = creationDisplayInfo) {
                    SimpleContent()
                }

            coreDocument?.let {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Player(coreDocument = it, creationDisplayInfo = creationDisplayInfo)
                }
            }
        }

        val screenshot = composeTestRule.onRoot().captureToImage()
        screenshot.assertAgainstGolden(
            screenshotTestRule,
            goldenScreenshotNameTestRule.getGoldenScreenshotName().getName(),
        )
    }

    @Test
    fun creationRtl_playLtr() {
        composeTestRule.setContent {
            val creationDisplayInfo = createCreationDisplayInfo()

            val coreDocumentState =
                rememberRemoteDocumentCustom(
                    creationDisplayInfo = creationDisplayInfo,
                    layoutDirection = LayoutDirection.Rtl,
                ) {
                    SimpleContent()
                }

            val coreDocument = coreDocumentState.value
            coreDocument?.let {
                Player(coreDocument = coreDocument, creationDisplayInfo = creationDisplayInfo)
            }
        }

        val screenshot = composeTestRule.onRoot().captureToImage()
        screenshot.assertAgainstGolden(
            screenshotTestRule,
            goldenScreenshotNameTestRule.getGoldenScreenshotName().getName(),
        )
    }

    @Test
    fun creationRtl_playRtl() {
        composeTestRule.setContent {
            val creationDisplayInfo = createCreationDisplayInfo()

            val coreDocumentState =
                rememberRemoteDocumentCustom(
                    creationDisplayInfo = creationDisplayInfo,
                    layoutDirection = LayoutDirection.Rtl,
                ) {
                    SimpleContent()
                }
            val coreDocument = coreDocumentState.value
            coreDocument?.let {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Player(coreDocument = coreDocument, creationDisplayInfo = creationDisplayInfo)
                }
            }
        }

        val screenshot = composeTestRule.onRoot().captureToImage()
        screenshot.assertAgainstGolden(
            screenshotTestRule,
            goldenScreenshotNameTestRule.getGoldenScreenshotName().getName(),
        )
    }

    @Test
    fun complex_creationLtr_playLtr() {
        composeTestRule.setContent {
            val creationDisplayInfo = createCreationDisplayInfo()

            val coreDocument: CoreDocument? by
                rememberRemoteDocument(creationDisplayInfo = creationDisplayInfo) {
                    ComplexContent()
                }

            coreDocument?.let {
                Player(coreDocument = it, creationDisplayInfo = creationDisplayInfo)
            }
        }

        val screenshot = composeTestRule.onRoot().captureToImage()
        screenshot.assertAgainstGolden(
            screenshotTestRule,
            goldenScreenshotNameTestRule.getGoldenScreenshotName().getName(),
        )
    }

    @Test
    fun complex_creationLtr_playRtl() {
        composeTestRule.setContent {
            val creationDisplayInfo = createCreationDisplayInfo()

            val coreDocument: CoreDocument? by
                rememberRemoteDocument(creationDisplayInfo = creationDisplayInfo) {
                    ComplexContent()
                }

            coreDocument?.let {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Player(coreDocument = it, creationDisplayInfo = creationDisplayInfo)
                }
            }
        }

        val screenshot = composeTestRule.onRoot().captureToImage()
        screenshot.assertAgainstGolden(
            screenshotTestRule,
            goldenScreenshotNameTestRule.getGoldenScreenshotName().getName(),
        )
    }

    @Test
    fun complex_creationRtl_playLtr() {
        composeTestRule.setContent {
            val creationDisplayInfo = createCreationDisplayInfo()

            val coreDocumentState =
                rememberRemoteDocumentCustom(
                    creationDisplayInfo = creationDisplayInfo,
                    layoutDirection = LayoutDirection.Rtl,
                ) {
                    ComplexContent()
                }

            val coreDocument = coreDocumentState.value
            coreDocument?.let {
                Player(coreDocument = coreDocument, creationDisplayInfo = creationDisplayInfo)
            }
        }

        val screenshot = composeTestRule.onRoot().captureToImage()
        screenshot.assertAgainstGolden(
            screenshotTestRule,
            goldenScreenshotNameTestRule.getGoldenScreenshotName().getName(),
        )
    }

    @Test
    fun complex_creationRtl_playRtl() {
        composeTestRule.setContent {
            val creationDisplayInfo = createCreationDisplayInfo()

            val coreDocumentState =
                rememberRemoteDocumentCustom(
                    creationDisplayInfo = creationDisplayInfo,
                    layoutDirection = LayoutDirection.Rtl,
                ) {
                    ComplexContent()
                }
            val coreDocument = coreDocumentState.value
            coreDocument?.let {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Player(coreDocument = coreDocument, creationDisplayInfo = creationDisplayInfo)
                }
            }
        }

        val screenshot = composeTestRule.onRoot().captureToImage()
        screenshot.assertAgainstGolden(
            screenshotTestRule,
            goldenScreenshotNameTestRule.getGoldenScreenshotName().getName(),
        )
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

    @Composable
    private fun rememberRemoteDocumentCustom(
        creationDisplayInfo: RemoteCreationDisplayInfo = createCreationDisplayInfo(),
        layoutDirection: LayoutDirection = LayoutDirection.Rtl,
        content: @Composable () -> Unit,
    ): MutableState<CoreDocument?> {
        val coreDocumentState = remember { mutableStateOf<CoreDocument?>(null) }
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            val docState =
                rememberRemoteDocument(creationDisplayInfo = creationDisplayInfo, content = content)
            coreDocumentState.value = docState.value
        }
        return coreDocumentState
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
