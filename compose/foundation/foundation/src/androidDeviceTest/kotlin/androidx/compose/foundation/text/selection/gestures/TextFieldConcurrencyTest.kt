/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.text.selection.gestures

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text.selection.gestures.util.touchDragNodeTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
internal class TextFieldConcurrencyTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    val testTag = "testTag"

    fun characterPosition(offset: Int): Offset {
        val textLayoutResult = rule.onNodeWithTag(testTag).fetchTextLayoutResult()
        return textLayoutResult.getBoundingBox(offset).center
    }

    private fun performTouchGesture(block: TouchInjectionScope.() -> Unit) {
        rule.onNodeWithTag(testTag).performTouchInput(block)
        rule.waitForIdle()
    }

    private fun touchDragTo(position: Offset, durationMillis: Long = 200L) {
        rule.onNodeWithTag(testTag).touchDragNodeTo(position, durationMillis)
        rule.waitForIdle()
    }

    private fun makeTextFieldState(): TextFieldState {
        return TextFieldState("123456789 1234")
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun whenSelectionChangesDuringGesture_noCrash() {
        ComposeFoundationFlags.isConcurrentTextFieldSelectionFixEnabled = true
        val textFieldState = makeTextFieldState()
        rule.setContent { BasicTextField(textFieldState, Modifier.testTag(testTag)) }

        performTouchGesture { click(characterPosition(13)) }
        performTouchGesture { down(characterPosition(13)) }

        rule.runOnIdle {
            textFieldState.editAsUser(
                inputTransformation = null,
                restartImeIfContentChanges = false,
            ) {
                delete(5, 13)
            }
        }

        // Emulate continuing gesture
        touchDragTo(characterPosition(0))
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test(expected = IllegalArgumentException::class)
    fun whenSelectionChangesDuringGesture_crash_flagDisabled() {
        val original = ComposeFoundationFlags.isConcurrentTextFieldSelectionFixEnabled
        ComposeFoundationFlags.isConcurrentTextFieldSelectionFixEnabled = false
        try {
            val textFieldState = makeTextFieldState()
            rule.setContent { BasicTextField(textFieldState, Modifier.testTag(testTag)) }

            performTouchGesture { click(characterPosition(13)) }
            performTouchGesture { down(characterPosition(13)) }

            rule.runOnIdle {
                textFieldState.editAsUser(
                    inputTransformation = null,
                    restartImeIfContentChanges = false,
                ) {
                    delete(5, 13)
                }
            }

            // Emulate continuing gesture
            touchDragTo(characterPosition(0))
        } finally {
            ComposeFoundationFlags.isConcurrentTextFieldSelectionFixEnabled = original
        }
    }
}
