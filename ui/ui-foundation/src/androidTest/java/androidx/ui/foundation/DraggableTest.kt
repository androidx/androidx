/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.foundation

import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.test.filters.SmallTest
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.gestures.draggable
import androidx.ui.layout.Stack
import androidx.ui.layout.preferredSize
import androidx.ui.test.center
import androidx.ui.test.createComposeRule
import androidx.ui.test.doGesture
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.sendSwipe
import androidx.ui.test.sendSwipeWithVelocity
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.milliseconds
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class DraggableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val draggableBoxTag = "dragTag"

    @Test
    fun draggable_horizontalDrag() {
        var total = 0f
        setDraggableContent {
            Modifier.draggable(
                dragDirection = DragDirection.Horizontal,
                onDragDeltaConsumptionRequested = { delta ->
                    total += delta
                    delta
                }
            )
        }
        findByTag(draggableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        val lastTotal = runOnIdleCompose {
            assertThat(total).isGreaterThan(0)
            total
        }
        findByTag(draggableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x, this.center.y + 100f),
                duration = 100.milliseconds
            )
        }
        runOnIdleCompose {
            assertThat(total).isEqualTo(lastTotal)
        }
        findByTag(draggableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x - 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        runOnIdleCompose {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun draggable_verticalDrag() {
        var total = 0f
        setDraggableContent {
            Modifier.draggable(
                dragDirection = DragDirection.Vertical,
                onDragDeltaConsumptionRequested = { delta ->
                    total += delta
                    delta
                }
            )
        }
        findByTag(draggableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x, this.center.y + 100f),
                duration = 100.milliseconds
            )
        }
        val lastTotal = runOnIdleCompose {
            assertThat(total).isGreaterThan(0)
            total
        }
        findByTag(draggableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        runOnIdleCompose {
            assertThat(total).isEqualTo(lastTotal)
        }
        findByTag(draggableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x, this.center.y - 100f),
                duration = 100.milliseconds
            )
        }
        runOnIdleCompose {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun draggable_startStop() {
        var startTrigger = 0f
        var stopTrigger = 0f
        setDraggableContent {
            Modifier.draggable(
                dragDirection = DragDirection.Horizontal,
                onDragStarted = {
                    startTrigger += 1
                },
                onDragStopped = {
                    stopTrigger += 1
                },
                onDragDeltaConsumptionRequested = { delta ->
                    delta
                }
            )
        }
        runOnIdleCompose {
            assertThat(startTrigger).isEqualTo(0)
            assertThat(stopTrigger).isEqualTo(0)
        }
        findByTag(draggableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        runOnIdleCompose {
            assertThat(startTrigger).isEqualTo(1)
            assertThat(stopTrigger).isEqualTo(1)
        }
    }

    @Test
    fun draggable_disabledWontCallLambda() {
        var total = 0f
        val enabled = mutableStateOf(true)
        setDraggableContent {
            Modifier.draggable(
                dragDirection = DragDirection.Horizontal,
                onDragDeltaConsumptionRequested = { delta ->
                    total += delta
                    delta
                },
                enabled = enabled.value
            )
        }
        findByTag(draggableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        val prevTotal = runOnIdleCompose {
            assertThat(total).isGreaterThan(0f)
            enabled.value = false
            total
        }
        findByTag(draggableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        runOnIdleCompose {
            assertThat(total).isEqualTo(prevTotal)
        }
    }

    @Test
    fun draggable_velocityProxy() {
        var velocityTriggered = 0f
        setDraggableContent {
            Modifier.draggable(
                dragDirection = DragDirection.Horizontal,
                onDragStopped = {
                    velocityTriggered = it
                },
                onDragDeltaConsumptionRequested = { delta ->
                    delta
                }
            )
        }
        findByTag(draggableBoxTag).doGesture {
            this.sendSwipeWithVelocity(
                start = this.center,
                end = PxPosition(this.center.x + 100f, this.center.y),
                endVelocity = 112f,
                duration = 100.milliseconds

            )
        }
        runOnIdleCompose {
            assertThat(velocityTriggered - 112f).isLessThan(0.1f)
        }
    }

    @Test
    fun draggable_startWithoutSlop_ifAnimating() {
        var total = 0f
        setDraggableContent {
            Modifier.draggable(
                dragDirection = DragDirection.Horizontal,
                onDragDeltaConsumptionRequested = { delta ->
                    total += delta
                    delta
                },
                startDragImmediately = true
            )
        }
        findByTag(draggableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        runOnIdleCompose {
            // should be exactly 100 as there's no slop
            assertThat(total).isEqualTo(100f)
        }
    }

    @Test
    fun draggable_cancel_callsDragStop() {
        var total = 0f
        var dragStopped = 0f
        setDraggableContent {
            if (total < 20f) {
                Modifier.draggable(
                    dragDirection = DragDirection.Horizontal,
                    onDragDeltaConsumptionRequested = { delta ->
                        total += delta
                        delta
                    },
                    onDragStopped = { dragStopped += 1 },
                    startDragImmediately = true
                )
            } else Modifier
        }
        findByTag(draggableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        runOnIdleCompose {
            // should be exactly 100 as there's no slop
            assertThat(total).isGreaterThan(0f)
            assertThat(dragStopped).isEqualTo(1f)
        }
    }

    @Test
    fun draggable_nestedDrag() {
        var innerDrag = 0f
        var outerDrag = 0f
        composeTestRule.setContent {
            Stack {
                Box(gravity = ContentGravity.Center,
                    modifier = Modifier
                        .testTag(draggableBoxTag)
                        .preferredSize(300.dp)
                        .draggable(
                            dragDirection = DragDirection.Horizontal,
                            onDragDeltaConsumptionRequested = { delta ->
                                outerDrag += delta
                                delta
                            }
                        )
                ) {
                    Box(modifier = Modifier.preferredSize(300.dp)
                        .draggable(
                            dragDirection = DragDirection.Horizontal,
                            onDragDeltaConsumptionRequested = { delta ->
                                innerDrag += delta / 2
                                delta / 2
                            }
                        ))
                }
            }
        }
        findByTag(draggableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 200f, this.center.y),
                duration = 300.milliseconds
            )
        }
        runOnIdleCompose {
            assertThat(innerDrag).isGreaterThan(0f)
            assertThat(outerDrag).isGreaterThan(0f)
            // we consumed half delta in child, so exactly half should go to the parent
            assertThat(outerDrag).isEqualTo(innerDrag)
        }
    }

    @Test
    fun draggable_interactionState() {
        val interactionState = InteractionState()

        setDraggableContent {
            Modifier.draggable(DragDirection.Horizontal, interactionState = interactionState) { 0f }
        }

        runOnIdleCompose {
            assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
        }

        // TODO: b/154498119 simulate drag event, replace with gesture injection when supported
        runOnIdleCompose {
            interactionState.addInteraction(Interaction.Dragged)
        }

        runOnIdleCompose {
            assertThat(interactionState.value).contains(Interaction.Dragged)
        }

        // TODO: b/154498119 simulate drag event, replace with gesture injection when supported
        runOnIdleCompose {
            interactionState.removeInteraction(Interaction.Dragged)
        }

        runOnIdleCompose {
            assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
        }
    }

    @Test
    fun draggable_interactionState_resetWhenDisposed() {
        val interactionState = InteractionState()
        var emitDraggableBox by mutableStateOf(true)

        composeTestRule.setContent {
            Stack {
                if (emitDraggableBox) {
                    Box(modifier = Modifier
                        .testTag(draggableBoxTag)
                        .preferredSize(100.dp)
                        .draggable(
                            DragDirection.Horizontal,
                            interactionState = interactionState
                        ) { 0f }
                    )
                }
            }
        }

        runOnIdleCompose {
            assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
        }

        // TODO: b/154498119 simulate drag event, replace with gesture injection when supported
        runOnIdleCompose {
            interactionState.addInteraction(Interaction.Dragged)
        }

        runOnIdleCompose {
            assertThat(interactionState.value).contains(Interaction.Dragged)
        }

        // Dispose draggable
        runOnIdleCompose {
            emitDraggableBox = false
        }

        runOnIdleCompose {
            assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
        }
    }

    private fun setDraggableContent(draggableFactory: @Composable () -> Modifier) {
        composeTestRule.setContent {
            Stack {
                val draggable = draggableFactory()
                Box(modifier = Modifier
                    .testTag(draggableBoxTag)
                    .preferredSize(100.dp) + draggable
                )
            }
        }
    }
}