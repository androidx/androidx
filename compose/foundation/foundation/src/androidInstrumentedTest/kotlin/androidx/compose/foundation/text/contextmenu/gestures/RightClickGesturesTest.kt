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

package androidx.compose.foundation.text.contextmenu.gestures

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.assertThatOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class OnRightClickDownTest {
    @get:Rule val rule = createComposeRule()

    private val tag = "testTag"

    @Composable
    private fun TestRightClickBox(onClick: (Offset) -> Unit) {
        Box(
            modifier =
                Modifier.background(Color.LightGray).size(100.dp).testTag(tag).pointerInput(Unit) {
                    onRightClickDown(onClick)
                }
        )
    }

    @Test
    fun whenOnRightClickDown_rightClick_doesTriggerWithCorrectOffset() {
        var clickCount = 0
        val expectedOffset = Offset(10f, 10f)
        rule.setContent {
            TestRightClickBox {
                assertThatOffset(it).equalsWithTolerance(expectedOffset)
                clickCount++
            }
        }
        rule.onNodeWithTag(tag).performMouseInput { rightClick(expectedOffset) }
        assertThat(clickCount).isEqualTo(1)
    }

    @Test
    fun whenOnRightClickDown_leftClick_doesNotTrigger() {
        var clickCount = 0
        rule.setContent { TestRightClickBox { clickCount++ } }
        rule.onNodeWithTag(tag).performMouseInput { click() }
        assertThat(clickCount).isEqualTo(0)
    }

    @Test
    fun whenOnRightClickDown_triggersOnPressAndNotOnRelease() {
        var clickCount = 0
        rule.setContent { TestRightClickBox { clickCount++ } }
        val interaction = rule.onNodeWithTag(tag)

        interaction.performMouseInput {
            updatePointerTo(center)
            press(MouseButton.Secondary)
        }
        assertThat(clickCount).isEqualTo(1)

        interaction.performMouseInput { release(MouseButton.Secondary) }
        assertThat(clickCount).isEqualTo(1)
    }

    @Test
    fun whenOnRightClickDown_alreadyConsumed_doesNotTrigger() {
        var clickCount = 0
        rule.setContent {
            Box(modifier = Modifier.pointerInput(Unit) { onRightClickDown { clickCount++ } }) {
                Box(
                    modifier =
                        Modifier.background(Color.LightGray).size(100.dp).testTag(tag).pointerInput(
                            Unit
                        ) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent().changes.fastForEach { it.consume() }
                                }
                            }
                        }
                )
            }
        }

        rule.onNodeWithTag(tag).performMouseInput { rightClick() }
        assertThat(clickCount).isEqualTo(0)
    }

    @Test
    fun whenOnRightClickDown_consumesRightClickPressAndRelease() {
        suspend fun PointerInputScope.assertCorrectlyConsumed() {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    assertThat(event.changes.fastMap { it.isConsumed }.toSet()).run {
                        when (event.type) {
                            PointerEventType.Press,
                            PointerEventType.Release -> containsExactly(true)
                            else -> containsExactly(false)
                        }
                    }
                }
            }
        }

        rule.setContent {
            Box(modifier = Modifier.pointerInput(Unit) { assertCorrectlyConsumed() }) {
                Box(
                    modifier =
                        Modifier.background(Color.LightGray).size(100.dp).testTag(tag).pointerInput(
                            Unit
                        ) {
                            onRightClickDown { /* Nothing */ }
                        }
                )
            }
        }

        rule.onNodeWithTag(tag).performMouseInput { rightClick() }
    }
}
