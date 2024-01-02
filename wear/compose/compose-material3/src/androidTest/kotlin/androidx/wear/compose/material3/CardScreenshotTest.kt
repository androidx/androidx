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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
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
    fun card_ltr() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
        sampleCard()
    }

    @Test
    fun card_disabled() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
        sampleCard(enabled = false)
    }

    @Test
    fun card_rtl() = verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
        sampleCard()
    }

    @Test
    fun card_image_background() = verifyScreenshot {
        sampleCard(
            colors = CardDefaults.imageCardColors(
                containerPainter = CardDefaults.imageWithScrimBackgroundPainter(
                    backgroundImagePainter = painterResource(
                        id = androidx.wear.compose.material3.test.R.drawable.backgroundimage1
                    )
                )
            )
        )
    }

    @Test
    fun outlined_card_ltr() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
        sampleOutlinedCard()
    }

    @Test
    fun outlined_card_disabled() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
        sampleOutlinedCard(enabled = false)
    }

    @Test
    fun outlined_card_rtl() = verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
        sampleOutlinedCard()
    }

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
    fun app_card_image_background() = verifyScreenshot {
        sampleAppCard(
            colors = CardDefaults.imageCardColors(
                containerPainter = CardDefaults.imageWithScrimBackgroundPainter(
                    backgroundImagePainter = painterResource(
                        id = androidx.wear.compose.material3.test.R.drawable.backgroundimage1
                    )
                )
            )
        )
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
    fun title_card_with_time_and_subtitle_ltr() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
            sampleTitleCardWithTimeAndSubtitle()
        }

    @Test
    fun title_card_with_time_and_subtitle_disabled() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
            sampleTitleCardWithTimeAndSubtitle(enabled = false)
        }

    @Test
    fun title_card_with_time_and_subtitle_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleTitleCardWithTimeAndSubtitle()
        }

    @Test
    fun title_card_with_content_time_and_subtitle_ltr() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
            sampleTitleCardWithContentTimeAndSubtitle()
        }

    @Test
    fun title_card_with_content_time_and_subtitle_disabled() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
            sampleTitleCardWithContentTimeAndSubtitle(enabled = false)
        }

    @Test
    fun title_card_with_content_time_and_subtitle_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleTitleCardWithContentTimeAndSubtitle()
        }

    @Test
    fun title_card_image_background() = verifyScreenshot {
        sampleTitleCard(
            colors = CardDefaults.imageCardColors(
                containerPainter = CardDefaults.imageWithScrimBackgroundPainter(
                    backgroundImagePainter = painterResource(
                        id = androidx.wear.compose.material3.test.R.drawable.backgroundimage1
                    )
                )
            )
        )
    }

    @Composable
    private fun sampleCard(
        enabled: Boolean = true,
        colors: CardColors = CardDefaults.cardColors()
    ) {
        Card(
            enabled = enabled,
            onClick = {},
            colors = colors,
            modifier = Modifier
                .testTag(TEST_TAG)
                .width(cardWidth),
        ) {
            Text("Card: Some body content")
        }
    }

    @Composable
    private fun sampleOutlinedCard(
        enabled: Boolean = true,
    ) {
        OutlinedCard(
            enabled = enabled,
            onClick = {},
            modifier = Modifier
                .testTag(TEST_TAG)
                .width(cardWidth),
        ) {
            Text("Outlined Card: Some body content")
        }
    }

    @Composable
    private fun sampleAppCard(
        enabled: Boolean = true,
        colors: CardColors = CardDefaults.cardColors()
    ) {
        AppCard(
            enabled = enabled,
            onClick = {},
            appName = { Text("AppName") },
            appImage = { TestIcon() },
            title = { Text("AppCard") },
            colors = colors,
            time = { Text("now") },
            modifier = Modifier
                .testTag(TEST_TAG)
                .width(cardWidth),
        ) {
            Text("Some body content")
            Text("and some more body content")
        }
    }

    @Composable
    private fun sampleTitleCard(
        enabled: Boolean = true,
        colors: CardColors = CardDefaults.cardColors()
    ) {
        TitleCard(
            enabled = enabled,
            onClick = {},
            title = { Text("TitleCard") },
            time = { Text("now") },
            colors = colors,
            modifier = Modifier
                .testTag(TEST_TAG)
                .width(cardWidth),
        ) {
            Text("Some body content")
            Text("and some more body content")
        }
    }

    @Composable
    private fun sampleTitleCardWithTimeAndSubtitle(
        enabled: Boolean = true
    ) {
        TitleCard(
            enabled = enabled,
            onClick = {},
            time = { Text("XXm") },
            title = { Text("TitleCard") },
            subtitle = { Text("Subtitle") },
            modifier = Modifier.testTag(TEST_TAG),
        )
    }

    @Composable
    private fun sampleTitleCardWithContentTimeAndSubtitle(
        enabled: Boolean = true
    ) {
        TitleCard(
            enabled = enabled,
            onClick = {},
            time = { Text("XXm") },
            title = { Text("TitleCard") },
            subtitle = { Text("Subtitle") },
            modifier = Modifier.testTag(TEST_TAG),
        ) {
            Text("Card content")
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

    private val cardWidth = 168.dp
}
