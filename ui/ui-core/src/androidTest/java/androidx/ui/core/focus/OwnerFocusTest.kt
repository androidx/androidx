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

import android.view.View
import androidx.compose.Composable
import androidx.test.filters.SmallTest
import androidx.ui.core.focus.FocusDetailedState.Active
import androidx.ui.core.focus.FocusDetailedState.Inactive
import androidx.ui.core.ViewAmbient
import androidx.ui.foundation.Box
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdleCompose
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class OwnerFocusTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun requestFocus_bringsViewInFocus() {
        // Arrange.
        lateinit var ownerView: View
        val modifier = FocusModifierImpl(Inactive)
        composeTestRule.setFocusableContent {
            ownerView = getOwner()
            Box(modifier = modifier)
        }

        // Act.
        runOnIdleCompose {
            modifier.requestFocus()
        }

        // Assert.
        runOnIdleCompose {
            assertThat(ownerView.isFocused).isTrue()
        }
    }

    @Ignore("Enable this test after the owner propagates focus to the hierarchy (b/152535715)")
    @Test
    fun whenOwnerGainsFocus_focusModifiersAreUpdated() {
        // Arrange.
        lateinit var ownerView: View
        val modifier = FocusModifierImpl(Inactive)
        composeTestRule.setFocusableContent {
            ownerView = getOwner()
            Box(modifier = modifier)
        }

        // Act.
        runOnIdleCompose {
            ownerView.requestFocus()
        }

        // Assert.
        runOnIdleCompose {
            assertThat(modifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Ignore("Enable this test after the owner propagates focus to the hierarchy (b/152535715)")
    @Test
    fun whenWindowGainsFocus_focusModifiersAreUpdated() {
        // Arrange.
        lateinit var ownerView: View
        val modifier = FocusModifierImpl(Inactive)
        composeTestRule.setFocusableContent {
            ownerView = getOwner()
            Box(modifier = modifier)
        }

        // Act.
        runOnIdleCompose {
            ownerView.dispatchWindowFocusChanged(true)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(modifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Test
    fun whenOwnerLosesFocus_focusModifiersAreUpdated() {
        // Arrange.
        lateinit var ownerView: View
        val modifier = FocusModifierImpl(Inactive)
        composeTestRule.setFocusableContent {
            ownerView = getOwner()
            Box(modifier = modifier)
        }
        runOnIdleCompose {
            modifier.requestFocus()
        }

        // Act.
        runOnIdleCompose {
            ownerView.clearFocus()
        }

        // Assert.
        runOnIdleCompose {
            assertThat(modifier.focusDetailedState).isEqualTo(Inactive)
        }
    }

    @Test
    fun whenWindowLosesFocus_focusStateIsUnchanged() {
        // Arrange.
        lateinit var ownerView: View
        lateinit var modifier: FocusModifier
        composeTestRule.setFocusableContent {
            ownerView = getOwner()
            modifier = FocusModifier()
            Box(modifier = modifier)
        }
        runOnIdleCompose {
            modifier.requestFocus()
        }

        // Act.
        runOnIdleCompose {
            ownerView.dispatchWindowFocusChanged(false)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(modifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Composable
    private fun getOwner() = ViewAmbient.current
}
