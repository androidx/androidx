/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.ui.material

import CircularProgressIndicator
import LinearProgressIndicator
import androidx.compose.composer
import androidx.compose.Model
import androidx.test.filters.LargeTest
import androidx.ui.core.TestTag
import androidx.ui.core.dp
import androidx.ui.test.assertIsVisible
import androidx.ui.test.assertValueEquals
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Model
private class State {
    var progress = 0f
}

@LargeTest
@RunWith(JUnit4::class)
class ProgressIndicatorTest {

    private val ExpectedLinearWidth = 240.dp
    private val ExpectedLinearHeight = 4.dp

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun determinateLinearProgressIndicator_Progress() {
        val tag = "linear"
        val state = State()

        composeTestRule
            .setMaterialContent {
                TestTag(tag = tag) {
                    LinearProgressIndicator(progress = state.progress)
                }
            }

        findByTag(tag)
            .assertIsVisible()
            .assertValueEquals("0.0")

        composeTestRule.runOnUiThread {
            state.progress = 0.5f
        }

        findByTag(tag)
            .assertIsVisible()
            .assertValueEquals("0.5")
    }

    @Test
    fun determinateLinearProgressIndicator_Size() {
        composeTestRule
            .setMaterialContentAndTestSizes {
                LinearProgressIndicator(progress = 0f)
            }
            .assertWidthEqualsTo(ExpectedLinearWidth)
            .assertHeightEqualsTo(ExpectedLinearHeight)
    }

    @Test
    fun indeterminateLinearProgressIndicator_Size() {
        composeTestRule
            .setMaterialContentAndTestSizes {
                LinearProgressIndicator()
            }
            .assertWidthEqualsTo(ExpectedLinearWidth)
            .assertHeightEqualsTo(ExpectedLinearHeight)
    }

    @Test
    fun determinateCircularProgressIndicator_Progress() {
        val tag = "circular"
        val state = State()

        composeTestRule
            .setMaterialContent {
                TestTag(tag = tag) {
                    CircularProgressIndicator(progress = state.progress)
                }
            }

        findByTag(tag)
            .assertIsVisible()
            .assertValueEquals("0.0")

        composeTestRule.runOnUiThread {
            state.progress = 0.5f
        }

        findByTag(tag)
            .assertIsVisible()
            .assertValueEquals("0.5")
    }

    @Test
    fun determinateCircularProgressIndicator_Size() {
        composeTestRule
            .setMaterialContentAndTestSizes {
                CircularProgressIndicator(progress = 0f)
            }
            .assertIsSquareWithSize { 4.dp.toIntPx() * 2 + 40.dp.toIntPx() }
    }

    @Test
    fun indeterminateCircularProgressIndicator_Size() {
        composeTestRule
            .setMaterialContentAndTestSizes {
                CircularProgressIndicator()
            }
            .assertIsSquareWithSize { 4.dp.toIntPx() * 2 + 40.dp.toIntPx() }
    }
}
