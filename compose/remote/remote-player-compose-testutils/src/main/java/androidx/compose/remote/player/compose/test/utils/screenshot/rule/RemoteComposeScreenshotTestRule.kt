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

package androidx.compose.remote.player.compose.test.utils.screenshot.rule

import android.content.Context
import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.capture.captureRemoteDocument
import androidx.compose.remote.creation.compose.capture.rememberRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.player.compose.ExperimentalRemoteComposePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.compose.test.utils.screenshot.TargetPlayer
import androidx.compose.remote.player.core.platform.BitmapLoader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.BitmapMatcher
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.withContext
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [org.junit.rules.TestRule] that takes screenshots of remote composable functions using devices
 * and the Remote Compose player.
 *
 * @param matcher The algorithm to be used to perform the matching. If null, it will let
 *   [androidx.compose.testutils.assertAgainstGolden] use its default.
 */
@OptIn(ExperimentalRemoteComposePlayerApi::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class RemoteComposeScreenshotTestRule(
    moduleDirectory: String,
    private val matcher: BitmapMatcher? = null,
    private val targetPlayer: TargetPlayer,
) : ExternalResource() {
    private val composeTestRule = createComposeRule(StandardTestDispatcher())
    private val screenshotRule = AndroidXScreenshotTestRule(moduleDirectory)

    private lateinit var testDescription: Description

    private val testName =
        object : TestWatcher() {

            override fun starting(description: Description) {
                testDescription = description
            }
        }

    private val delegateChain: RuleChain =
        RuleChain.outerRule(testName).around(composeTestRule).around(screenshotRule)

    var bitmapLoader = BitmapLoader.UNSUPPORTED

    override fun apply(base: Statement, description: Description): Statement {
        return delegateChain.apply(base, description)
    }

    override fun before() {
        super.before()

        RemoteComposePlayerFlags.isViewPlayerEnabled = targetPlayer == TargetPlayer.View
    }

    override fun after() {
        super.after()

        RemoteComposePlayerFlags.isViewPlayerEnabled = true
    }

    suspend fun captureDocument(
        context: Context,
        content: @Composable @RemoteComposable () -> Unit,
    ): CoreDocument {
        val document: ByteArray =
            withContext(Dispatchers.Main) { captureRemoteDocument(context) { content() } }

        val remoteComposeDocument =
            CoreDocument().apply {
                ByteArrayInputStream(document).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }
        return remoteComposeDocument
    }

    fun runTest(
        size: Size = displaySize(),
        backgroundColor: Color? = null,
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        setContent(size = size, backgroundColor = backgroundColor, content = content)
    }

    fun runScreenshotTest(
        screenshotName: Description = testDescription,
        size: Size = displaySize(),
        backgroundColor: Color? = null,
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        setContent(size = size, backgroundColor = backgroundColor, content = content)
        composeTestRule.verifyScreenshot(screenshotName, screenshotRule)
    }

    fun runScreenshotTest(
        screenshotName: Description = testDescription,
        size: Size = displaySize(),
        backgroundColor: Color? = null,
        document: CoreDocument,
        outerContent: (@Composable (content: @Composable @RemoteComposable () -> Unit) -> Unit)? =
            null,
    ) {
        composeTestRule.setContent {
            val pxSize = with(LocalDensity.current) { size.toDpSize() }

            val boxModifier =
                Modifier.width(pxSize.width)
                    .height(pxSize.height)
                    .then(
                        if (backgroundColor != null) {
                            Modifier.background(backgroundColor)
                        } else {
                            Modifier
                        }
                    )
                    .testTag("playerRoot")

            val content: @Composable @RemoteComposable () -> Unit = {
                RemoteDocumentPlayer(document, size)
            }
            Box(modifier = boxModifier) {
                if (outerContent != null) {
                    outerContent(content)
                } else {
                    content()
                }
            }
        }
        composeTestRule.verifyScreenshot(screenshotName, screenshotRule)
    }

    private fun setContent(
        size: Size,
        backgroundColor: Color?,
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        composeTestRule.setContent {
            val pxSize = with(LocalDensity.current) { size.toDpSize() }

            val boxModifier =
                Modifier.width(pxSize.width)
                    .height(pxSize.height)
                    .then(
                        if (backgroundColor != null) {
                            Modifier.background(backgroundColor)
                        } else {
                            Modifier
                        }
                    )
                    .testTag(ROOT_TEST_TAG)

            Box(modifier = boxModifier) {
                val document: CoreDocument? by rememberRemoteDocument(content = content)

                document?.let { RemoteDocumentPlayer(it, size) }
            }
        }
    }

    @Composable
    private fun RemoteDocumentPlayer(document: CoreDocument, size: Size) {
        RemoteDocumentPlayer(
            document,
            size.width.toInt(),
            size.height.toInt(),
            debugMode = 1,
            bitmapLoader = bitmapLoader,
        )
    }

    fun ComposeContentTestRule.verifyScreenshot(
        testName: Description,
        screenshotRule: AndroidXScreenshotTestRule,
    ) {
        val goldenScreenshotName = testName.goldenIdentifier()
        val screenshot = onRoot().captureToImage()
        if (matcher != null) {
            screenshot.assertAgainstGolden(screenshotRule, goldenScreenshotName, matcher)
        } else {
            screenshot.assertAgainstGolden(screenshotRule, goldenScreenshotName)
        }
    }

    /**
     * Valid characters for golden identifiers are [A-Za-z0-9_-] TestParameterInjector adds '[' +
     * parameter_values + ']' + ',' to the test name.
     */
    fun Description.goldenIdentifier(): String {
        val testIdentifier = className.substringAfterLast('.') + "_" + methodName
        return testIdentifier.replace("[\\[$]".toRegex(), "_").replace("]", "")
    }

    fun assertRootNodeContainsColor(color: Color) {
        composeTestRule.onNodeWithTag(ROOT_TEST_TAG).captureToImage().assertContainsColor(color)
    }

    internal companion object {
        const val ROOT_TEST_TAG = "playerRoot"

        fun displaySize(): Size {
            val width: Int = Resources.getSystem().displayMetrics.widthPixels
            val height: Int = Resources.getSystem().displayMetrics.heightPixels
            return Size(width.toFloat(), height.toFloat())
        }
    }
}
