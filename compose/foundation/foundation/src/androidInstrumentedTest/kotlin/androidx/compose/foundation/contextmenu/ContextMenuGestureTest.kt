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

package androidx.compose.foundation.contextmenu

import androidx.compose.foundation.contextmenu.ContextMenuState.Status
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.assertThatOffset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class ContextMenuGestureTest {
    @get:Rule val rule = createComposeRule()

    private val tag = "testTag"

    @Test
    fun whenContextMenuGestures_thenRightClick_isOpenAtOffset() {
        val state = ContextMenuState()
        val touchPosition = Offset(10f, 10f)
        rule.setContent {
            Box(modifier = Modifier.testTag(tag).size(100.dp).contextMenuGestures(state))
        }

        assertThatContextMenuState(state).statusIsClosed()

        rule.onNodeWithTag(tag).performMouseInput { rightClick(touchPosition) }
        rule.waitForIdle()

        assertThatContextMenuState(state).statusIsOpen()
        val openStatus = state.status as Status.Open
        assertThatOffset(openStatus.offset).equalsWithTolerance(touchPosition)
    }

    /**
     * Fails if [Unit] is used as the key in the [pointerInput] in [Modifier.contextMenuGestures].
     */
    @Test
    fun whenContextMenuGestures_insertedAboveOtherPointerInputLate_correctlyDispatchesEvents() {
        val state = ContextMenuState()
        var enableContextMenuGesture by mutableStateOf(false)
        var pressCount = 0
        rule.setContent {
            val maybeAdditionalPointerInput =
                if (enableContextMenuGesture) Modifier.contextMenuGestures(state) else Modifier
            Box(
                modifier =
                    Modifier.testTag(tag)
                        .size(100.dp)
                        .then(maybeAdditionalPointerInput)
                        .pointerInput(Unit) {
                            // increment counter on a down, don't consume event.
                            awaitEachGesture {
                                val event = awaitPointerEvent()
                                if (event.changes.fastAll { it.changedToDown() }) {
                                    pressCount++
                                }
                            }
                        }
            )
        }

        assertThatContextMenuState(state).statusIsClosed()
        val interaction = rule.onNodeWithTag(tag)

        interaction.performMouseInput { rightClick() }
        assertThatContextMenuState(state).statusIsClosed()
        assertThat(pressCount).isEqualTo(1)

        enableContextMenuGesture = true
        rule.waitForIdle()

        interaction.performMouseInput { rightClick() }
        assertThatContextMenuState(state).statusIsOpen()
        assertThat(pressCount).isEqualTo(2)
    }
}
