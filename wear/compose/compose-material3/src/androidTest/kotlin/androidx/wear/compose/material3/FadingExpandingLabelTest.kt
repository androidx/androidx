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

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class FadingExpandingLabelTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun fading_expanding_label_supports_testtag() {
        rule.setContentWithTheme {
            FadingExpandingLabel(text = "test", modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun fading_expanding_label_animates_on_text_line_change() {
        var initialHeight = 0f
        var updatedHeight = 0f
        var finalHeight = 0f
        var text = mutableStateOf("test")

        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme(modifier = Modifier.size(SCREEN_SIZE_SMALL.dp)) {
            FadingExpandingLabel(text = text.value, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot().let {
            initialHeight = it.height.value
        }

        // Update text to long value that will take several lines
        text.value = "Really long text that will take several lines."

        // Advance animation a couple frames
        repeat(5) { rule.mainClock.advanceTimeByFrame() }
        rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot().let {
            updatedHeight = it.height.value
        }
        // Verify that text height has increased in animation
        assertThat(updatedHeight).isGreaterThan(initialHeight)

        // Finish animation
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot().let {
            finalHeight = it.height.value
        }
        // Verify that the text height at end of animation has increased
        assertThat(finalHeight).isGreaterThan(updatedHeight)
    }

    @Test
    fun fading_expanding_label_no_animation_on_initial_text() {
        var initialHeight = 0f
        var finalHeight = 0f
        var text = mutableStateOf("test")

        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme(modifier = Modifier.size(SCREEN_SIZE_SMALL.dp)) {
            FadingExpandingLabel(text = text.value, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot().let {
            initialHeight = it.height.value
        }
        // Manually advance time to run any animations
        rule.mainClock.advanceTimeBy(50)
        rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot().let {
            finalHeight = it.height.value
        }

        // Verify that the text height hasn't changed
        assertThat(finalHeight).isEqualTo(initialHeight)
    }
}
