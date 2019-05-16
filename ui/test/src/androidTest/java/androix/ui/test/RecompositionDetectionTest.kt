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

package androix.ui.test

import androidx.compose.composer
import androidx.test.filters.MediumTest
import androidx.ui.baseui.selection.ToggleableState
import androidx.ui.core.TestTag
import androidx.ui.material.Checkbox
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import androidx.ui.test.android.AndroidSemanticsTreeInteraction
import androidx.ui.test.assertIsChecked
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class RecompositionDetectionTest {

    @get:Rule
    val composeTestRule = createComposeRule(
        disableTransitions = true,
        throwOnRecomposeTimeout = true // This makes sure that any timeout will cause crash
    )

    /**
     * This test verifies that our recomposition callback are getting called. This is achieved
     * thanks to 'throwOnRecomposeTimeout'. This helps to make sure that we are actually waiting for
     * callbacks and not just relying on implicit timeout delays.
     */
    @Test
    fun actionShouldTriggerRecomposeAndTimeOutShouldNotHappen() {
        val state = CheckboxState(value = ToggleableState.Unchecked)
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    TestTag(tag = "checkbox") {
                        Checkbox(
                            value = state.value,
                            onClick = { state.toggle() }
                        )
                    }
                }
            }
        }

        val interaction = findByTag("checkbox") as AndroidSemanticsTreeInteraction

        Truth.assertThat(interaction.hadPendingChangesAfterLastAction).isFalse()

        interaction.doClick()

        Truth.assertThat(interaction.hadPendingChangesAfterLastAction).isTrue()

        interaction.assertIsChecked()
    }
}