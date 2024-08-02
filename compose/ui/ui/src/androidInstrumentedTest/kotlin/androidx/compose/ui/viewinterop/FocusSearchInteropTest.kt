/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.viewinterop

import android.content.Context
import android.view.KeyEvent as AndroidKeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.META_SHIFT_ON
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val Tag: String = "tag"

@MediumTest
@RunWith(Parameterized::class)
class FocusSearchInteropTest(private val keyEvent: AndroidKeyEvent) {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun singleFocusableComposable() {
        // Arrange.
        rule.setContent {
            Box(Modifier.testTag(Tag).size(10.dp).focusable())
        }

        // Act.
        rule.onRoot().performKeyPress(keyEvent)

        // Assert.
        rule.onNodeWithTag(Tag).assertIsFocused()
    }

    @Test
    fun singleFocusableView() {
        // Arrange.
        lateinit var embeddedView: View
        rule.setContent {
            AndroidView({
                FocusableView(it).apply { embeddedView = this }
            })
        }

        // Act.
        rule.onRoot().performKeyPress(keyEvent)

        // Assert.
        rule.runOnIdle {
            assertThat(embeddedView.isFocused).isTrue()
        }
    }

    private fun FocusableView(context: Context): View {
        return TextView(context).apply {
            text = "Test"
            isFocusable = true
            isFocusableInTouchMode = true
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "key = {0}")
        fun initParameters() = listOf(
            AndroidKeyEvent(ACTION_DOWN, Key.DirectionUp.nativeKeyCode),
            AndroidKeyEvent(ACTION_DOWN, Key.DirectionDown.nativeKeyCode),
            AndroidKeyEvent(ACTION_DOWN, Key.DirectionLeft.nativeKeyCode),
            AndroidKeyEvent(ACTION_DOWN, Key.DirectionRight.nativeKeyCode),
            AndroidKeyEvent(ACTION_DOWN, Key.Tab.nativeKeyCode),
            AndroidKeyEvent(0L, 0L, ACTION_DOWN, Key.Tab.nativeKeyCode, META_SHIFT_ON),
        )
    }
}

private fun SemanticsNodeInteraction.performKeyPress(keyEvent: AndroidKeyEvent) {
    performKeyPress(KeyEvent(keyEvent))
}
