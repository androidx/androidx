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

package androidx.compose.ui.focus

import android.view.View
import android.view.View.FOCUS_DOWN
import android.view.View.FOCUS_UP
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class OwnerFocusTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun requestFocus_bringsViewInFocus() {
        // Arrange.
        lateinit var ownerView: View
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            ownerView = LocalView.current
            Box(modifier = Modifier.focusRequester(focusRequester).focusTarget())
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(ownerView.isFocused).isTrue() }
    }

    @Ignore("b/325466015")
    @Test
    fun whenOwnerGainsFocus_focusModifiersAreUpdated() {
        // Arrange.
        lateinit var ownerView: View
        lateinit var focusState: FocusState
        rule.setFocusableContent(extraItemForInitialFocus = false) {
            ownerView = LocalView.current
            Box(modifier = Modifier.onFocusChanged { focusState = it }.focusTarget())
        }

        // Act.
        rule.runOnIdle { ownerView.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isTrue() }
    }

    @Test
    fun callingRequestFocusDownWhenOwnerAlreadyHasFocus() {
        // Arrange.
        lateinit var ownerView: View
        lateinit var focusState1: FocusState
        lateinit var focusState2: FocusState
        lateinit var focusState3: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent(extraItemForInitialFocus = false) {
            ownerView = LocalView.current
            Column {
                Box(
                    modifier =
                        Modifier.size(10.dp).onFocusChanged { focusState1 = it }.focusTarget()
                )
                Box(
                    modifier =
                        Modifier.size(10.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState2 = it }
                            .focusTarget()
                )
                Box(
                    modifier =
                        Modifier.size(10.dp).onFocusChanged { focusState3 = it }.focusTarget()
                )
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        val focusRequested = rule.runOnIdle { ownerView.requestFocus(FOCUS_DOWN) }

        // Assert.
        rule.runOnIdle {
            assertThat(focusRequested).isTrue()
            assertThat(focusState1.isFocused).isFalse()
            assertThat(focusState2.isFocused).isTrue()
            assertThat(focusState3.isFocused).isFalse()
        }
    }

    @Test
    fun callingRequestFocusUpWhenOwnerAlreadyHasFocus() {
        // Arrange.
        lateinit var ownerView: View
        lateinit var focusState1: FocusState
        lateinit var focusState2: FocusState
        lateinit var focusState3: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent(extraItemForInitialFocus = false) {
            ownerView = LocalView.current
            Column {
                Box(
                    modifier =
                        Modifier.size(10.dp).onFocusChanged { focusState1 = it }.focusTarget()
                )
                Box(
                    modifier =
                        Modifier.size(10.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState2 = it }
                            .focusTarget()
                )
                Box(
                    modifier =
                        Modifier.size(10.dp).onFocusChanged { focusState3 = it }.focusTarget()
                )
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        val focusRequested = rule.runOnIdle { ownerView.requestFocus(FOCUS_UP) }

        // Assert.
        rule.runOnIdle {
            assertThat(focusRequested).isTrue()
            assertThat(focusState1.isFocused).isFalse()
            assertThat(focusState2.isFocused).isTrue()
            assertThat(focusState3.isFocused).isFalse()
        }
    }

    @Ignore("Enable this test after the owner propagates focus to the hierarchy (b/152535715)")
    @Test
    fun whenWindowGainsFocus_focusModifiersAreUpdated() {
        // Arrange.
        lateinit var ownerView: View
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            ownerView = LocalView.current
            Box(
                modifier =
                    Modifier.onFocusChanged { focusState = it }
                        .focusRequester(focusRequester)
                        .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle { ownerView.dispatchWindowFocusChanged(true) }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isTrue() }
    }

    @Test
    fun whenOwnerLosesFocus_focusModifiersAreUpdated() {
        // Arrange.
        lateinit var ownerView: View
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            ownerView = LocalView.current
            Box(
                modifier =
                    Modifier.size(10.dp)
                        .onFocusChanged { focusState = it }
                        .focusRequester(focusRequester)
                        .focusTarget()
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        rule.runOnIdle { ownerView.clearFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun whenWindowLosesFocus_focusStateIsUnchanged() {
        // Arrange.
        lateinit var ownerView: View
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            ownerView = LocalView.current
            Box(
                modifier =
                    Modifier.onFocusChanged { focusState = it }
                        .focusRequester(focusRequester)
                        .focusTarget()
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        rule.runOnIdle { ownerView.dispatchWindowFocusChanged(false) }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isTrue() }
    }

    @Test
    fun viewDoesNotTakeFocus_whenThereAreNoFocusableItems() {
        // Arrange.
        lateinit var ownerView: View
        rule.setFocusableContent(extraItemForInitialFocus = false) {
            ownerView = LocalView.current
            Box {}
        }

        // Act.
        val success = rule.runOnIdle { ownerView.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isFalse()
            assertThat(ownerView.isFocused).isFalse()
        }
    }

    @Ignore("b/325466015")
    @Test
    fun clickingOnNonClickableSpaceInAppWhenViewIsFocused_doesNotChangeViewFocus() {
        // Arrange.
        val nonClickable = "notClickable"
        var didViewFocusChange = false
        lateinit var ownerView: View
        rule.setFocusableContent {
            ownerView = LocalView.current
            Box(Modifier.testTag(nonClickable))
        }
        rule.runOnIdle {
            ownerView.requestFocus()
            assertThat(ownerView.isFocused).isTrue()
        }
        ownerView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                didViewFocusChange = true
            }
        }

        // Act.
        rule.onNodeWithTag(nonClickable).performClick()

        // Assert.
        rule.runOnIdle {
            assertThat(didViewFocusChange).isFalse()
            assertThat(ownerView.isFocused).isTrue()
        }
    }

    @Test
    fun clickingOnNonClickableSpaceInAppWhenViewIsNotFocused_doesNotChangeViewFocus() {
        // Arrange.
        val nonClickable = "notClickable"
        var didViewFocusChange = false
        lateinit var ownerView: View
        rule.setFocusableContent {
            ownerView = LocalView.current
            Box(Modifier.testTag(nonClickable))
        }
        rule.runOnIdle { assertThat(ownerView.isFocused).isFalse() }
        ownerView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                didViewFocusChange = true
            }
        }

        // Act.
        rule.onNodeWithTag(nonClickable).performClick()

        // Assert.
        rule.runOnIdle {
            assertThat(didViewFocusChange).isFalse()
            assertThat(ownerView.isFocused).isFalse()
        }
    }
}
