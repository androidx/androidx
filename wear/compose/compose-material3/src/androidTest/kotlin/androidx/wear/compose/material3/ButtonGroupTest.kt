/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class ButtonGroupTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            val interactionSource = remember { MutableInteractionSource() }
            ButtonGroup(modifier = Modifier.testTag(TEST_TAG)) {
                buttonGroupItem(interactionSource) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun two_items_equally_sized_by_default() =
        doTest(
            2,
            expectedWidths = { availableSpace -> arrayOf(availableSpace / 2, availableSpace / 2) }
        )

    @Test
    fun two_items_one_double_size() =
        doTest(
            2,
            expectedWidths = { availableSpace ->
                arrayOf(availableSpace / 3, availableSpace / 3 * 2)
            },
            minWidthsAndWeights = arrayOf(50.dp to 1f, 50.dp to 2f)
        )

    @Test
    fun respects_min_width() =
        doTest(
            2,
            expectedWidths = { availableSpace -> arrayOf(30.dp, availableSpace - 30.dp) },
            size = 100.dp,
            minWidthsAndWeights = arrayOf(30.dp to 1f, 30.dp to 10f)
        )

    @Test
    fun three_equal_buttons() =
        doTest(3, expectedWidths = { availableSpace -> Array(3) { availableSpace / 3 } })

    @Test
    fun three_buttons_one_two_one() =
        doTest(
            3,
            expectedWidths = { availableSpace ->
                arrayOf(availableSpace / 4, availableSpace / 2, availableSpace / 4)
            },
            minWidthsAndWeights = arrayOf(50.dp to 1f, 50.dp to 2f, 50.dp to 1f)
        )

    private fun doTest(
        numItems: Int,
        expectedWidths: (Dp) -> Array<Dp>,
        size: Dp = 300.dp,
        spacing: Dp = 10.dp,
        minWidthsAndWeights: Array<Pair<Dp, Float>> = Array(numItems) { 48.dp to 1f },
    ) {
        val horizontalPadding = 10.dp
        val actualExpectedWidths =
            expectedWidths(size - horizontalPadding * 2 - spacing * (numItems - 1))

        require(numItems == actualExpectedWidths.size)
        require(numItems == minWidthsAndWeights.size)

        rule.setContentWithTheme {
            val interactionSources = remember { Array(numItems) { MutableInteractionSource() } }
            ButtonGroup(
                modifier = Modifier.size(size),
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                spacing = spacing
            ) {
                repeat(numItems) { ix ->
                    buttonGroupItem(
                        interactionSources[ix],
                        minWidth = minWidthsAndWeights[ix].first,
                        weight = minWidthsAndWeights[ix].second
                    ) {
                        Box(
                            modifier =
                                Modifier.testTag(TEST_TAG + (ix + 1).toString()).fillMaxSize(),
                        )
                    }
                }
            }
        }

        repeat(numItems) {
            rule
                .onNodeWithTag(TEST_TAG + (it + 1).toString())
                .assertWidthIsEqualTo(actualExpectedWidths[it])
        }
    }
}
