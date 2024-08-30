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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.Draggable2DState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertModifierIsPure
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import java.lang.Float.NaN
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class Draggable2DTest {

    @get:Rule val rule = createComposeRule()

    private val draggable2DBoxTag = "drag2DTag"

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun draggable2D_2d_drag() {
        var total = Offset.Zero
        setDraggable2DContent { Modifier.draggable2D { total += it } }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        rule.runOnIdle {
            assertThat(total.x).isGreaterThan(0)
            assertThat(total.y).isGreaterThan(0)
        }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput { moveBy(Offset(-100f, -100f)) }
        rule.runOnIdle {
            assertThat(total.x).isLessThan(0.01f)
            assertThat(total.y).isLessThan(0.01f)
        }
    }

    @Test
    fun draggable2D_startStop() {
        var startTrigger = 0f
        var stopTrigger = 0f
        setDraggable2DContent {
            Modifier.draggable2D(
                onDragStarted = { startTrigger += 1 },
                onDragStopped = { stopTrigger += 1 }
            ) {}
        }
        rule.runOnIdle {
            assertThat(startTrigger).isEqualTo(0)
            assertThat(stopTrigger).isEqualTo(0)
        }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
            up()
        }
        rule.runOnIdle {
            assertThat(startTrigger).isEqualTo(1)
            assertThat(stopTrigger).isEqualTo(1)
        }
    }

    @Test
    fun draggable2D_disableWontCallLambda() {
        var total = Offset.Zero
        val enabled = mutableStateOf(true)
        setDraggable2DContent { Modifier.draggable2D(enabled = enabled.value) { total += it } }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        val prevTotal =
            rule.runOnIdle {
                assertThat(total.x).isGreaterThan(0f)
                assertThat(total.y).isGreaterThan(0f)
                enabled.value = false
                total
            }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput { moveBy(Offset(100f, 100f)) }
        rule.runOnIdle { assertThat(total).isEqualTo(prevTotal) }
    }

    @Test
    fun draggable2D_velocityProxy() {
        var velocityTriggered = Velocity.Zero
        setDraggable2DContent {
            Modifier.draggable2D(onDragStopped = { velocityTriggered = it }) {}
        }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        rule.runOnIdle {
            assertThat(velocityTriggered.x - 112f).isLessThan(0.1f)
            assertThat(velocityTriggered.y - 112f).isLessThan(0.1f)
        }
    }

    @Test
    fun draggable2D_startWithoutSlop_ifAnimating() {
        var total = Offset.Zero
        setDraggable2DContent { Modifier.draggable2D(startDragImmediately = true) { total += it } }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        rule.runOnIdle {
            // should be exactly 100 as there's no slop
            assertThat(total).isEqualTo(Offset(100f, 100f))
        }
    }

    @Test
    @Ignore("b/303237627")
    fun draggable2D_cancel_callsDragStop() {
        var total = Offset.Zero
        var dragStopped = 0f
        setDraggable2DContent {
            if (total.x < 20f) {
                Modifier.draggable2D(
                    onDragStopped = { dragStopped += 1 },
                    startDragImmediately = true
                ) {
                    total += it
                }
            } else Modifier
        }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        rule.runOnIdle {
            assertThat(total.x).isGreaterThan(0f)
            assertThat(total.y).isGreaterThan(0f)
            assertThat(dragStopped).isEqualTo(1f)
        }
    }

    @Test
    fun draggable2D_immediateStart_callsStopWithoutSlop() {
        var total = Offset.Zero
        var dragStopped = 0f
        var dragStarted = 0f
        setDraggable2DContent {
            Modifier.draggable2D(
                onDragStopped = { dragStopped += 1 },
                onDragStarted = { dragStarted += 1 },
                startDragImmediately = true
            ) {
                total += it
            }
        }
        rule.onNodeWithTag(draggable2DBoxTag).performMouseInput { this.press() }
        rule.runOnIdle { assertThat(dragStarted).isEqualTo(1f) }
        rule.onNodeWithTag(draggable2DBoxTag).performMouseInput { this.release() }
        rule.runOnIdle { assertThat(dragStopped).isEqualTo(1f) }
    }

    @Test
    fun draggable2D_callsDragStop_whenNewState() {
        var dragStopped = 0f
        val state = mutableStateOf(Draggable2DState {})
        setDraggable2DContent {
            Modifier.draggable2D(onDragStopped = { dragStopped += 1 }, state = state.value)
        }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        rule.runOnIdle {
            assertThat(dragStopped).isEqualTo(0f)
            state.value = Draggable2DState { /* Do nothing */ }
        }
        rule.runOnIdle { assertThat(dragStopped).isEqualTo(1f) }
    }

    @Test
    fun draggable2D_callsDragStop_whenDisabled() {
        var dragStopped = 0f
        var enabled by mutableStateOf(true)
        setDraggable2DContent {
            Modifier.draggable2D(
                onDragStopped = { dragStopped += 1 },
                enabled = enabled,
                onDrag = {}
            )
        }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        rule.runOnIdle {
            assertThat(dragStopped).isEqualTo(0f)
            enabled = false
        }
        rule.runOnIdle { assertThat(dragStopped).isEqualTo(1f) }
    }

    @Test
    fun draggable2D_callsDragStop_whenNewReverseDirection() {
        var dragStopped = 0f
        var reverseDirection by mutableStateOf(false)
        setDraggable2DContent {
            Modifier.draggable2D(
                onDragStopped = { dragStopped += 1 },
                onDrag = {},
                reverseDirection = reverseDirection
            )
        }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        rule.runOnIdle {
            assertThat(dragStopped).isEqualTo(0f)
            reverseDirection = true
        }
        rule.runOnIdle { assertThat(dragStopped).isEqualTo(1f) }
    }

    @Test
    fun draggable2D_updates_startDragImmediately() {
        var total = Offset.Zero
        var startDragImmediately by mutableStateOf(false)
        var touchSlop: Float? = null
        setDraggable2DContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Modifier.draggable2D(
                onDrag = { total += it },
                startDragImmediately = startDragImmediately
            )
        }
        val delta = touchSlop!! / 2f
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(delta, delta))
            up()
        }
        rule.runOnIdle {
            assertThat(total).isEqualTo(Offset.Zero)
            startDragImmediately = true
        }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(delta, delta))
            up()
        }
        rule.runOnIdle { assertThat(total).isEqualTo(Offset(delta, delta)) }
    }

    @Test
    fun draggable2D_updates_onDragStarted() {
        var total = Offset.Zero
        var onDragStarted1Calls = 0
        var onDragStarted2Calls = 0
        var onDragStarted: (Offset) -> Unit by mutableStateOf({ onDragStarted1Calls += 1 })
        setDraggable2DContent {
            Modifier.draggable2D(onDrag = { total += it }, onDragStarted = onDragStarted)
        }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
            up()
        }
        rule.runOnIdle {
            assertThat(onDragStarted1Calls).isEqualTo(1)
            assertThat(onDragStarted2Calls).isEqualTo(0)
            onDragStarted = { onDragStarted2Calls += 1 }
        }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
            up()
        }
        rule.runOnIdle {
            assertThat(onDragStarted1Calls).isEqualTo(1)
            assertThat(onDragStarted2Calls).isEqualTo(1)
        }
    }

    @Test
    fun draggable2D_updates_onDragStopped() {
        var total = Offset.Zero
        var onDragStopped1Calls = 0
        var onDragStopped2Calls = 0
        var onDragStopped: (Velocity) -> Unit by mutableStateOf({ onDragStopped1Calls += 1 })
        setDraggable2DContent {
            Modifier.draggable2D(onDrag = { total += it }, onDragStopped = onDragStopped)
        }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        rule.runOnIdle {
            assertThat(onDragStopped1Calls).isEqualTo(0)
            assertThat(onDragStopped2Calls).isEqualTo(0)
            onDragStopped = { onDragStopped2Calls += 1 }
        }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput { up() }
        rule.runOnIdle {
            // We changed the lambda before we ever stopped dragging, so only the new one should be
            // called
            assertThat(onDragStopped1Calls).isEqualTo(0)
            assertThat(onDragStopped2Calls).isEqualTo(1)
        }
    }

    @Test
    fun draggable2D_resumesNormally_whenInterruptedWithHigherPriority() = runBlocking {
        var total = Offset.Zero
        var dragStopped = 0f
        val state = Draggable2DState { total += it }
        setDraggable2DContent {
            Modifier.draggable2D(onDragStopped = { dragStopped += 1 }, state = state)
        }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        val prevTotal =
            rule.runOnIdle {
                assertThat(total.x).isGreaterThan(0f)
                assertThat(total.y).isGreaterThan(0f)
                total
            }
        state.drag(MutatePriority.PreventUserInput) { dragBy(Offset(123f, 123f)) }
        rule.runOnIdle {
            assertThat(total).isEqualTo(prevTotal + Offset(123f, 123f))
            assertThat(dragStopped).isEqualTo(1f)
        }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            up()
            down(center)
            moveBy(Offset(100f, 100f))
            up()
        }
        rule.runOnIdle {
            assertThat(total.x).isGreaterThan(prevTotal.x + 123f)
            assertThat(total.y).isGreaterThan(prevTotal.y + 123f)
        }
    }

    @Test
    fun draggable2D_noNestedDrag() {
        var innerDrag = Offset.Zero
        var outerDrag = Offset.Zero
        rule.setContent {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier.testTag(draggable2DBoxTag).size(300.dp).draggable2D {
                            outerDrag += it
                        }
                ) {
                    Box(
                        modifier =
                            Modifier.size(300.dp).draggable2D { delta -> innerDrag += delta / 2f }
                    )
                }
            }
        }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(200f, 200f))
        }
        rule.runOnIdle {
            assertThat(innerDrag.x).isGreaterThan(0f)
            assertThat(innerDrag.y).isGreaterThan(0f)
            // draggable2D doesn't participate in nested scrolling, so outer should receive 0 events
            assertThat(outerDrag).isEqualTo(Offset.Zero)
        }
    }

    @Test
    fun draggable2D_noNestedDragWithDraggable() {
        var innerDrag2D = Offset.Zero
        var outerDrag = 0f
        rule.setContent {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier.testTag(draggable2DBoxTag).size(300.dp).draggable(
                            Orientation.Horizontal
                        ) {
                            outerDrag += it
                        }
                ) {
                    Box(
                        modifier =
                            Modifier.size(300.dp).draggable2D { delta -> innerDrag2D += delta / 2f }
                    )
                }
            }
        }
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(200f, 200f))
        }
        rule.runOnIdle {
            assertThat(innerDrag2D.x).isGreaterThan(0f)
            assertThat(innerDrag2D.y).isGreaterThan(0f)
            // draggable2D doesn't participate in nested scrolling, so outer should receive 0 events
            assertThat(outerDrag).isEqualTo(0f)
        }
    }

    @Test
    fun draggable2D_interactionSource() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        setDraggable2DContent {
            scope = rememberCoroutineScope()
            Modifier.draggable2D(interactionSource = interactionSource) {}
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(Offset(visibleSize.width / 4f, visibleSize.height / 4f))
            moveBy(Offset(visibleSize.width / 2f, visibleSize.height / 2f))
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Stop::class.java)
            assertThat((interactions[1] as DragInteraction.Stop).start).isEqualTo(interactions[0])
        }
    }

    @Test
    fun draggable2D_interactionSource_resetWhenDisposed() {
        val interactionSource = MutableInteractionSource()
        var emitDraggableBox by mutableStateOf(true)

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                if (emitDraggableBox) {
                    Box(
                        modifier =
                            Modifier.testTag(draggable2DBoxTag).size(100.dp).draggable2D(
                                interactionSource = interactionSource
                            ) {}
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(Offset(visibleSize.width / 4f, visibleSize.height / 4f))
            moveBy(Offset(visibleSize.width / 2f, visibleSize.height / 2f))
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        // Dispose draggable
        rule.runOnIdle { emitDraggableBox = false }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Cancel::class.java)
            assertThat((interactions[1] as DragInteraction.Cancel).start).isEqualTo(interactions[0])
        }
    }

    @Test
    fun draggable2D_interactionSource_resetWhenEnabledChanged() {
        val interactionSource = MutableInteractionSource()
        val enabledState = mutableStateOf(true)

        var scope: CoroutineScope? = null

        setDraggable2DContent {
            scope = rememberCoroutineScope()
            Modifier.draggable2D(
                enabled = enabledState.value,
                interactionSource = interactionSource
            ) {}
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(Offset(visibleSize.width / 4f, visibleSize.height / 4f))
            moveBy(Offset(visibleSize.width / 2f, visibleSize.height / 2f))
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        rule.runOnIdle { enabledState.value = false }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Cancel::class.java)
            assertThat((interactions[1] as DragInteraction.Cancel).start).isEqualTo(interactions[0])
        }
    }

    @Test
    fun draggable2D_velocityIsLimitedByViewConfiguration() {
        var latestVelocity = Velocity.Zero
        val maxVelocity = 1000f

        rule.setContent {
            val viewConfig = LocalViewConfiguration.current
            val newConfig =
                object : ViewConfiguration by viewConfig {
                    override val maximumFlingVelocity: Float
                        get() = maxVelocity
                }
            CompositionLocalProvider(LocalViewConfiguration provides newConfig) {
                Box {
                    Box(
                        modifier =
                            Modifier.testTag(draggable2DBoxTag).size(100.dp).draggable2D(
                                onDragStopped = { latestVelocity = it }
                            ) {}
                    )
                }
            }
        }

        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.topLeft,
                end = this.bottomRight,
                endVelocity = 2000f
            )
        }
        rule.runOnIdle { assertThat(latestVelocity).isEqualTo(Velocity(maxVelocity, maxVelocity)) }
    }

    @Test
    fun draggable2D_interactionSource_resetWhenInteractionSourceChanged() {
        val interactionSource1 = MutableInteractionSource()
        val interactionSource2 = MutableInteractionSource()
        val interactionSourceState = mutableStateOf(interactionSource1)

        var scope: CoroutineScope? = null

        setDraggable2DContent {
            scope = rememberCoroutineScope()
            Modifier.draggable2D(interactionSource = interactionSourceState.value) {}
        }

        val interactions1 = mutableListOf<Interaction>()
        val interactions2 = mutableListOf<Interaction>()

        scope!!.launch { interactionSource1.interactions.collect { interactions1.add(it) } }

        rule.runOnIdle {
            assertThat(interactions1).isEmpty()
            assertThat(interactions2).isEmpty()
        }

        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(Offset(visibleSize.width / 4f, visibleSize.height / 4f))
            moveBy(Offset(visibleSize.width / 2f, visibleSize.height / 2f))
        }

        rule.runOnIdle {
            assertThat(interactions1).hasSize(1)
            assertThat(interactions1.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions2).isEmpty()
        }

        rule.runOnIdle { interactionSourceState.value = interactionSource2 }

        rule.runOnIdle {
            assertThat(interactions1).hasSize(2)
            assertThat(interactions1.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions1[1]).isInstanceOf(DragInteraction.Cancel::class.java)
            assertThat((interactions1[1] as DragInteraction.Cancel).start)
                .isEqualTo(interactions1[0])
            // Currently we don't emit drag start for an in progress drag, but this might change
            // in the future.
            assertThat(interactions2).isEmpty()
        }
    }

    @Test
    fun draggable2D_cancelMidDown_shouldContinueWithNextDown() {
        var total = Offset.Zero

        setDraggable2DContent { Modifier.draggable2D(startDragImmediately = true) { total += it } }

        rule.onNodeWithTag(draggable2DBoxTag).performMouseInput {
            enter()
            exit()
        }

        assertThat(total).isEqualTo(Offset.Zero)
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            cancel()
        }

        assertThat(total).isEqualTo(Offset.Zero)
        rule.onNodeWithTag(draggable2DBoxTag).performMouseInput {
            enter()
            exit()
        }

        assertThat(total).isEqualTo(Offset.Zero)
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }

        assertThat(total.x).isGreaterThan(0f)
        assertThat(total.y).isGreaterThan(0f)
    }

    @Test
    fun draggable2D_noMomentumDragging_onDragStopped_shouldGenerateZeroVelocity() {
        val delta = -10f
        var flingVelocity = Velocity(NaN, NaN)
        setDraggable2DContent {
            Modifier.draggable2D(
                state = rememberDraggable2DState {},
                onDragStopped = { velocity -> flingVelocity = velocity }
            )
        }
        // Drag, stop and release. The resulting velocity should be zero because we lost the
        // gesture momentum.
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            // generate various move events
            repeat(30) { moveBy(Offset(delta, delta), delayMillis = 16L) }
            // stop for a moment
            advanceEventTime(3000L)
            up()
        }
        rule.runOnIdle { Assert.assertEquals(Velocity.Zero, flingVelocity) }
    }

    @Test
    fun onDragStopped_inputChanged_shouldNotCancelScope() {
        val enabled = mutableStateOf(true)
        lateinit var runningJob: Job
        rule.setContent {
            val scope = rememberCoroutineScope()
            Box(
                modifier =
                    Modifier.testTag(draggable2DBoxTag)
                        .size(100.dp)
                        .draggable2D(
                            enabled = enabled.value,
                            state = rememberDraggable2DState {},
                            onDragStopped = { _ ->
                                runningJob =
                                    scope.launch { delay(10_000L) } // long running operation
                            }
                        )
            )
        }

        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
            up()
        }

        rule.runOnIdle {
            enabled.value = false // cancels pointer input scope
        }

        rule.runOnIdle {
            assertTrue { runningJob.isActive } // check if scope is still active
        }
    }

    @Test
    fun onDragStarted_startDragImmediatelyFalse_offsetShouldBePostSlopPosition() {
        var onDragStartedOffset = Offset.Unspecified
        var downEventPosition = Offset.Unspecified
        var touchSlop = 0f
        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop

            Box(
                modifier =
                    Modifier.testTag(draggable2DBoxTag)
                        .size(100.dp)
                        .draggable2D(
                            enabled = true,
                            state = rememberDraggable2DState {},
                            onDragStarted = { offset -> onDragStartedOffset = offset },
                            startDragImmediately = false
                        )
            )
        }

        val moveOffset = Offset(100f, 100f)
        rule.onNodeWithTag(draggable2DBoxTag).performTouchInput {
            downEventPosition = center
            down(center)
            moveBy(moveOffset)
            up()
        }
        val moveAngle = Math.atan(moveOffset.x / moveOffset.y.toDouble())

        rule.runOnIdle {
            assertThat(downEventPosition.x + touchSlop * Math.cos(moveAngle).toFloat())
                .isWithin(0.5f)
                .of(onDragStartedOffset.x)
            assertThat(downEventPosition.y + touchSlop * Math.sin(moveAngle).toFloat())
                .isWithin(0.5f)
                .of(onDragStartedOffset.y)
        }
    }

    @Test
    fun testInspectableValue() {
        rule.setContent {
            val modifier =
                Modifier.draggable2D(state = rememberDraggable2DState {}) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("draggable2D")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "enabled",
                    "reverseDirection",
                    "interactionSource",
                    "startDragImmediately",
                    "onDragStarted",
                    "onDragStopped",
                    "state",
                )
        }
    }

    @Test
    fun equalInputs_shouldResolveToEquals() {
        val state = Draggable2DState {}

        assertModifierIsPure { toggleInput ->
            if (toggleInput) {
                Modifier.draggable2D(state, enabled = false)
            } else {
                Modifier.draggable2D(state, enabled = true)
            }
        }
    }

    private fun setDraggable2DContent(draggable2DFactory: @Composable () -> Modifier) {
        rule.setContent {
            Box {
                val draggable2D = draggable2DFactory()
                Box(modifier = Modifier.testTag(draggable2DBoxTag).size(100.dp).then(draggable2D))
            }
        }
    }

    private fun Modifier.draggable2D(
        enabled: Boolean = true,
        reverseDirection: Boolean = false,
        interactionSource: MutableInteractionSource? = null,
        startDragImmediately: Boolean = false,
        onDragStarted: (startedPosition: Offset) -> Unit = {},
        onDragStopped: (velocity: Velocity) -> Unit = {},
        onDrag: (Offset) -> Unit
    ): Modifier = composed {
        val state = rememberDraggable2DState(onDrag)
        draggable2D(
            enabled = enabled,
            reverseDirection = reverseDirection,
            interactionSource = interactionSource,
            startDragImmediately = startDragImmediately,
            onDragStarted = { onDragStarted(it) },
            onDragStopped = { onDragStopped(it) },
            state = state
        )
    }

    private fun Modifier.draggable(
        orientation: Orientation,
        enabled: Boolean = true,
        reverseDirection: Boolean = false,
        interactionSource: MutableInteractionSource? = null,
        startDragImmediately: Boolean = false,
        onDragStarted: (startedPosition: Offset) -> Unit = {},
        onDragStopped: (velocity: Float) -> Unit = {},
        onDrag: (Float) -> Unit
    ): Modifier = composed {
        val state = rememberDraggableState(onDrag)
        draggable(
            orientation = orientation,
            enabled = enabled,
            reverseDirection = reverseDirection,
            interactionSource = interactionSource,
            startDragImmediately = startDragImmediately,
            onDragStarted = { onDragStarted(it) },
            onDragStopped = { onDragStopped(it) },
            state = state
        )
    }
}
