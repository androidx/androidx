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

package androidx.xr.glimmer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.xr.glimmer.samples.CardSample
import androidx.xr.glimmer.samples.CardWithLongText
import androidx.xr.glimmer.samples.CardWithTitleAndActionSample
import androidx.xr.glimmer.samples.CardWithTitleAndHeaderSample
import androidx.xr.glimmer.samples.CardWithTitleAndSubtitleAndLeadingIconAndTrailingIconLongText
import androidx.xr.glimmer.samples.CardWithTitleAndSubtitleAndLeadingIconLongText
import androidx.xr.glimmer.samples.CardWithTitleAndSubtitleAndLeadingIconSample
import androidx.xr.glimmer.samples.CardWithTrailingIconSample
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class CardScreenshotTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    @Test
    fun card() {
        rule.setGlimmerThemeContent { CardSample() }
        rule.assertRootAgainstGolden("card", screenshotRule)
    }

    @Test
    fun card_withLongText() {
        rule.setGlimmerThemeContent { CardWithLongText() }
        rule.assertRootAgainstGolden("card_longText", screenshotRule)
    }

    @Test
    fun card_withTrailingIcon() {
        rule.setGlimmerThemeContent { CardWithTrailingIconSample() }
        rule.assertRootAgainstGolden("card_trailingIcon", screenshotRule)
    }

    @Test
    fun card_withTitleAndSubtitleAndLeadingIcon() {
        rule.setGlimmerThemeContent { CardWithTitleAndSubtitleAndLeadingIconSample() }
        rule.assertRootAgainstGolden("card_titleSubtitleLeadingIcon", screenshotRule)
    }

    @Test
    fun card_withTitleAndHeader() {
        rule.setGlimmerThemeContent { CardWithTitleAndHeaderSample() }
        rule.assertRootAgainstGolden("card_titleHeader", screenshotRule)
    }

    @Test
    fun card_withTitleAndAction() {
        rule.setGlimmerThemeContent { CardWithTitleAndActionSample() }
        rule.assertRootAgainstGolden("card_titleAction", screenshotRule)
    }

    @Test
    fun card_withTitleAndSubtitleAndLeadingIconLongText() {
        rule.setGlimmerThemeContent { CardWithTitleAndSubtitleAndLeadingIconLongText() }
        rule.assertRootAgainstGolden("card_titleSubtitleLeadingIconLongText", screenshotRule)
    }

    @Test
    fun card_withTitleAndSubtitleAndLeadingIconAndTrailingIconLongText() {
        rule.setGlimmerThemeContent {
            CardWithTitleAndSubtitleAndLeadingIconAndTrailingIconLongText()
        }
        rule.assertRootAgainstGolden(
            "card_titleSubtitleLeadingIconTrailingIconLongText",
            screenshotRule,
        )
    }

    @Test
    fun card_focused() {
        rule.mainClock.autoAdvance = false
        rule.setGlimmerThemeContent {
            Card(interactionSource = AlwaysFocusedInteractionSource) { Text("This is a card") }
        }
        // Advance past the animation
        rule.mainClock.advanceTimeBy(10000)
        rule.assertRootAgainstGolden("card_focused", screenshotRule)
    }

    /**
     * Practically a Card cannot be pressed without also being focused, but we test them in
     * isolation as well to make it easier to identify changes. See [card_focused_and_pressed] for
     * the combined state.
     */
    @Test
    fun card_pressed() {
        rule.mainClock.autoAdvance = false
        rule.setGlimmerThemeContent {
            Card(onClick = {}, interactionSource = AlwaysPressedInteractionSource) {
                Text("This is a card")
            }
        }
        // Skip until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)
        rule.assertRootAgainstGolden("card_pressed", screenshotRule)
    }

    @Test
    fun card_focused_and_pressed() {
        rule.mainClock.autoAdvance = false
        rule.setGlimmerThemeContent {
            Card(onClick = {}, interactionSource = AlwaysFocusedAndPressedInteractionSource) {
                Text("This is a card")
            }
        }
        // Advance past the animation
        rule.mainClock.advanceTimeBy(10000)
        rule.assertRootAgainstGolden("card_focused_and_pressed", screenshotRule)
    }
}
