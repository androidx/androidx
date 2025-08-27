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

package androidx.compose.remote.foundation

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.frontend.capture.rememberRemoteDocument
import androidx.compose.remote.frontend.layout.RemoteComposable
import androidx.compose.remote.frontend.player.RemoteDocumentPlayer
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
import org.junit.rules.RuleChain
import org.junit.rules.TestName
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** A [TestRule] that takes screenshots of remote composable functions using devices. */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class RemoteComposeScreenshotTestRule() : TestRule {
    val composeTestRule = createComposeRule()
    val testName = TestName()
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_REMOTE_COMPOSE)

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
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        setContent(size = size) { content() }
        composeTestRule.verifyScreenshot(screenshotName, screenshotRule)
    }

    private fun setContent(size: Size, content: @Composable @RemoteComposable () -> Unit) {
        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.width(size.width.dp)
                        .height(size.height.dp)
                        .background(Color.Black)
                        .testTag("playerRoot")
            ) {
                val document: CoreDocument? by rememberRemoteDocument { content() }
                document?.let {
                    RemoteDocumentPlayer(it, size.width.toInt(), size.height.toInt(), 1)
                }
            }
        }
    }

    fun ComposeContentTestRule.verifyScreenshot(
        testName: TestName,
        screenshotRule: AndroidXScreenshotTestRule,
    ) {
        val goldenScreenshotName = testName.goldenIdentifier()
        val screenshot = onRoot().captureToImage()
        screenshot.assertAgainstGolden(screenshotRule, goldenScreenshotName)
    }

    /**
     * Valid characters for golden identifiers are [A-Za-z0-9_-] TestParameterInjector adds '[' +
     * parameter_values + ']' + ',' to the test name.
     */
    fun TestName.goldenIdentifier(): String =
        methodName.replace("[", "_").replace("]", "").replace(",", "_")

    internal companion object {
        const val GOLDEN_REMOTE_COMPOSE = "compose/remote/remote-foundation"

        fun displaySize(): Size {
            val width: Int = Resources.getSystem().displayMetrics.widthPixels
            val height: Int = Resources.getSystem().displayMetrics.heightPixels
            return Size(width.toFloat(), height.toFloat())
        }
    }
}
