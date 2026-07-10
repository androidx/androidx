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

package androidx.compose.foundation.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.junit4.ComposeContentTestRule

/**
 * This is needed to pass a WindowInfo CompositionLocal with isWindowFocused == true to nearly all
 * textfield and selection related tests. This helps us doing it in one place instead of defining a
 * new "rule.setTestContent" in every such test file. When a better solution for this problem
 * arrives (b/303841592), this interface should be removed and all "rule.setTextFieldTestContent"
 * calls replaced with "rule.setContent".
 */
interface FocusedWindowTest {
    fun ComposeContentTestRule.setTextFieldTestContent(content: @Composable () -> Unit) {
        val focusedWindowInfo =
            object : WindowInfo {
                override val isWindowFocused = true
            }

        this.setContent {
            CompositionLocalProvider(
                LocalWindowInfo provides focusedWindowInfo,
                LocalInputModeManager provides TouchInputModeManager,
                content = content,
            )
        }
    }
}

/**
 * An [InputModeManager] that is always in [InputMode.Touch] mode.
 *
 * This allows tests to avoid depending on the system's touch mode
 * ([android.view.View.isInTouchMode]).
 */
internal object TouchInputModeManager : InputModeManager {
    override val inputMode: InputMode
        get() = InputMode.Touch

    override fun requestInputMode(inputMode: InputMode) = false
}

/** Provides [TouchInputModeManager] as the [LocalInputModeManager]. */
@Composable
internal fun ForceTouchInputMode(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalInputModeManager provides TouchInputModeManager, content)
}
