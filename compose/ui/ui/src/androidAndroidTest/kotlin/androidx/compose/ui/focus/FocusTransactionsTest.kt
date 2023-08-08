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

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusTransactionsTest {
    @get:Rule
    val rule = createComposeRule()

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun reentrantRequestFocus_byCallingRequestFocusWithinOnFocusChanged() {
        // Arrange.
        val (item1, item2) = FocusRequester.createRefs()
        var (item1Focused, item2Focused) = List(2) { false }
        var requestingFocusOnItem2 = false
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(item1)
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
                Modifier
                    .focusRequester(item2)
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
    fun cancelTakeFocus_fromOnFocusChanged() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var view: View
        val box = FocusRequester()

        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            view = LocalView.current
            Box(
                Modifier
                    .size(10.dp)
                    .focusRequester(box)
                    .onFocusChanged { if (it.isFocused) focusManager.clearFocus() }
                    .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle {
            box.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            val root = view as AndroidComposeView
            val focusOwner = root.focusOwner as FocusOwnerImpl
            assertThat(focusOwner.rootFocusNode.focusState).isEqualTo(Inactive)
            // TODO(b/288096244): Find out why this is flaky.
            //  assertThat(view.isFocused()).isFalse()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun cancelTakeFocus_fromCustomEnter() {
        // Arrange.
        lateinit var view: View
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            view = LocalView.current
            Box(
                Modifier
                    .focusProperties {
                        enter = { Cancel }
                    }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .focusTarget()
                )
            }
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            val root = view as AndroidComposeView
            val focusOwner = root.focusOwner as FocusOwnerImpl
            assertThat(focusOwner.rootFocusNode.focusState).isEqualTo(Inactive)
            assertThat(view.isFocused).isFalse()
        }
    }

    @Test
    fun rootFocusNodeIsActiveWhenViewIsFocused() {
        lateinit var view: View
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            view = LocalView.current
            Box(Modifier.focusRequester(focusRequester).focusTarget())
        }

        // Act.
        rule.runOnIdle {
            view.requestFocus()
        }

        // Assert.
        val root = view as AndroidComposeView
        val focusOwner = root.focusOwner as FocusOwnerImpl
        rule.runOnIdle {
            assertThat(focusOwner.rootFocusNode.focusState).isEqualTo(Active)
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
            assertThat(focusOwner.rootFocusNode.focusState).isEqualTo(Active)
            assertThat(view.isFocused).isTrue()
        }
    }
}
