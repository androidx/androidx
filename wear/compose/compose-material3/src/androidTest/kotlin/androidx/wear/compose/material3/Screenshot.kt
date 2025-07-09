/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material3

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.BitmapMatcher
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.rules.TestName

/**
 * Verify node tagged with 'testTag' against golden screenshot, and generate a new golden screenshot
 * if generateScreenshots is true.
 *
 * To re-generate screenshots locally:
 * 1) Pass 'generateScreenshots = true' or set GENERATE_SCREENSHOTS = true in Screenshot.kt
 * 2) Run the tests on a Medium Phone API 35 emulator
 * 3) Use adb pull to access the generated files from
 *    storage/emulated/0/Android/data/androidx.wear.compose.material3.test/cache/screenshots
 * 4) Create a CL for the goldens in platform/frameworks/support-golden to submit alongside your
 *    other changes, linked by topic. See aosp/3681063 as an example.
 *
 * @param testName The test name returned by org.junit.rules.TestName(), used for the golden
 *   screenshot file names.
 * @param screenshotRule AndroidXScreenshotTestRule instance created as a class property, which
 *   locates the directory containing the golden screenshots.
 * @param generateScreenshots Whether to generate new golden screenshots.
 * @param testTag The tag of the node to verify.
 * @param matcher The matcher used to compare the screenshot against the goldens - default uses a a
 *   threshold of 0.98, it can be useful to pass 1.0 to test a more exact match.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal fun ComposeContentTestRule.verifyScreenshot(
    testName: TestName,
    screenshotRule: AndroidXScreenshotTestRule,
    generateScreenshots: Boolean = GENERATE_SCREENSHOTS,
    testTag: String = TEST_TAG,
    matcher: BitmapMatcher = MSSIMMatcher(),
) {
    val goldenScreenshotName = testName.goldenIdentifier()
    val screenshot = this.onNodeWithTag(testTag).captureToImage()

    if (generateScreenshots) {
        screenshot.writeToDevice(goldenScreenshotName + "_emulator")
    }

    screenshot.assertAgainstGolden(screenshotRule, goldenScreenshotName, matcher = matcher)
}

/**
 * Verify node tagged with 'testTag' against golden screenshot, and generate a new golden screenshot
 * if generateScreenshots is true. This overload also accepts the content to be tested as a lambda.
 *
 * To re-generate screenshots locally:
 * 1) Pass 'generateScreenshots = true' or set GENERATE_SCREENSHOTS = true in Screenshot.kt
 * 2) Run the tests on a Medium Phone API 35 emulator
 * 3) Use adb pull to access the generated files from
 *    storage/emulated/0/Android/data/androidx.wear.compose.material3.test/cache/screenshots
 * 4) Create a CL for the goldens in platform/frameworks/support-golden to submit alongside your
 *    other changes, linked by topic. See aosp/3681063 as an example.
 *
 * @param testName The test name returned by org.junit.rules.TestName(), used for the golden
 *   screenshot file names.
 * @param screenshotRule AndroidXScreenshotTestRule instance created as a class property, which
 *   locates the directory containing the golden screenshots.
 * @param generateScreenshots Whether to generate new golden screenshots.
 * @param testTag The tag of the node to verify.
 * @param layoutDirection The layout direction of the content.
 * @param matcher The matcher used to compare the screenshot against the goldens - default uses a a
 *   threshold of 0.98, it can be useful to pass 1.0 to test a more exact match.
 * @param content The content for which a screenshot will be generated.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal fun ComposeContentTestRule.verifyScreenshot(
    testName: TestName,
    screenshotRule: AndroidXScreenshotTestRule,
    generateScreenshots: Boolean = GENERATE_SCREENSHOTS,
    testTag: String = TEST_TAG,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    matcher: BitmapMatcher = MSSIMMatcher(),
    content: @Composable () -> Unit,
) {
    setContentWithTheme {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
                content()
            }
        }
    }

    verifyScreenshot(
        testName,
        testTag = testTag,
        screenshotRule = screenshotRule,
        generateScreenshots = generateScreenshots,
        matcher = matcher,
    )
}

internal const val SCREENSHOT_GOLDEN_PATH = "wear/compose/compose-material3"

/**
 * Global override for generating all screenshots. This file contains utilities for verifying and
 * re-generating screenshots. See KDoc for [verifyScreenshot] for steps to re-generate the
 * screenshots. See below for commands to pull, move and tidy up the generated files.
 */
private const val GENERATE_SCREENSHOTS = false

/*
 * Commands to pull, move and tidy up the generated screenshot files:
 adb pull storage/emulated/0/Android/data/androidx.wear.compose.material3.test/cache/screenshots \
     ../../golden/wear/compose/compose-material3
 mv ../../golden/wear/compose/compose-material3/screenshots \
     ../../golden/wear/compose/compose-material3
 rmdir ../../golden/wear/compose/compose-material3/screenshots
*/
