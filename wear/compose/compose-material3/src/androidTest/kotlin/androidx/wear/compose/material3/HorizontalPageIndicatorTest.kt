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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

@RequiresApi(Build.VERSION_CODES.O)
class HorizontalPageIndicatorTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    public fun supports_testtag_circular() {
        rule.setContentWithTheme {
            HorizontalPageIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                pageIndicatorState = pageIndicatorState(),
                indicatorStyle = PageIndicatorStyle.Curved
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    public fun supports_testtag_linear() {
        rule.setContentWithTheme {
            HorizontalPageIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                pageIndicatorState = pageIndicatorState(),
                indicatorStyle = PageIndicatorStyle.Linear
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    public fun position_is_selected_circular() {
        position_is_selected(PageIndicatorStyle.Curved)
    }

    @Test
    public fun position_is_selected_linear() {
        position_is_selected(PageIndicatorStyle.Linear)
    }

    @Test
    public fun in_between_positions_circular() {
        in_between_positions(PageIndicatorStyle.Curved)
    }

    @Test
    public fun in_between_positions_linear() {
        in_between_positions(PageIndicatorStyle.Linear)
    }

    private fun position_is_selected(indicatorStyle: PageIndicatorStyle) {
        rule.setContentWithTheme {
            HorizontalPageIndicator(
                modifier = Modifier
                    .testTag(TEST_TAG)
                    .size(150.dp),
                indicatorStyle = indicatorStyle,
                pageIndicatorState = pageIndicatorState(),
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                indicatorSize = 20.dp
            )
        }
        rule.waitForIdle()

        // A selected dot with specified color should be visible on the screen, which is apprx 1.3%
        // (1.3% per dot, 1 dot in total)
        rule.onNodeWithTag(TEST_TAG).captureToImage()
            .assertColorInPercentageRange(selectedColor, 1.2f..1.5f)
        // Unselected dots should also be visible on the screen, and should take around 4%
        // (1.3% per dot, 3 dots total)
        rule.onNodeWithTag(TEST_TAG).captureToImage()
            .assertColorInPercentageRange(unselectedColor, 3.8f..4.5f)
    }

    private fun in_between_positions(indicatorStyle: PageIndicatorStyle) {
        rule.setContentWithTheme {
            HorizontalPageIndicator(
                modifier = Modifier
                    .testTag(TEST_TAG)
                    .size(150.dp)
                    .fillMaxWidth(),
                pageIndicatorState = pageIndicatorState(pageOffset = 0.5f),
                indicatorStyle = indicatorStyle,
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                indicatorSize = 20.dp
            )
        }
        rule.waitForIdle()

        // Selected color should occupy 2 dots with space in between, which
        // approximately equals to 3.5%
        rule.onNodeWithTag(TEST_TAG).captureToImage()
            .assertColorInPercentageRange(selectedColor, 3f..4f)
        // Unselected dots ( which doesn't participate in color merge)
        // should also be visible on the screen, and should take around 2.7%
        // (1.3% per dot, 2 dots in total)
        rule.onNodeWithTag(TEST_TAG).captureToImage()
            .assertColorInPercentageRange(unselectedColor, 2.5f..3f)
    }

    companion object {
        val selectedColor = Color.Yellow
        val unselectedColor = Color.Red

        fun pageIndicatorState(
            pageOffset: Float = 0f,
            selectedPage: Int = 1,
            pageCount: Int = 4
        ) = object : PageIndicatorState {
            override val selectedPageWithOffset: () -> Float
                get() = { selectedPage + pageOffset }
            override val pageCount: Int
                get() = pageCount
        }
    }
}
