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

package androidx.compose.ui.focus

import android.os.Build
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
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
class FocusTransactionsTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun reentrantRequestFocus_byCallingRequestFocusWithinOnFocusChanged() {
        // Arrange.
        val (item1, item2) = FocusRequester.createRefs()
        var (item1Focused, item2Focused) = List(2) { false }
        var requestingFocusOnItem2 = false
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(item1)
                    .onFocusChanged {
                        item1Focused = it.isFocused
                        if (!item1Focused && requestingFocusOnItem2) {
                            // While losing focus, we trigger a re-entrant request focus. We expect
                            // the focus transaction manager to cancel the previous request focus
                            // before performing this requestFocus() call. Before introducing the
                            // focus transaction system this would cause a crash (b/275633128).
                            item2.requestFocus()
                        }
                    }
                    .focusTarget()
            )
            Box(
                Modifier.focusRequester(item2)
                    .onFocusChanged { item2Focused = it.isFocused }
                    .focusTarget()
            )
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        rule.runOnIdle {
            requestingFocusOnItem2 = true
            item2.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isFalse()
            assertThat(item2Focused).isTrue()
        }
    }

    @Test
    fun reentrantRequestFocus_byCallingRequestFocusWithinOnFocusChanged2() {
        // Arrange.
        val (item1, item2) = FocusRequester.createRefs()
        var (item1Focused, item2Focused) = List(2) { false }
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(item1)
                    .onFocusChanged {
                        item1Focused = it.isFocused
                        if (item1Focused) item2.requestFocus()
                    }
                    .focusTarget()
            )
            Box(
                Modifier.focusRequester(item2)
                    .onFocusChanged { item2Focused = it.isFocused }
                    .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle { item1.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isFalse()
            assertThat(item2Focused).isTrue()
        }
    }

    @Test
    fun cancelTakeFocus_fromOnFocusChanged() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var inputModeManager: InputModeManager
        lateinit var view: View
        lateinit var focusState1: FocusState
        lateinit var focusState2: FocusState
        lateinit var focusState3: FocusState
        val box = FocusRequester()

        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            inputModeManager = LocalInputModeManager.current
            view = LocalView.current
            Box(
                Modifier.size(10.dp)
                    .focusRequester(box)
                    .onFocusChanged { focusState1 = it }
                    .onFocusChanged {
                        focusState2 = it
                        if (it.isFocused) focusManager.clearFocus()
                    }
                    .onFocusChanged { focusState3 = it }
                    .focusTarget()
            )
        }

        // Act.
        rule.runOnUiThread { box.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState1).isEqualTo(Inactive)
            assertThat(focusState2).isEqualTo(Inactive)
            assertThat(focusState3).isEqualTo(Inactive)

            val root = view as AndroidComposeView

            when (inputModeManager.inputMode) {
                Keyboard -> {
                    assertThat(root.focusOwner.rootState).isEqualTo(ActiveParent)
                    assertThat(view.isFocused).isTrue()
                }
                Touch -> {
                    // On devices pre-P, clearFocus() will cause a subsequent requestFocus()
                    // the causes another request for focus on the ComposeView.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        assertThat(root.focusOwner.rootState).isEqualTo(ActiveParent)
                        assertThat(view.isFocused).isTrue()
                    } else {
                        assertThat(root.focusOwner.rootState).isEqualTo(Inactive)
                        assertThat(view.isFocused).isFalse()
                    }
                }
                else -> error("invalid input mode")
            }
        }
    }

    @Test
    fun cancelTakeFocus_fromCustomEnter() {
        // Arrange.
        lateinit var view: View
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            view = LocalView.current
            Box(Modifier.focusProperties { enter = { Cancel } }.focusTarget()) {
                Box(Modifier.focusRequester(focusRequester).focusTarget())
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            val root = view as AndroidComposeView
            assertThat(root.focusOwner.rootState).isEqualTo(Inactive)
            assertThat(view.isFocused).isFalse()
        }
    }

    @Ignore("b/325466015")
    @Test
    fun rootFocusNodeHasFocusWhenViewIsFocused() {
        lateinit var view: View
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            view = LocalView.current
            Box(Modifier.focusRequester(focusRequester).focusTarget())
        }

        // Act.
        rule.runOnIdle { view.requestFocus() }

        // Assert.
        val root = view as AndroidComposeView
        rule.runOnIdle {
            assertThat(root.focusOwner.rootState).isEqualTo(ActiveParent)
            assertThat(view.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle {
            // Do something that causes the previous transaction to be cancelled.
            // This should be a no-op because the specified focus target is not captured, but it
            // creates a new transaction which will cancel the previous one.
            focusRequester.freeFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(root.focusOwner.rootState.hasFocus).isEqualTo(true)
            assertThat(view.isFocused).isTrue()
        }
    }
}
