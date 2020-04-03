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
import androidx.ui.focus.FocusDetailedState.Active
import androidx.ui.focus.FocusDetailedState.ActiveParent
import androidx.ui.focus.FocusDetailedState.Captured
import androidx.ui.focus.FocusDetailedState.Disabled
import androidx.ui.focus.FocusDetailedState.Inactive
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
class CaptureFocusTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun active_captureFocus_changesStateToCaptured() {
        runOnUiThread {
            // Arrange.
            val focusModifier = createFocusModifier(Active).also {
                composeTestRule.setContent { Box(modifier = it) }
            }

            // Act.
            val success = focusModifier.captureFocus()

            // Assert.
            Truth.assertThat(success).isTrue()
            Truth.assertThat(focusModifier.focusDetailedState).isEqualTo(Captured)
        }
    }

    @Test
    fun activeParent_captureFocus_retainsStateAsActiveParent() {
        runOnUiThread {
            // Arrange.
            val focusModifier = createFocusModifier(ActiveParent).also {
                composeTestRule.setContent { Box(modifier = it) }
            }

            // Act.
            val success = focusModifier.captureFocus()

            // Assert.
            Truth.assertThat(success).isFalse()
            Truth.assertThat(focusModifier.focusDetailedState).isEqualTo(ActiveParent)
        }
    }

    @Test
    fun captured_captureFocus_retainsStateAsCaptured() {
        runOnUiThread {
            // Arrange.
            val focusModifier = createFocusModifier(Captured).also {
                composeTestRule.setContent { Box(modifier = it) }
            }

            // Act.
            val success = focusModifier.captureFocus()

            // Assert.
            Truth.assertThat(success).isTrue()
            Truth.assertThat(focusModifier.focusDetailedState).isEqualTo(Captured)
        }
    }

    @Test
    fun disabled_captureFocus_retainsStateAsDisabled() {
        runOnUiThread {
            // Arrange.
            val focusModifier = createFocusModifier(Disabled).also {
                composeTestRule.setContent { Box(modifier = it) }
            }

            // Act.
            val success = focusModifier.captureFocus()

            // Assert.
            Truth.assertThat(success).isFalse()
            Truth.assertThat(focusModifier.focusDetailedState).isEqualTo(Disabled)
        }
    }

    @Test
    fun inactive_captureFocus_retainsStateAsInactive() {
        runOnUiThread {
            // Arrange.
            val focusModifier = createFocusModifier(Inactive).also {
                composeTestRule.setContent { Box(modifier = it) }
            }

            // Act.
            val success = focusModifier.captureFocus()

            // Assert.
            Truth.assertThat(success).isFalse()
            Truth.assertThat(focusModifier.focusDetailedState).isEqualTo(Inactive)
        }
    }
}