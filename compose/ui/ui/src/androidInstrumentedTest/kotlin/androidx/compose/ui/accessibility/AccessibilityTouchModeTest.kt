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

package androidx.compose.ui.accessibility

import android.view.View
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.resetInTouchModeCompat
import androidx.compose.ui.focus.setFocusableContent
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semanticsId
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_FOCUS
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class AccessibilityTouchModeTest(private val param: Param) {

    @get:Rule val rule = createComposeRule()

    private lateinit var inputModeManager: InputModeManager
    private lateinit var view: View

    @After
    fun resetTouchMode() = InstrumentationRegistry.getInstrumentation().resetInTouchModeCompat()

    @Test
    fun requestFocus_doesNotChangeInputMode() {
        // Arrange.
        rule.setTestContent { Box(Modifier.size(10.dp).testTag("item").focusable()) }

        // Act.
        rule.onNodeWithTag("item").requestFocus()

        // Assert.
        rule.onNodeWithTag("item").assertIsFocused()
        assertThat(inputModeManager.inputMode).isEqualTo(param.inputMode)
    }

    @Test
    fun accessibilityActionFocus_putsSystemInKeyboardMode() {
        // Arrange.
        rule.setTestContent { Box(Modifier.size(10.dp).testTag("item").focusable()) }
        val semanticsId = rule.onNodeWithTag("item").semanticsId()

        // Act.
        rule.runOnUiThread {
            view.accessibilityNodeProvider.performAction(semanticsId, ACTION_FOCUS, null)
        }

        // Assert.
        rule.onNodeWithTag("item").assertIsFocused()
        assertThat(inputModeManager.inputMode).isEqualTo(Keyboard)
    }

    @Test
    fun accessibilityActionFocus_putsSystemInKeyboardModeWhenViewWasFocused() {
        // Arrange.
        rule.setTestContent { Box(Modifier.size(10.dp).testTag("item").focusable()) }
        val semanticsId = rule.onNodeWithTag("item").semanticsId()
        rule.runOnUiThread { view.requestFocus() }
        assertThat(view.isFocused).isTrue()

        // Act.
        rule.runOnUiThread {
            view.accessibilityNodeProvider.performAction(semanticsId, ACTION_FOCUS, null)
        }

        // Assert.
        rule.onNodeWithTag("item").assertIsFocused()
        assertThat(inputModeManager.inputMode).isEqualTo(Keyboard)
    }

    @Test
    fun accessibilityActionFocus_itemFocusableInTouchMode() {
        // Arrange.
        rule.setTestContent {
            Box(
                Modifier.size(10.dp)
                    .testTag("item")
                    .focusProperties { canFocus = inputModeManager.inputMode == Touch }
                    .focusable()
            )
        }
        val semanticsId = rule.onNodeWithTag("item").semanticsId()

        // Act.
        rule.runOnUiThread {
            view.accessibilityNodeProvider.performAction(semanticsId, ACTION_FOCUS, null)
        }

        // Assert.
        rule.onNodeWithTag("item").assertIsNotFocused()
        assertThat(inputModeManager.inputMode).isEqualTo(Keyboard)
    }

    @Test
    fun accessibilityActionFocus_itemFocusableInKeyboardMode() {
        // Arrange.
        rule.setTestContent {
            Box(
                Modifier.size(10.dp)
                    .testTag("item")
                    .focusProperties { canFocus = inputModeManager.inputMode == Keyboard }
                    .focusable()
            )
        }
        val semanticsId = rule.onNodeWithTag("item").semanticsId()

        // Act.
        rule.runOnUiThread {
            view.accessibilityNodeProvider.performAction(semanticsId, ACTION_FOCUS, null)
        }

        // Assert.
        rule.onNodeWithTag("item").assertIsFocused()
        assertThat(inputModeManager.inputMode).isEqualTo(Keyboard)
    }

    private fun ComposeContentTestRule.setTestContent(content: @Composable () -> Unit) {
        when (param.inputMode) {
            Touch -> InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
            Keyboard -> InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
        }
        setFocusableContent {
            inputModeManager = LocalInputModeManager.current
            view = LocalView.current
            content()
        }
        runOnIdle {
            // SetInTouchMode does not work on some devices. Skip this test for those devices.
            assumeTrue("input mode check", inputModeManager.inputMode == param.inputMode)
        }
    }

    // We need to wrap the inline class parameter in another class because Java can't instantiate
    // the inline class.
    data class Param(val inputMode: InputMode)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = arrayOf(Param(Touch), Param(Keyboard))
    }
}
