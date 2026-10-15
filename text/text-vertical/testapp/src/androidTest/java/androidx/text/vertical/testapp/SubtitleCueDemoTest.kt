/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.text.vertical.testapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the wrapped bounds of the [SubtitleCueDemo] tab in [VerticalTextSampleApp]. */
@RunWith(AndroidJUnit4::class)
class SubtitleCueDemoTest {

    @get:Rule val rule = createComposeRule()

    @Test
    @MediumTest
    fun subtitleCueTab_wrapsHeightToContent() {
        val maxHeightPx = with(rule.density) { SUBTITLE_CUE_MAX_HEIGHT.roundToPx() }
        rule.setContent { VerticalTextSampleApp() }

        rule.onNodeWithText("Subtitle Cue").performScrollTo().performClick()

        val cueSize = rule.onNodeWithTag(SUBTITLE_CUE_TEXT_TAG).fetchSemanticsNode().size
        assertThat(cueSize.width).isGreaterThan(0)
        assertThat(cueSize.height).isGreaterThan(0)
        assertThat(cueSize.height).isLessThan(maxHeightPx)
    }

    @Test
    @MediumTest
    fun subtitleCueTab_horizontalCue_wrapsWidthToContent() {
        val maxWidthPx = with(rule.density) { SUBTITLE_CUE_MAX_WIDTH.roundToPx() }
        rule.setContent { VerticalTextSampleApp() }

        rule.onNodeWithText("Subtitle Cue").performScrollTo().performClick()

        val cueSize =
            rule
                .onNodeWithTag(SUBTITLE_CUE_HORIZONTAL_TAG)
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .size
        assertThat(cueSize.width).isGreaterThan(0)
        assertThat(cueSize.height).isGreaterThan(0)
        assertThat(cueSize.width).isLessThan(maxWidthPx)
    }
}
