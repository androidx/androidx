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

package androidx.compose.remote.test.screenshot.rule

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.frontend.capture.rememberRemoteDocument
import androidx.compose.remote.frontend.layout.RemoteComposable
import androidx.compose.remote.frontend.player.RemoteDocumentPlayer as ViewRemoteDocumentPlayer
import androidx.compose.remote.player.compose.RemoteDocumentPlayer as ComposeRemoteDocumentPlayer
import androidx.compose.remote.test.screenshot.TargetPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.BitmapMatcher
import org.junit.rules.RuleChain
import org.junit.rules.TestName
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that takes screenshots of remote composable functions using devices and the Remote
 * Compose [Compose Player][androidx.compose.remote.player.compose.RemoteDocumentPlayer].
 *
 * @param matcher The algorithm to be used to perform the matching. If null, it will let
 *   [assertAgainstGolden] use its default.
 */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class RemoteComposeScreenshotTestRule(
    moduleDirectory: String,
    private val matcher: BitmapMatcher? = null,
    private val targetPlayer: TargetPlayer,
) : TestRule {
    val composeTestRule = createComposeRule()
    val testName = TestName()
    val screenshotRule = AndroidXScreenshotTestRule(moduleDirectory)

    val thisRule =
        object : TestRule {
            override fun apply(base: Statement, description: Description): Statement {
                return object : Statement() {
                    override fun evaluate() {
                        val result = base.evaluate()
                        return result
                    }
                }
            }
        }

    val delegateChain: RuleChain =
        RuleChain.outerRule(thisRule)
            .around(testName)
            .around(composeTestRule)
            .around(screenshotRule)

    override fun apply(base: Statement, description: Description): Statement {
        return delegateChain.apply(base, description)
    }

    fun runScreenshotTest(
        screenshotName: TestName = testName,
        size: Size = displaySize(),
        backgroundColor: Color? = null,
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        setContent(size = size, backgroundColor = backgroundColor, content = content)
        composeTestRule.verifyScreenshot(screenshotName, screenshotRule)
    }

    fun runScreenshotTest(
        screenshotName: TestName = testName,
        size: Size = displaySize(),
        backgroundColor: Color? = null,
        document: CoreDocument,
        outerContent: (@Composable (content: @Composable @RemoteComposable () -> Unit) -> Unit)? =
            null,
    ) {
        val boxModifier =
            Modifier.width(size.width.dp)
                .height(size.height.dp)
                .then(
                    if (backgroundColor != null) {
                        Modifier.background(backgroundColor)
                    } else {
                        Modifier
                    }
                )
                .testTag("playerRoot")
        composeTestRule.setContent {
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
        val boxModifier =
            Modifier.width(size.width.dp)
                .height(size.height.dp)
                .then(
                    if (backgroundColor != null) {
                        Modifier.background(backgroundColor)
                    } else {
                        Modifier
                    }
                )
                .testTag("playerRoot")
        composeTestRule.setContent {
            Box(modifier = boxModifier) {
                val document: CoreDocument? by rememberRemoteDocument(content = content)
                document?.let { RemoteDocumentPlayer(it, size) }
            }
        }
    }

    @Composable
    private fun RemoteDocumentPlayer(document: CoreDocument, size: Size) {
        when (targetPlayer) {
            TargetPlayer.View -> {
                ViewRemoteDocumentPlayer(
                    document,
                    size.width.toInt(),
                    size.height.toInt(),
                    debugMode = 1,
                )
            }

            TargetPlayer.Compose -> {
                ComposeRemoteDocumentPlayer(
                    document,
                    size.width.toInt(),
                    size.height.toInt(),
                    debugMode = 1,
                )
            }
        }
    }

    fun ComposeContentTestRule.verifyScreenshot(
        testName: TestName,
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
    fun TestName.goldenIdentifier(): String =
        methodName.replace("[", "_").replace("]", "").replace(",", "_")

    internal companion object {
        fun displaySize(): Size {
            val width: Int = Resources.getSystem().displayMetrics.widthPixels
            val height: Int = Resources.getSystem().displayMetrics.heightPixels
            return Size(width.toFloat(), height.toFloat())
        }
    }
}
