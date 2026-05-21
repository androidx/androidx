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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteString
import androidx.compose.remote.testing.util.GoldenScreenshotNameTestRule
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Verify that a [RemoteContentTestRule] and [RemoteDocContentTestRule] can be used together in the
 * same test, sharing the same instance of [ComposeContentTestRule].
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RemoteSharedTestRuleScreenshotTest {
    private val composeTestRule: ComposeContentTestRule =
        createComposeRule(StandardTestDispatcher())
    private val remoteContentTestRule = RemoteContentTestRule(composeTestRule)
    private val remoteDocContentTestRule = RemoteDocContentTestRule(composeTestRule)

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)
    @get:Rule val goldenScreenshotNameTestRule = GoldenScreenshotNameTestRule()

    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(remoteContentTestRule)
            .around(remoteDocContentTestRule)
            .around(composeTestRule)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun content() {
        remoteContentTestRule.setContent(createCreationDisplayInfo(context)) {
            val text = rememberMutableRemoteString("Initial")
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize().background(Color.White),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(text, color = Color.Black.rc)
            }
        }

        val screenshotBefore = remoteContentTestRule.captureRootToImage()
        screenshotBefore.assertAgainstGolden(screenshotRule, goldenScreenshotNameTestRule.getName())
    }

    @Test
    fun doc() {
        val doc = CoreDocument()

        val documentStream = context.assets.open("screenshottest.rc")
        val buffer = RemoteComposeBuffer.fromInputStream(documentStream)
        doc.initFromBuffer(buffer)

        remoteDocContentTestRule.setContent(coreDocument = doc, size = Size(100f, 100f))

        val screenshotBefore = remoteDocContentTestRule.captureRootToImage()
        screenshotBefore.assertAgainstGolden(screenshotRule, goldenScreenshotNameTestRule.getName())
    }
}
