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

package androidx.compose.ui.test.injectionscope.touch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests if [TouchInjectionScope.moveBy] and [TouchInjectionScope.updatePointerBy] work while UI is
 * placed and unplaced.
 */
@MediumTest
class MoveByWithUnplacedUiTest {
    private val targetTag = "TargetTag"
    private val zeroPosition = Offset(0f, 0f)
    private val downPosition = Offset(10f, 10f)
    private val moveToPosition1 = Offset(100f, 100f)
    private val moveToPosition2 = Offset(200f, 200f)
    private val moveToPosition3 = Offset(300f, 300f)

    private var eventType: PointerEventType = PointerEventType.Unknown
    private var isPointerInputPlaced = mutableStateOf(true)
    private var position = zeroPosition

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Before
    fun setUp() {
        eventType = PointerEventType.Unknown
        isPointerInputPlaced = mutableStateOf(true)
        position = zeroPosition
    }

    @Test
    fun onePointer_moveByWithUnPlaceUi_stopsTrackingChanges() {
        rule.setContent {
            Box(Modifier.fillMaxSize().safeContentPadding()) {
                Box(
                    Modifier.testTag(targetTag)
                        .layout { measurable, constraints ->
                            measurable.measure(constraints).run {
                                layout(width, height) {
                                    if (isPointerInputPlaced.value) {
                                        place(0, 0)
                                    }
                                }
                            }
                        }
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    eventType = event.type
                                    val changes = event.changes.first()
                                    position = changes.position
                                }
                            }
                        }
                )
                Column {
                    Button({ isPointerInputPlaced.value = !isPointerInputPlaced.value }) {
                        Text("Toggle pointerInput placement")
                    }
                    Text("isPointerInputPlaced=$isPointerInputPlaced")
                    Text("eventType: $eventType")
                    Text("position: $position")
                }
            }
        }

        rule.runOnIdle {
            assertThat(eventType).isEqualTo(PointerEventType.Unknown)
            assertThat(position).isEqualTo(zeroPosition)
        }

        // When we inject a down event followed by a move event
        rule.onNodeWithTag(targetTag).performTouchInput { down(downPosition) }

        rule.runOnIdle {
            assertThat(eventType).isEqualTo(PointerEventType.Press)
            assertThat(position).isEqualTo(downPosition)
        }

        rule.onNodeWithTag(targetTag).performTouchInput { moveTo(moveToPosition1) }

        rule.runOnIdle {
            assertThat(eventType).isEqualTo(PointerEventType.Move)
            assertThat(position).isEqualTo(moveToPosition1)
        }

        isPointerInputPlaced.value = false

        rule.onNodeWithTag(targetTag).performTouchInput { moveTo(moveToPosition2) }

        rule.runOnIdle {
            // Should not have changed
            assertThat(position).isEqualTo(moveToPosition1)
        }
    }

    @Test
    fun onePointer_moveByAndUnPlaceUiThenPlaceUi_temporarilyStopsTrackingChangesDuringUnPlace() {
        rule.setContent {
            Box(Modifier.fillMaxSize().safeContentPadding()) {
                Box(
                    Modifier.testTag(targetTag)
                        .layout { measurable, constraints ->
                            measurable.measure(constraints).run {
                                layout(width, height) {
                                    if (isPointerInputPlaced.value) {
                                        place(0, 0)
                                    }
                                }
                            }
                        }
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    eventType = event.type
                                    val changes = event.changes.first()
                                    position = changes.position
                                }
                            }
                        }
                )
                Column {
                    Button({ isPointerInputPlaced.value = !isPointerInputPlaced.value }) {
                        Text("Toggle pointerInput placement")
                    }
                    Text("isPointerInputPlaced=$isPointerInputPlaced")
                    Text("eventType: $eventType")
                    Text("position: $position")
                }
            }
        }

        rule.runOnIdle {
            assertThat(eventType).isEqualTo(PointerEventType.Unknown)
            assertThat(position).isEqualTo(zeroPosition)
        }

        // When we inject a down event followed by a move event
        rule.onNodeWithTag(targetTag).performTouchInput { down(downPosition) }

        rule.runOnIdle {
            assertThat(eventType).isEqualTo(PointerEventType.Press)
            assertThat(position).isEqualTo(downPosition)
        }

        rule.onNodeWithTag(targetTag).performTouchInput { moveTo(moveToPosition1) }

        rule.runOnIdle {
            assertThat(eventType).isEqualTo(PointerEventType.Move)
            assertThat(position).isEqualTo(moveToPosition1)
        }

        isPointerInputPlaced.value = false

        rule.onNodeWithTag(targetTag).performTouchInput { moveTo(moveToPosition2) }

        rule.runOnIdle {
            // Should not have changed
            assertThat(position).isEqualTo(moveToPosition1)
        }

        isPointerInputPlaced.value = true

        rule.onNodeWithTag(targetTag).performTouchInput { moveTo(moveToPosition3) }

        rule.runOnIdle {
            // Should have changed now that UI is placed again.
            assertThat(position).isEqualTo(moveToPosition3)
        }
    }
}
