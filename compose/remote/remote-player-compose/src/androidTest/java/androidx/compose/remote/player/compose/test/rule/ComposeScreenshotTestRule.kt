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

package androidx.compose.remote.player.compose.test.rule

import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.BitmapMatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that takes screenshots of composable functions using devices.
 *
 * @param matcher The algorithm to be used to perform the matching. If null, it will let
 *   [assertAgainstGolden] use its default.
 */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class ComposeScreenshotTestRule(
    moduleDirectory: String,
    private val matcher: BitmapMatcher? = null,
) : TestRule {
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

    override fun apply(base: Statement, description: Description): Statement {
        return delegateChain.apply(base, description)
    }

    fun runScreenshotTest(content: @Composable () -> Unit) {
        composeTestRule.setContent(composable = content)
        composeTestRule.verifyScreenshot()
    }

    fun ComposeContentTestRule.verifyScreenshot() {
        val goldenScreenshotName = testDescription.goldenIdentifier()
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
}
