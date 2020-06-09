/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core.focus

import androidx.test.filters.SmallTest
import androidx.ui.core.focus.FocusDetailedState.Active
import androidx.ui.core.focus.FocusDetailedState.ActiveParent
import androidx.ui.core.focus.FocusDetailedState.Captured
import androidx.ui.core.focus.FocusDetailedState.Disabled
import androidx.ui.core.focus.FocusDetailedState.Inactive
import androidx.ui.foundation.Box
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnUiThread
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class FreeFocusTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun active_freeFocus_retainFocusAsActive() {
        runOnUiThread {
            // Arrange.
            val focusModifier = FocusModifierImpl(Active).also {
                composeTestRule.setFocusableContent { Box(modifier = it) }
            }

            // Act.
            val success = focusModifier.freeFocus()

            // Assert.
            Truth.assertThat(success).isFalse()
            Truth.assertThat(focusModifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Test
    fun activeParent_freeFocus_retainFocusAsActiveParent() {
        runOnUiThread {
            // Arrange.
            val focusModifier = FocusModifierImpl(ActiveParent).also {
                composeTestRule.setFocusableContent { Box(modifier = it) }
            }

            // Act.
            val success = focusModifier.freeFocus()

            // Assert.
            Truth.assertThat(success).isFalse()
            Truth.assertThat(focusModifier.focusDetailedState).isEqualTo(ActiveParent)
        }
    }

    @Test
    fun captured_freeFocus_changesStateToActive() {
        runOnUiThread {
            // Arrange.
            val focusModifier = FocusModifierImpl(Captured).also {
                composeTestRule.setFocusableContent { Box(modifier = it) }
            }

            // Act.
            val success = focusModifier.freeFocus()

            // Assert.
            Truth.assertThat(success).isTrue()
            Truth.assertThat(focusModifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Test
    fun disabled_freeFocus_retainFocusAsDisabled() {
        runOnUiThread {
            // Arrange.
            val focusModifier = FocusModifierImpl(Disabled).also {
                composeTestRule.setFocusableContent { Box(modifier = it) }
            }

            // Act.
            val success = focusModifier.freeFocus()

            // Assert.
            Truth.assertThat(success).isFalse()
            Truth.assertThat(focusModifier.focusDetailedState).isEqualTo(Disabled)
        }
    }

    @Test
    fun inactive_freeFocus_retainFocusAsInactive() {
        runOnUiThread {
            // Arrange.
            val focusModifier = FocusModifierImpl(Inactive).also {
                composeTestRule.setFocusableContent { Box(modifier = it) }
            }

            // Act.
            val success = focusModifier.freeFocus()

            // Assert.
            Truth.assertThat(success).isFalse()
            Truth.assertThat(focusModifier.focusDetailedState).isEqualTo(Inactive)
        }
    }
}