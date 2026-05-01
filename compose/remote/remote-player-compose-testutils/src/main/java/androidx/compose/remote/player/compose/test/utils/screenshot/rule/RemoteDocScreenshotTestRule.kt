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

package androidx.compose.remote.player.compose.test.utils.screenshot.rule

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.testing.RemoteBaseDocContentTestRule
import androidx.compose.remote.testing.RemoteContentTestRule
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.BitmapMatcher
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that uses [RemoteContentTestRule] to set the Remote Compose content and uses
 * [AndroidXScreenshotTestRule] for screenshot testing.
 */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class RemoteDocScreenshotTestRule(moduleDirectory: String, val matcher: BitmapMatcher? = null) :
    TestRule {
    private val remoteContentTestRule = RemoteBaseDocContentTestRule()

    private val screenshotTestRule = AndroidXScreenshotTestRule(moduleDirectory)

    private val testNameRule =
        object : TestWatcher() {

            override fun starting(description: Description) {
                testDescription = description
            }
        }

    private val delegateChain: RuleChain =
        RuleChain.outerRule(testNameRule).around(remoteContentTestRule).around(screenshotTestRule)

    private lateinit var testDescription: Description

    override fun apply(base: Statement, description: Description): Statement {
        return delegateChain.apply(base, description)
    }

    fun runScreenshotTest(
        coreDocument: CoreDocument,
        context: Context,
        goldenScreenshotName: GoldenScreenshotName? = null,
        composableWrapper: (@Composable (composable: @Composable () -> Unit) -> Unit)? = null,
    ) {
        runScreenshotTestInternal(
            coreDocument = coreDocument,
            size =
                Size(
                    context.resources.displayMetrics.widthPixels.toFloat(),
                    context.resources.displayMetrics.heightPixels.toFloat(),
                ),
            goldenScreenshotName = getGoldenScreenshotName(goldenScreenshotName),
            composableWrapper = composableWrapper,
        )
    }

    fun runScreenshotTest(
        coreDocument: CoreDocument,
        size: Size,
        goldenScreenshotName: GoldenScreenshotName? = null,
        composableWrapper: (@Composable (composable: @Composable () -> Unit) -> Unit)? = null,
    ) {
        runScreenshotTestInternal(
            coreDocument = coreDocument,
            size = size,
            goldenScreenshotName = getGoldenScreenshotName(goldenScreenshotName),
            composableWrapper = composableWrapper,
        )
    }

    fun runScreenshotTestInternal(
        coreDocument: CoreDocument,
        size: Size,
        goldenScreenshotName: GoldenScreenshotName,
        composableWrapper: (@Composable (content: @Composable () -> Unit) -> Unit)?,
    ) {
        remoteContentTestRule.setContent(
            coreDocument = coreDocument,
            player = PlayerImpl(composableWrapper = composableWrapper),
            size = size,
        )

        val screenshot =
            remoteContentTestRule.composeTestRule.onNodeWithTag(ROOT_TEST_TAG).captureToImage()

        if (matcher == null) {
            screenshot.assertAgainstGolden(screenshotTestRule, goldenScreenshotName.getName())
        } else {
            screenshot.assertAgainstGolden(
                screenshotTestRule,
                goldenScreenshotName.getName(),
                matcher,
            )
        }
    }

    private fun getGoldenScreenshotName(goldenScreenshotName: GoldenScreenshotName?) =
        goldenScreenshotName ?: GoldenScreenshotName(testDescription)

    companion object {
        const val ROOT_TEST_TAG: String = "ROOT_TEST_TAG"
    }

    private class PlayerImpl(
        private val composableWrapper: (@Composable (composable: @Composable () -> Unit) -> Unit)?
    ) : RemoteBaseDocContentTestRule.Player {
        @Composable
        override fun Play(coreDocument: CoreDocument, size: Size) {
            Box(
                modifier =
                    Modifier.width(size.width.toInt().dp)
                        .height(size.height.toInt().dp)
                        .testTag(ROOT_TEST_TAG)
            ) {
                val composable: @Composable () -> Unit = {
                    RemoteDocumentPlayer(
                        document = coreDocument,
                        documentWidth = size.width.toInt(),
                        documentHeight = size.height.toInt(),
                    )
                }
                if (composableWrapper == null) {
                    composable()
                } else {
                    composableWrapper { composable() }
                }
            }
        }
    }
}
