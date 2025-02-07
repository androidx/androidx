/*
 * Copyright 2025 The Android Open Source Project
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
import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.node.requireSemanticsInfo
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semanticsId
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class FocusListenerTest {
    @get:Rule val rule = createComposeRule()

    @OptIn(ExperimentalComposeUiApi::class)
    private val previousFlagValue = ComposeUiFlags.isSemanticAutofillEnabled

    // When we clear focus on Pre P devices, request focus is called even when we are
    // in touch mode.
    // https://developer.android.com/about/versions/pie/android-9.0-changes-28#focus
    private val initialFocusAfterClearFocus = SDK_INT < Build.VERSION_CODES.P

    @Before
    fun enableAutofill() {
        @OptIn(ExperimentalComposeUiApi::class)
        ComposeUiFlags.isSemanticAutofillEnabled = true
    }

    @After
    fun disableAutofill() {
        @OptIn(ExperimentalComposeUiApi::class)
        ComposeUiFlags.isSemanticAutofillEnabled = previousFlagValue
    }

    @Test
    fun nothingFocused() {
        // Arrange.
        val listener = TestFocusListener()
        rule.setContent(listener) { Box(Modifier.size(10.dp).focusable()) }

        // Assert.
        rule.runOnIdle { assertThat(listener.events).isEmpty() }
    }

    @Test
    fun firstItemFocused() {
        // Arrange.
        val listener = TestFocusListener()
        rule.setContent(listener) { Box(Modifier.size(10.dp).testTag("item").focusable()) }
        val itemId = rule.onNodeWithTag("item").semanticsId()

        // Act.
        rule.onNodeWithTag("item").requestFocus()

        // Assert.
        rule.runOnIdle {
            assertThat(listener).isEqualTo(TestFocusListener(mutableListOf(Pair(null, itemId))))
        }
    }

    @Test
    fun firstItemUnFocused() {
        // Arrange.
        val listener = TestFocusListener()
        lateinit var focusManager: FocusManager
        lateinit var inputModeManager: InputModeManager
        rule.setContent(listener) {
            inputModeManager = LocalInputModeManager.current
            focusManager = LocalFocusManager.current
            Box(Modifier.size(10.dp).testTag("item").focusable())
        }
        val itemId = rule.onNodeWithTag("item").semanticsId()
        rule.onNodeWithTag("item").requestFocus()
        rule.runOnIdle { listener.reset() }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(listener)
                .isEqualTo(
                    TestFocusListener(
                        if (
                            initialFocusAfterClearFocus ||
                                inputModeManager.inputMode == InputMode.Keyboard
                        ) {
                            mutableListOf(Pair(itemId, null), Pair(null, itemId))
                        } else {
                            mutableListOf(Pair(itemId, null))
                        }
                    )
                )
        }
    }

    @Test
    fun secondItemFocused() {
        // Arrange.
        val listener = TestFocusListener()
        rule.setContent(listener) {
            Column {
                Box(Modifier.size(10.dp).testTag("item1").focusable())
                Box(Modifier.size(10.dp).testTag("item2").focusable())
            }
        }
        val item1Id = rule.onNodeWithTag("item1").semanticsId()
        val item2Id = rule.onNodeWithTag("item2").semanticsId()
        rule.onNodeWithTag("item1").requestFocus()
        rule.runOnIdle { listener.reset() }

        // Act.
        rule.onNodeWithTag("item2").requestFocus()

        // Assert.
        rule.runOnIdle {
            assertThat(listener)
                .isEqualTo(
                    TestFocusListener(mutableListOf(Pair(item1Id, null), Pair(null, item2Id)))
                )
        }
    }

    @Test
    fun secondItemUnFocused() {
        // Arrange.
        val listener = TestFocusListener()
        lateinit var focusManager: FocusManager
        lateinit var inputModeManager: InputModeManager
        rule.setContent(listener) {
            inputModeManager = LocalInputModeManager.current
            focusManager = LocalFocusManager.current
            Column {
                Box(Modifier.size(10.dp).testTag("item1").focusable())
                Box(Modifier.size(10.dp).testTag("item2").focusable())
            }
        }
        val item1Id = rule.onNodeWithTag("item1").semanticsId()
        val item2Id = rule.onNodeWithTag("item2").semanticsId()
        rule.onNodeWithTag("item1").requestFocus()
        rule.onNodeWithTag("item2").requestFocus()
        rule.runOnIdle { listener.reset() }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(listener)
                .isEqualTo(
                    TestFocusListener(
                        if (
                            initialFocusAfterClearFocus ||
                                inputModeManager.inputMode == InputMode.Keyboard
                        ) {
                            mutableListOf(Pair(item2Id, null), Pair(null, item1Id))
                        } else {
                            mutableListOf(Pair(item2Id, null))
                        }
                    )
                )
        }
    }

    private data class TestFocusListener(
        val events: MutableList<Pair<Int?, Int?>> = mutableListOf<Pair<Int?, Int?>>()
    ) : FocusListener {
        override fun onFocusChanged(
            previous: FocusTargetModifierNode?,
            current: FocusTargetModifierNode?
        ) {
            events +=
                Pair(
                    previous?.requireSemanticsInfo()?.semanticsId,
                    current?.requireSemanticsInfo()?.semanticsId
                )
        }

        fun reset() {
            events.clear()
        }
    }

    private fun ComposeContentTestRule.setContent(
        focusListener: FocusListener,
        content: @Composable (() -> Unit)
    ) {
        setContent {
            val focusOwner = LocalFocusManager.current as FocusOwner
            DisposableEffect(focusOwner, focusListener) {
                focusOwner.listeners += focusListener
                onDispose { focusOwner.listeners -= focusListener }
            }
            content()
        }
    }
}
