/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.wear.compose.material.test

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.SCREENSHOT_GOLDEN_PATH
import androidx.wear.compose.material.TEST_TAG
import androidx.wear.compose.material.TestIcon
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TitleCard
import androidx.wear.compose.material.setContentWithTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class CardScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    @Test
    fun app_card_ltr() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
        sampleAppCard()
    }

    @Test
    fun app_card_disabled() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
        sampleAppCard(enabled = false)
    }

    @Test
    fun app_card_rtl() = verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
        sampleAppCard()
    }

    @Test
    fun title_card_ltr() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
        sampleTitleCard()
    }

    @Test
    fun title_card_disabled() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
        sampleTitleCard(enabled = false)
    }

    @Test
    fun title_card_rtl() = verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
        sampleTitleCard()
    }

    @Test
    fun title_card_image_background() = verifyScreenshot {
        sampleTitleCard(
            backgroundPainter = CardDefaults.imageWithScrimBackgroundPainter(
                backgroundImagePainter = painterResource(id = R.drawable.backgroundimage1))
        )
    }

    @Composable
    private fun sampleAppCard(enabled: Boolean = true) {
        AppCard(
            enabled = enabled,
            onClick = {},
            appName = { Text("AppName") },
            appImage = { TestIcon() },
            title = { Text("AppCard") },
            time = { Text("now") },
            modifier = Modifier.testTag(TEST_TAG),
        ) {
            Text("Some body content")
            Text("and some more body content")
        }
    }

    @Composable
    private fun sampleTitleCard(
        enabled: Boolean = true,
        backgroundPainter: Painter = CardDefaults.cardBackgroundPainter()
    ) {
        TitleCard(
            enabled = enabled,
            onClick = {},
            title = { Text("TitleCard") },
            time = { Text("now") },
            backgroundPainter = backgroundPainter,
            modifier = Modifier.testTag(TEST_TAG),
        ) {
            Text("Some body content")
            Text("and some more body content")
        }
    }

    private fun verifyScreenshot(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit
    ) {
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                content()
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}
