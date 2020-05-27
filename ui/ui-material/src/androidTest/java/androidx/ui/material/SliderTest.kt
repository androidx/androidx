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

import androidx.compose.mutableStateOf
import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.layout.DpConstraints
import androidx.ui.test.assertValueEquals
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.runOnUiThread
import androidx.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class SliderTest {
    private val tag = "slider"

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun sliderPosition_valueCoercion() {
        val state = mutableStateOf(0f)
        composeTestRule.setContent {
            Slider(
                modifier = Modifier.testTag(tag),
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 0f..1f
            )
        }
        runOnIdleCompose {
            state.value = 2f
        }
        findByTag(tag).assertValueEquals("1.0")
        runOnIdleCompose {
            state.value = -123145f
        }
        findByTag(tag).assertValueEquals("0.0")
    }

    @Test(expected = IllegalArgumentException::class)
    fun sliderPosition_stepsThrowWhenLessThanZero() {
        composeTestRule.setContent {
            Slider(value = 0f, onValueChange = {}, steps = -1)
        }
    }

    @Test
    fun slider_semantics() {
        val state = mutableStateOf(0f)

        composeTestRule
            .setMaterialContent {
                Slider(modifier = Modifier.testTag(tag), value = state.value,
                    onValueChange = { state.value = it })
            }

        findByTag(tag)
            .assertValueEquals("0.0")

        runOnUiThread {
            state.value = 0.5f
        }

        findByTag(tag)
            .assertValueEquals("0.5")
    }

    @Test
    fun slider_sizes() {
        val state = mutableStateOf(0f)
        composeTestRule
            .setMaterialContentAndCollectSizes(
                parentConstraints = DpConstraints(maxWidth = 100.dp, maxHeight = 100.dp)
            ) { Slider(value = state.value, onValueChange = { state.value = it }) }
            .assertHeightEqualsTo(48.dp)
            .assertWidthEqualsTo(100.dp)
    }
}