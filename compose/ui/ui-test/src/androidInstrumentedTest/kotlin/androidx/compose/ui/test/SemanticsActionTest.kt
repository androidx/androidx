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

package androidx.compose.ui.test

import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SemanticsActionTest(
    private val action: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>
) {
    @get:Rule val rule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                SemanticsActions.OnClick,
                SemanticsActions.OnLongClick,
                SemanticsActions.OnImeAction,
                SemanticsActions.CopyText,
                SemanticsActions.CutText,
                SemanticsActions.PasteText,
                SemanticsActions.Expand,
                SemanticsActions.Collapse,
                SemanticsActions.Dismiss,
                SemanticsActions.RequestFocus,
                SemanticsActions.PageUp,
                SemanticsActions.PageLeft,
                SemanticsActions.PageDown,
                SemanticsActions.PageRight,
                SemanticsActions.ClearTextSubstitution,
            )
    }

    @Test
    fun action_executesOnMainThread() {
        val testTag = "testTag"
        val wasOnMainThread = AtomicBoolean(false)
        rule.setContent {
            Box(
                Modifier.size(100.dp).testTag(testTag).semantics {
                    this[action] =
                        AccessibilityAction(label = "test action") {
                            wasOnMainThread.set(isOnMainThread())
                            true
                        }
                }
            )
        }
        rule.onNodeWithTag(testTag).performSemanticsAction(action)
        assertTrue(wasOnMainThread.get())
    }

    private fun isOnMainThread(): Boolean = Thread.currentThread() == Looper.getMainLooper().thread
}
