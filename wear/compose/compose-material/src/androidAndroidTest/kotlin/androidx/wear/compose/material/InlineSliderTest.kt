/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

@ExperimentalWearMaterialApi
class InlineSliderTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            InlineSlider(
                value = 1f,
                onValueChange = {},
                steps = 5,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun coerces_value_top_limit() {
        val state = mutableStateOf(4f)

        rule.setContentWithTheme {
            InlineSlider(
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 0f..10f,
                steps = 4,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.runOnIdle {
            state.value = 20f
        }
        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(10f, 0f..10f, 4))
    }

    @Test
    fun coerces_value_lower_limit() {
        val state = mutableStateOf(4f)

        rule.setContentWithTheme {
            InlineSlider(
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 0f..10f,
                steps = 4,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.runOnIdle {
            state.value = -20f
        }
        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..10f, 4))
    }

    @Test(expected = IllegalArgumentException::class)
    fun throws_when_steps_negative() {
        rule.setContent {
            InlineSlider(value = 0f, onValueChange = {}, steps = -1)
        }
    }

    @Test
    fun snaps_value_exactly() {
        val state = mutableStateOf(0f)
        val range = 0f..1f

        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                steps = 4,
                valueRange = range
            )
        }

        rule.runOnUiThread {
            state.value = 0.6f
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.6f, range, 4))
    }

    @Test
    fun snaps_value_to_previous() {
        val state = mutableStateOf(0f)
        val range = 0f..1f

        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                steps = 4,
                valueRange = range
            )
        }

        rule.runOnUiThread {
            state.value = 0.65f
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.6f, range, 4))
    }

    @Test
    fun snaps_value_to_next() {
        val state = mutableStateOf(0f)
        val range = 0f..1f

        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                steps = 4,
                valueRange = range
            )
        }

        rule.runOnUiThread {
            state.value = 0.55f
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.6f, range, 4))
    }
}
