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

package androidx.wear.compose.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.material3.samples.R
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

/**
 * NonClickableCardScreenshotTest uses same test function names and parameters as CardScreenshotTest
 * to verify that non-clickable Card variants has same appearance with clickable card.
 */
@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class NonClickableCardScreenshotTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun card(
        @TestParameter enabled: CardScreenshotTest.EnabledState,
        @TestParameter layoutDirection: LayoutDirection,
    ) =
        rule.verifyScreenshot(testName, screenshotRule, layoutDirection = layoutDirection) {
            TestNonClickableCard()
        }

    @Test
    fun card_image_background() =
        rule.verifyScreenshot(testName, screenshotRule) {
            TestNonClickableCardWithContainerPainter(
                image =
                    painterResource(
                        id = androidx.wear.compose.material3.test.R.drawable.backgroundimage1
                    ),
                sizeToIntrinsics = false,
            )
        }

    @Test
    fun card_image_background_with_intrinsic_size() =
        rule.verifyScreenshot(testName, screenshotRule) {
            TestNonClickableCardWithContainerPainter(
                image =
                    painterResource(
                        id = androidx.wear.compose.material3.test.R.drawable.backgroundimage1
                    ),
                sizeToIntrinsics = true,
            )
        }

    @Test
    fun outlined_card(
        @TestParameter enabled: CardScreenshotTest.EnabledState,
        @TestParameter layoutDirection: LayoutDirection,
    ) =
        rule.verifyScreenshot(testName, screenshotRule, layoutDirection = layoutDirection) {
            OutlinedCard(modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max)) {
                Text("Outlined Card: Some body content")
            }
        }

    @Test
    fun app_card() = rule.verifyScreenshot(testName, screenshotRule) { TestNonClickableAppCard() }

    @Test
    fun app_card(
        @TestParameter enabled: CardScreenshotTest.EnabledState,
        @TestParameter layoutDirection: LayoutDirection,
    ) =
        rule.verifyScreenshot(testName, screenshotRule, layoutDirection = layoutDirection) {
            TestNonClickableAppCard()
        }

    @Test
    fun app_card_with_border() =
        rule.verifyScreenshot(testName, screenshotRule) {
            TestNonClickableAppCard(borderStroke = BorderStroke(4.dp, Color.Red))
        }

    @Test
    fun app_card_with_content_padding() =
        rule.verifyScreenshot(testName, screenshotRule) {
            TestNonClickableAppCard(contentPadding = PaddingValues(all = 16.dp))
        }

    @Test
    fun app_card_with_body_image() =
        rule.verifyScreenshot(testName, screenshotRule) {
            TestNonClickableAppCard(hasBodyImage = true)
        }

    @Test
    fun title_card(
        @TestParameter enabled: CardScreenshotTest.EnabledState,
        @TestParameter layoutDirection: LayoutDirection,
    ) =
        rule.verifyScreenshot(testName, screenshotRule, layoutDirection = layoutDirection) {
            TestNonClickableTitleCard(hasTime = true, useIntrinsicWidth = true)
        }

    @Test
    fun title_card(
        @TestParameter hasTime: CardScreenshotTest.TimeState,
        @TestParameter hasSubtitle: CardScreenshotTest.SubtitleState,
        @TestParameter hasContent: CardScreenshotTest.ContentState,
        @TestParameter layoutDirection: LayoutDirection,
    ) =
        rule.verifyScreenshot(testName, screenshotRule, layoutDirection = layoutDirection) {
            TestNonClickableTitleCard(
                hasTime = hasTime.enabled,
                hasSubtitle = hasSubtitle.enabled,
                hasContent = hasContent.enabled,
            )
        }

    @Test
    fun title_card_with_content_time_subtitle_and_border() =
        rule.verifyScreenshot(testName, screenshotRule) {
            TestNonClickableTitleCard(
                hasSubtitle = true,
                hasTime = true,
                hasContent = true,
                hasBorder = true,
            )
        }

    @Test
    fun title_card_image_background() =
        rule.verifyScreenshot(testName, screenshotRule) {
            TestNonClickableTitleCardWithContainerPainter(
                image =
                    painterResource(
                        id = androidx.wear.compose.material3.test.R.drawable.backgroundimage1
                    ),
                sizeToIntrinsics = false,
            )
        }

    @Composable
    private fun TestNonClickableCard(
        colors: CardColors = CardDefaults.cardColors(),
        contentPadding: PaddingValues = CardDefaults.ContentPadding,
    ) {
        Card(
            colors = colors,
            contentPadding = contentPadding,
            modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max),
        ) {
            Text("Card: Some body content")
        }
    }

    @Composable
    fun TestNonClickableCardWithContainerPainter(
        image: Painter,
        sizeToIntrinsics: Boolean,
        contentPadding: PaddingValues = CardDefaults.CardWithContainerPainterContentPadding,
        colors: CardColors = CardDefaults.cardWithContainerPainterColors(),
    ) {
        Card(
            containerPainter =
                CardDefaults.containerPainter(image = image, sizeToIntrinsics = sizeToIntrinsics),
            colors = colors,
            contentPadding = contentPadding,
            modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max),
        ) {
            Text("Card: Some body content")
        }
    }

    @Composable
    private fun TestNonClickableAppCard(
        hasBodyImage: Boolean = false,
        colors: CardColors = CardDefaults.cardColors(),
        contentPadding: PaddingValues = CardDefaults.ContentPadding,
        borderStroke: BorderStroke? = null,
    ) {
        AppCard(
            appName = { Text("AppName") },
            appImage = { TestIcon() },
            title = { Text("AppCard") },
            colors = colors,
            time = { Text("now") },
            contentPadding = contentPadding,
            border = borderStroke,
            modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max),
        ) {
            if (hasBodyImage) {
                Text("Some body content and some more body content and an image")
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        modifier =
                            Modifier.weight(1f)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(16.dp)),
                        painter = painterResource(id = R.drawable.card_content_image),
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                    )
                }
            } else {
                Text("Some body content and some more body content")
            }
        }
    }

    @Composable
    fun TestNonClickableTitleCardWithContainerPainter(
        image: Painter,
        sizeToIntrinsics: Boolean,
        title: String = "TitleCard",
        time: String = "now",
        contentPadding: PaddingValues = CardDefaults.CardWithContainerPainterContentPadding,
        colors: CardColors = CardDefaults.cardWithContainerPainterColors(),
    ) {
        TitleCard(
            containerPainter =
                CardDefaults.containerPainter(image = image, sizeToIntrinsics = sizeToIntrinsics),
            title = { Text(title) },
            time = { Text(time) },
            colors = colors,
            contentPadding = contentPadding,
            modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max),
        ) {
            Text("Some body content and some more body content")
        }
    }

    @Composable
    private fun TestNonClickableTitleCard(
        hasContent: Boolean = true,
        hasTime: Boolean = false,
        hasSubtitle: Boolean = false,
        hasBorder: Boolean = false,
        contentPadding: PaddingValues = CardDefaults.ContentPadding,
        useIntrinsicWidth: Boolean = false,
        colors: CardColors = CardDefaults.cardColors(),
    ) {
        val timeComposable: @Composable (() -> Unit) = { Text("now") }
        val subtitleComposable: @Composable ((ColumnScope) -> Unit) = { Text("Subtitle") }
        val contentComposable: @Composable (() -> Unit) = {
            Text("Some body content and some more body content")
        }
        TitleCard(
            title = { Text("TitleCard") },
            subtitle = if (hasSubtitle) subtitleComposable else null,
            time = if (hasTime) timeComposable else null,
            colors = colors,
            border = if (hasBorder) BorderStroke(4.dp, Color.Red) else null,
            contentPadding = contentPadding,
            modifier =
                Modifier.testTag(TEST_TAG).run {
                    if (useIntrinsicWidth) width(IntrinsicSize.Max) else this
                },
            content = if (hasContent) contentComposable else null,
        )
    }
}
