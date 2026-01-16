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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
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

@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class CardScreenshotTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun card(
        @TestParameter enabled: EnabledState,
        @TestParameter layoutDirection: LayoutDirection,
    ) = verifyScreenshot(layoutDirection = layoutDirection) { TestCard(enabled = enabled.enabled) }

    @Test
    fun card_image_background() = verifyScreenshot {
        TestCardWithContainerPainter(
            image =
                painterResource(
                    id = androidx.wear.compose.material3.test.R.drawable.backgroundimage1
                ),
            sizeToIntrinsics = false,
        )
    }

    @Test
    fun card_image_background_with_intrinsic_size() = verifyScreenshot {
        TestCardWithContainerPainter(
            image =
                painterResource(
                    id = androidx.wear.compose.material3.test.R.drawable.backgroundimage1
                ),
            sizeToIntrinsics = true,
        )
    }

    @Test
    fun outlined_card(
        @TestParameter enabled: EnabledState,
        @TestParameter layoutDirection: LayoutDirection,
    ) =
        verifyScreenshot(layoutDirection = layoutDirection) {
            OutlinedCard(
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max),
                enabled = enabled.enabled,
            ) {
                Text("Outlined Card: Some body content")
            }
        }

    @Test fun app_card() = verifyScreenshot { TestAppCard() }

    @Test
    fun app_card(
        @TestParameter enabled: EnabledState,
        @TestParameter layoutDirection: LayoutDirection,
    ) =
        verifyScreenshot(layoutDirection = layoutDirection) {
            TestAppCard(enabled = enabled.enabled)
        }

    @Test
    fun app_card_with_border() = verifyScreenshot {
        TestAppCard(borderStroke = BorderStroke(4.dp, Color.Red))
    }

    @Test
    fun app_card_with_content_padding() = verifyScreenshot {
        TestAppCard(contentPadding = PaddingValues(all = 16.dp))
    }

    @Test fun app_card_with_body_image() = verifyScreenshot { TestAppCard(hasBodyImage = true) }

    @Test
    fun title_card(
        @TestParameter enabled: EnabledState,
        @TestParameter layoutDirection: LayoutDirection,
    ) =
        verifyScreenshot(layoutDirection) {
            TestTitleCard(enabled = enabled.enabled, hasTime = true, useIntrinsicWidth = true)
        }

    @Test
    fun title_card(
        @TestParameter hasTime: TimeState,
        @TestParameter hasSubtitle: SubtitleState,
        @TestParameter hasContent: ContentState,
        @TestParameter layoutDirection: LayoutDirection,
    ) =
        verifyScreenshot(layoutDirection = layoutDirection) {
            TestTitleCard(
                hasTime = hasTime.enabled,
                hasSubtitle = hasSubtitle.enabled,
                hasContent = hasContent.enabled,
                enabled = true,
            )
        }

    @Test
    fun title_card_with_content_time_subtitle_and_border() = verifyScreenshot {
        TestTitleCard(hasSubtitle = true, hasTime = true, hasContent = true, hasBorder = true)
    }

    @Test
    fun title_card_image_background() = verifyScreenshot {
        TestTitleCardWithContainerPainter(
            image =
                painterResource(
                    id = androidx.wear.compose.material3.test.R.drawable.backgroundimage1
                ),
            sizeToIntrinsics = false,
        )
    }

    @Composable
    private fun TestCard(
        enabled: Boolean = true,
        colors: CardColors = CardDefaults.cardColors(),
        contentPadding: PaddingValues = CardDefaults.ContentPadding,
    ) {
        Card(
            enabled = enabled,
            onClick = {},
            colors = colors,
            contentPadding = contentPadding,
            modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max),
        ) {
            Text("Card: Some body content")
        }
    }

    @Composable
    fun TestCardWithContainerPainter(
        image: Painter,
        sizeToIntrinsics: Boolean,
        enabled: Boolean = true,
        contentPadding: PaddingValues = CardDefaults.CardWithContainerPainterContentPadding,
        colors: CardColors = CardDefaults.cardWithContainerPainterColors(),
    ) {
        Card(
            containerPainter =
                CardDefaults.containerPainter(image = image, sizeToIntrinsics = sizeToIntrinsics),
            enabled = enabled,
            onClick = {},
            colors = colors,
            contentPadding = contentPadding,
            modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max),
        ) {
            Text("Card: Some body content")
        }
    }

    @Composable
    private fun TestAppCard(
        enabled: Boolean = true,
        hasBodyImage: Boolean = false,
        colors: CardColors = CardDefaults.cardColors(),
        contentPadding: PaddingValues = CardDefaults.ContentPadding,
        borderStroke: BorderStroke? = null,
    ) {
        AppCard(
            enabled = enabled,
            onClick = {},
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
    fun TestTitleCardWithContainerPainter(
        image: Painter,
        sizeToIntrinsics: Boolean,
        title: String = "TitleCard",
        time: String = "now",
        enabled: Boolean = true,
        contentPadding: PaddingValues = CardDefaults.CardWithContainerPainterContentPadding,
        colors: CardColors = CardDefaults.cardWithContainerPainterColors(),
    ) {
        TitleCard(
            containerPainter =
                CardDefaults.containerPainter(image = image, sizeToIntrinsics = sizeToIntrinsics),
            title = { Text(title) },
            time = { Text(time) },
            enabled = enabled,
            onClick = {},
            colors = colors,
            contentPadding = contentPadding,
            modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max),
        ) {
            Text("Some body content and some more body content")
        }
    }

    @Composable
    private fun TestTitleCard(
        hasContent: Boolean = true,
        hasTime: Boolean = false,
        hasSubtitle: Boolean = false,
        hasBorder: Boolean = false,
        enabled: Boolean = true,
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
            enabled = enabled,
            onClick = {},
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

    private fun verifyScreenshot(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit,
    ) {
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    content()
                }
            }
        }

        rule.verifyScreenshot(testName, screenshotRule, generateScreenshots = true)
    }

    enum class SubtitleState(val enabled: Boolean) {
        Subtitle(true),
        NoSubtitle(false),
    }

    enum class TimeState(val enabled: Boolean) {
        Time(true),
        NoTime(false),
    }

    enum class ContentState(val enabled: Boolean) {
        Content(true),
        NoContent(false),
    }

    enum class BorderState(val enabled: Boolean) {
        Border(true),
        NoBorder(false),
    }

    enum class EnabledState(val enabled: Boolean) {
        Enabled(true),
        Disabled(false),
    }
}
