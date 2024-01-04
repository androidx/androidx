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

package androidx.compose.ui.draganddrop

import android.os.Parcel
import android.view.DragEvent
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.OpenComposeView
import androidx.compose.ui.findAndroidComposeView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.spy

private val ContainerSize = 400.dp
private val ParentSize = 150.dp
private val ChildSize = 50.dp

private val HalfContainerSize = ContainerSize / 2
private val HalfParentSize = ParentSize / 2
private val HalfChildSize = ChildSize / 2

class DragAndDropNodeTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule(
        TestActivity::class.java
    )

    private lateinit var container: OpenComposeView

    private lateinit var acceptingTopStartDropTarget: DropTargetModifierHolder
    private lateinit var acceptingTopEndDropTarget: DropTargetModifierHolder
    private lateinit var acceptingParentBottomStartDropTarget: DropTargetModifierHolder
    private lateinit var acceptingInnerBottomStartDropTarget: DropTargetModifierHolder
    private lateinit var rejectingParentBottomEndDropTarget: DropTargetModifierHolder
    private lateinit var acceptingInnerBottomEndDropTarget: DropTargetModifierHolder
    private lateinit var acceptingOffsetInnerBottomStartDropTarget: DropTargetModifierHolder

    private lateinit var density: Density

    /**
     * Sets up a grid of drop targets resembling the following for testing:
     *
     *    accepts                 accepts
     * ┌───────────┐           ┌───────────┐
     * │           │           │           │
     * │           │           │           │
     * │           │           │           │
     * └───────────┘           └───────────┘
     *
     *    accepts                 rejects
     * ┌───────────┐  accepts  ┌───────────┐
     * │  accepts  │  ┌─────┐  │  accepts  │
     * │─────┐     │  │     │  │     ┌─────│
     * │     │     │  └─────┘  │     │     │
     * └─────┘─────┘           └─────└─────┘
     *
     * parent <------> child
     *         offset
     */
    @Before
    fun setup() {
        val activity = rule.activity
        container = spy(OpenComposeView(activity))

        acceptingTopStartDropTarget = DropTargetModifierHolder(
            acceptsDragAndDrop = { true }
        )
        acceptingTopEndDropTarget = DropTargetModifierHolder(
            acceptsDragAndDrop = { true }
        )
        acceptingParentBottomStartDropTarget = DropTargetModifierHolder(
            acceptsDragAndDrop = { true }
        )
        acceptingInnerBottomStartDropTarget = DropTargetModifierHolder(
            acceptsDragAndDrop = { true }
        )
        rejectingParentBottomEndDropTarget = DropTargetModifierHolder(
            acceptsDragAndDrop = { false }
        )
        acceptingInnerBottomEndDropTarget = DropTargetModifierHolder(
            acceptsDragAndDrop = { true }
        )
        acceptingOffsetInnerBottomStartDropTarget = DropTargetModifierHolder(
            acceptsDragAndDrop = { true }
        )

        rule.runOnUiThread {
            activity.setContentView(
                container,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        countDown(from = 1) { latch ->
            rule.runOnUiThread {
                container.setContent {
                    density = LocalDensity.current
                    Box(
                        modifier = Modifier
                            .requiredSize(ContainerSize)
                            .onGloballyPositioned { latch.countDown() }

                    ) {
                        Box(
                            modifier = Modifier
                                .requiredSize(ParentSize)
                                .align(Alignment.TopStart)
                                .testDropTarget(acceptingTopStartDropTarget)
                        )
                        Box(
                            modifier = Modifier
                                .requiredSize(ParentSize)
                                .align(Alignment.TopEnd)
                                .testDropTarget(acceptingTopEndDropTarget)
                        )
                        Box(
                            modifier = Modifier
                                .requiredSize(ParentSize)
                                .align(Alignment.BottomStart)
                                .testDropTarget(acceptingParentBottomStartDropTarget)
                        ) {
                            Box(
                                modifier = Modifier
                                    .requiredSize(ChildSize)
                                    .align(Alignment.BottomStart)
                                    .testDropTarget(acceptingInnerBottomStartDropTarget)
                            )
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = HalfContainerSize - HalfChildSize,
                                        y = (HalfParentSize - HalfChildSize)
                                    )
                                    .requiredSize(ChildSize)
                                    .testDropTarget(acceptingOffsetInnerBottomStartDropTarget)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .requiredSize(ParentSize)
                                .align(Alignment.BottomEnd)
                                .testDropTarget(rejectingParentBottomEndDropTarget)
                        ) {
                            Box(
                                modifier = Modifier
                                    .requiredSize(ChildSize)
                                    .align(Alignment.BottomEnd)
                                    .testDropTarget(acceptingInnerBottomEndDropTarget)
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun dispatchDragEvent_callsStartOnAllDragAndDropTargetsOn_ACTION_DRAG_STARTED() {
        rule.runOnUiThread {
            val dragEvent = DragEvent(
                action = DragEvent.ACTION_DRAG_STARTED,
                x = 0f,
                y = 0f,
            )

            val androidComposeView = findAndroidComposeView(container)!!
            // Act
            val acceptedDrag = androidComposeView.dispatchDragEvent(dragEvent)

            // Assertions
            Truth.assertThat(acceptedDrag).isTrue()

            // The following modifiers should have all seen the start event
            Truth.assertThat(acceptingTopStartDropTarget.startOffsets.first())
                .isEqualTo(dragEvent.offset())
            Truth.assertThat(acceptingTopStartDropTarget.startOffsets.size)
                .isEqualTo(1)

            Truth.assertThat(acceptingTopEndDropTarget.startOffsets.first())
                .isEqualTo(dragEvent.offset())
            Truth.assertThat(acceptingTopEndDropTarget.startOffsets.size)
                .isEqualTo(1)

            Truth.assertThat(acceptingParentBottomStartDropTarget.startOffsets.first())
                .isEqualTo(dragEvent.offset())
            Truth.assertThat(acceptingParentBottomStartDropTarget.startOffsets.size)
                .isEqualTo(1)

            Truth.assertThat(acceptingInnerBottomStartDropTarget.startOffsets.first())
                .isEqualTo(dragEvent.offset())
            Truth.assertThat(acceptingInnerBottomStartDropTarget.startOffsets.size)
                .isEqualTo(1)

            Truth.assertThat(acceptingOffsetInnerBottomStartDropTarget.startOffsets.first())
                .isEqualTo(dragEvent.offset())
            Truth.assertThat(acceptingOffsetInnerBottomStartDropTarget.startOffsets.size)
                .isEqualTo(1)

            Truth.assertThat(acceptingInnerBottomEndDropTarget.startOffsets.first())
                .isEqualTo(dragEvent.offset())
            Truth.assertThat(acceptingInnerBottomEndDropTarget.startOffsets.size)
                .isEqualTo(1)

            // Rejected drag and drop, it should have no start offset
            Truth.assertThat(rejectingParentBottomEndDropTarget.startOffsets.isEmpty())
                .isEqualTo(true)
        }
    }

    @Test
    fun dispatchDragEvent_canEnterThenMoveWithinAndExitWhenDraggedAcross() {
        rule.runOnUiThread {
            val events = listOf(
                // Start in the center
                DragEvent(
                    action = DragEvent.ACTION_DRAG_STARTED,
                    x = with(density) { HalfContainerSize.toPx() },
                    y = with(density) { HalfContainerSize.toPx() },
                ),
                // Move to the top start
                DragEvent(
                    action = DragEvent.ACTION_DRAG_LOCATION,
                    x = 0f,
                    y = 0f,
                ),
                // Move across the top start
                DragEvent(
                    action = DragEvent.ACTION_DRAG_LOCATION,
                    x = with(density) { HalfParentSize.toPx() },
                    y = 0f,
                ),
                // Exit the top start
                DragEvent(
                    action = DragEvent.ACTION_DRAG_LOCATION,
                    x = with(density) { (ParentSize + ChildSize).toPx() },
                    y = 0f,
                )
            )
            val (initialEvent, moveStartEvent, moveEndEvent, exitEvent) = events

            val androidComposeView = findAndroidComposeView(container)!!
            events.forEach(androidComposeView::dispatchDragEvent)

            // Assertions
            Truth.assertThat(acceptingTopStartDropTarget.startOffsets.first())
                .isEqualTo(initialEvent.offset())

            // Enter at top start
            Truth.assertThat(acceptingTopStartDropTarget.enterOffsets.first())
                .isEqualTo(moveStartEvent.offset())

            // Move across top start
            Truth.assertThat(acceptingTopStartDropTarget.moveOffsets.first())
                .isEqualTo(moveStartEvent.offset())
            Truth.assertThat(acceptingTopStartDropTarget.moveOffsets[1])
                .isEqualTo(moveEndEvent.offset())

            // Exit top start
            Truth.assertThat(acceptingTopStartDropTarget.exitOffsets.first())
                .isEqualTo(exitEvent.offset())
        }
    }

    @Test
    fun dispatchDragEvent_ignoresRejectingParentDropTargetButReachesInnerChild() {
        rule.runOnUiThread {
            val events = listOf(
                // Start in the center
                DragEvent(
                    action = DragEvent.ACTION_DRAG_STARTED,
                    x = with(density) { HalfContainerSize.toPx() },
                    y = with(density) { HalfContainerSize.toPx() },
                ),
                // Move into bottom end
                DragEvent(
                    action = DragEvent.ACTION_DRAG_LOCATION,
                    x = with(density) { (ContainerSize - HalfChildSize).toPx() },
                    y = with(density) { (ContainerSize - HalfChildSize).toPx() },
                ),
            )

            val (initialEvent, moveEvent) = events

            val androidComposeView = findAndroidComposeView(container)!!
            events.forEach(androidComposeView::dispatchDragEvent)

            // Rejecting parent should have seen no events
            Truth.assertThat(rejectingParentBottomEndDropTarget.startOffsets.isEmpty())
                .isEqualTo(true)

            Truth.assertThat(rejectingParentBottomEndDropTarget.enterOffsets.isEmpty())
                .isEqualTo(true)

            Truth.assertThat(rejectingParentBottomEndDropTarget.moveOffsets.isEmpty())
                .isEqualTo(true)

            Truth.assertThat(rejectingParentBottomEndDropTarget.exitOffsets.isEmpty())
                .isEqualTo(true)

            Truth.assertThat(rejectingParentBottomEndDropTarget.endedOffsets.isEmpty())
                .isEqualTo(true)

            // Inner offset child should have received events
            Truth.assertThat(acceptingInnerBottomEndDropTarget.startOffsets.first())
                .isEqualTo(initialEvent.offset())

            Truth.assertThat(acceptingInnerBottomEndDropTarget.enterOffsets.first())
                .isEqualTo(moveEvent.offset())
        }
    }

    @Test
    fun dispatchDragEvent_reachesInnerDropTargetOffsetOutsideImmediateParent() {
        rule.runOnUiThread {
            val events = listOf(
                // Start in the center
                DragEvent(
                    action = DragEvent.ACTION_DRAG_STARTED,
                    x = with(density) { HalfContainerSize.toPx() },
                    y = with(density) { HalfContainerSize.toPx() },
                ),
                // Move into bottom center where offset child is
                DragEvent(
                    action = DragEvent.ACTION_DRAG_LOCATION,
                    x = with(density) { HalfContainerSize.toPx() },
                    y = with(density) { (ContainerSize - HalfParentSize).toPx() },
                ),
            )

            val (initialEvent, moveEvent) = events

            val androidComposeView = findAndroidComposeView(container)!!
            events.forEach(androidComposeView::dispatchDragEvent)

            // Assertions
            Truth.assertThat(acceptingOffsetInnerBottomStartDropTarget.startOffsets.first())
                .isEqualTo(initialEvent.offset())

            // The Inner offset child should have seen the enter event
            Truth.assertThat(acceptingOffsetInnerBottomStartDropTarget.enterOffsets.first())
                .isEqualTo(moveEvent.offset())
        }
    }

    @Test
    fun dispatchDragEvent_canMoveBetweenTargets() {
        rule.runOnUiThread {
            val events = listOf(
                // Start in the center
                DragEvent(
                    action = DragEvent.ACTION_DRAG_STARTED,
                    x = with(density) { HalfContainerSize.toPx() },
                    y = with(density) { HalfContainerSize.toPx() },
                ),
                // Move across the top start
                DragEvent(
                    action = DragEvent.ACTION_DRAG_LOCATION,
                    x = 0f,
                    y = 0f,
                ),
                // Exit the top start and into the top right
                DragEvent(
                    action = DragEvent.ACTION_DRAG_LOCATION,
                    x = with(density) { ContainerSize.toPx() - ParentSize.toPx() },
                    y = 0f,
                )
            )

            val (initialEvent, farStartEvent, startToEndEvent) = events

            val androidComposeView = findAndroidComposeView(container)!!
            events.forEach(androidComposeView::dispatchDragEvent)

            // Assertions
            Truth.assertThat(acceptingTopStartDropTarget.startOffsets.first())
                .isEqualTo(initialEvent.offset())

            Truth.assertThat(acceptingTopEndDropTarget.startOffsets.first())
                .isEqualTo(initialEvent.offset())

            Truth.assertThat(acceptingTopStartDropTarget.enterOffsets.first())
                .isEqualTo(farStartEvent.offset())

            Truth.assertThat(acceptingTopStartDropTarget.exitOffsets.first())
                .isEqualTo(startToEndEvent.offset())

            Truth.assertThat(acceptingTopEndDropTarget.enterOffsets.first())
                .isEqualTo(startToEndEvent.offset())
        }
    }

    @Test
    fun dispatchDragEvent_multicasts_ACTION_DRAG_ENDED() {
        rule.runOnUiThread {
            val events = listOf(
                // Start in the center
                DragEvent(
                    action = DragEvent.ACTION_DRAG_STARTED,
                    x = with(density) { HalfContainerSize.toPx() },
                    y = with(density) { HalfContainerSize.toPx() },
                ),
                // End in the center
                DragEvent(
                    action = DragEvent.ACTION_DRAG_ENDED,
                    x = with(density) { HalfContainerSize.toPx() },
                    y = with(density) { HalfContainerSize.toPx() },
                ),
            )

            val (initialEvent, endEvent) = events

            val androidComposeView = findAndroidComposeView(container)!!
            events.forEach(androidComposeView::dispatchDragEvent)

            // The following modifiers should have all seen the start event
            Truth.assertThat(acceptingTopStartDropTarget.startOffsets.first())
                .isEqualTo(initialEvent.offset())
            Truth.assertThat(acceptingTopStartDropTarget.endedOffsets.first())
                .isEqualTo(endEvent.offset())
            Truth.assertThat(acceptingTopStartDropTarget.startOffsets.size)
                .isEqualTo(1)
            Truth.assertThat(acceptingTopStartDropTarget.endedOffsets.size)
                .isEqualTo(1)

            Truth.assertThat(acceptingTopEndDropTarget.startOffsets.first())
                .isEqualTo(initialEvent.offset())
            Truth.assertThat(acceptingTopEndDropTarget.endedOffsets.first())
                .isEqualTo(endEvent.offset())
            Truth.assertThat(acceptingTopEndDropTarget.startOffsets.size)
                .isEqualTo(1)
            Truth.assertThat(acceptingTopEndDropTarget.endedOffsets.size)
                .isEqualTo(1)

            Truth.assertThat(acceptingParentBottomStartDropTarget.startOffsets.first())
                .isEqualTo(initialEvent.offset())
            Truth.assertThat(acceptingParentBottomStartDropTarget.endedOffsets.first())
                .isEqualTo(endEvent.offset())
            Truth.assertThat(acceptingParentBottomStartDropTarget.startOffsets.size)
                .isEqualTo(1)
            Truth.assertThat(acceptingParentBottomStartDropTarget.endedOffsets.size)
                .isEqualTo(1)

            Truth.assertThat(acceptingInnerBottomStartDropTarget.startOffsets.first())
                .isEqualTo(initialEvent.offset())
            Truth.assertThat(acceptingInnerBottomStartDropTarget.endedOffsets.first())
                .isEqualTo(endEvent.offset())
            Truth.assertThat(acceptingInnerBottomStartDropTarget.startOffsets.size)
                .isEqualTo(1)
            Truth.assertThat(acceptingInnerBottomStartDropTarget.endedOffsets.size)
                .isEqualTo(1)

            Truth.assertThat(acceptingOffsetInnerBottomStartDropTarget.startOffsets.first())
                .isEqualTo(initialEvent.offset())
            Truth.assertThat(acceptingOffsetInnerBottomStartDropTarget.endedOffsets.first())
                .isEqualTo(endEvent.offset())
            Truth.assertThat(acceptingOffsetInnerBottomStartDropTarget.startOffsets.size)
                .isEqualTo(1)
            Truth.assertThat(acceptingOffsetInnerBottomStartDropTarget.endedOffsets.size)
                .isEqualTo(1)

            Truth.assertThat(acceptingInnerBottomEndDropTarget.startOffsets.first())
                .isEqualTo(initialEvent.offset())
            Truth.assertThat(acceptingInnerBottomEndDropTarget.endedOffsets.first())
                .isEqualTo(endEvent.offset())
            Truth.assertThat(acceptingInnerBottomEndDropTarget.startOffsets.size)
                .isEqualTo(1)
            Truth.assertThat(acceptingInnerBottomEndDropTarget.endedOffsets.size)
                .isEqualTo(1)

            // Rejected drag and drop, it should have no start or end offset
            Truth.assertThat(rejectingParentBottomEndDropTarget.startOffsets.isEmpty())
                .isEqualTo(true)
            Truth.assertThat(rejectingParentBottomEndDropTarget.endedOffsets.isEmpty())
                .isEqualTo(true)
        }
    }

    @Test
    fun dispatchDragEvent_canEnterThenMoveWithinMultipleNodes() {
        rule.runOnUiThread {
            val events = listOf(
                // Start in the center
                DragEvent(
                    action = DragEvent.ACTION_DRAG_STARTED,
                    x = with(density) { HalfContainerSize.toPx() },
                    y = with(density) { HalfContainerSize.toPx() },
                ),
                // Move into top start parent
                DragEvent(
                    action = DragEvent.ACTION_DRAG_LOCATION,
                    x = with(density) { HalfChildSize.toPx() },
                    y = with(density) { HalfChildSize.toPx() },
                ),
                // Move into bottom start parent
                DragEvent(
                    action = DragEvent.ACTION_DRAG_LOCATION,
                    x = with(density) { HalfChildSize.toPx() },
                    y = with(density) {
                        (ContainerSize - ChildSize - HalfChildSize).toPx()
                    },
                ),
                // Move into bottom start inner child
                DragEvent(
                    action = DragEvent.ACTION_DRAG_LOCATION,
                    x = with(density) { HalfChildSize.toPx() },
                    y = with(density) { (ContainerSize - HalfChildSize).toPx() },
                ),
                // Move into bottom offset inner child
                DragEvent(
                    action = DragEvent.ACTION_DRAG_LOCATION,
                    x = with(density) { HalfContainerSize.toPx() },
                    y = with(density) { (ContainerSize - HalfParentSize).toPx() },
                )

            )
            val (
                initialEvent,
                topStartParentEvent,
                bottomStartParentEvent,
                bottomStartInnerChildEvent,
                bottomInnerOffsetChildEvent,
            ) = events

            val androidComposeView = findAndroidComposeView(container)!!
            events.forEach(androidComposeView::dispatchDragEvent)

            // Assertions
            Truth.assertThat(acceptingTopStartDropTarget.startOffsets.first())
                .isEqualTo(initialEvent.offset())

            // Enter at top start parent
            Truth.assertThat(acceptingTopStartDropTarget.enterOffsets.first())
                .isEqualTo(topStartParentEvent.offset())
            Truth.assertThat(acceptingTopStartDropTarget.moveOffsets.first())
                .isEqualTo(topStartParentEvent.offset())

            // Enter at bottom start parent
            Truth.assertThat(acceptingParentBottomStartDropTarget.enterOffsets.first())
                .isEqualTo(bottomStartParentEvent.offset())
            Truth.assertThat(acceptingParentBottomStartDropTarget.moveOffsets.first())
                .isEqualTo(bottomStartParentEvent.offset())

            // Enter at bottom start child
            Truth.assertThat(acceptingInnerBottomStartDropTarget.enterOffsets.first())
                .isEqualTo(bottomStartInnerChildEvent.offset())
            Truth.assertThat(acceptingInnerBottomStartDropTarget.moveOffsets.first())
                .isEqualTo(bottomStartInnerChildEvent.offset())

            // Enter at bottom offset child
            Truth.assertThat(acceptingOffsetInnerBottomStartDropTarget.enterOffsets.first())
                .isEqualTo(bottomInnerOffsetChildEvent.offset())
            Truth.assertThat(acceptingOffsetInnerBottomStartDropTarget.moveOffsets.first())
                .isEqualTo(bottomInnerOffsetChildEvent.offset())
        }
    }

    @Test
    fun dispatchDragEvent_ignoresNodeThatWasInterestedAndNoLongerIs() {
        // Create UI with node placed in top start of parent
        var shouldRenderItem by mutableStateOf(true)
        val dropTargetHolder = DropTargetModifierHolder(
            acceptsDragAndDrop = { shouldRenderItem }
        )

        // Set up UI
        countDown(from = 1) { latch ->
            rule.runOnUiThread {
                container.setContent {
                    density = LocalDensity.current
                    Box(
                        modifier = Modifier
                            .requiredSize(ContainerSize)
                            .testDropTarget(dropTargetHolder)
                            .onGloballyPositioned { latch.countDown() }
                    )
                }
            }
        }

        val acceptingStartEvent = DragEvent(
            action = DragEvent.ACTION_DRAG_STARTED,
            x = with(density) { HalfContainerSize.toPx() },
            y = with(density) { HalfContainerSize.toPx() },
        )
        val acceptingEndEvent = DragEvent(
            action = DragEvent.ACTION_DRAG_ENDED,
            x = with(density) { ParentSize.toPx() },
            y = with(density) { ParentSize.toPx() },
        )
        val rejectingStartEvent = DragEvent(
            action = DragEvent.ACTION_DRAG_STARTED,
            x = with(density) { HalfParentSize.toPx() },
            y = with(density) { HalfParentSize.toPx() },
        )
        val rejectingEndEvent = DragEvent(
            action = DragEvent.ACTION_DRAG_ENDED,
            x = with(density) { ChildSize.toPx() },
            y = with(density) { ChildSize.toPx() },
        )

        rule.runOnUiThread {
            val androidComposeView = findAndroidComposeView(container)!!

            // Dispatch accepting start and end
            androidComposeView.dispatchDragEvent(acceptingStartEvent)
            androidComposeView.dispatchDragEvent(acceptingEndEvent)

            // Reject events
            shouldRenderItem = false

            // Dispatch rejecting start and end
            androidComposeView.dispatchDragEvent(rejectingStartEvent)
            androidComposeView.dispatchDragEvent(rejectingEndEvent)

            // Assert accepting start and end were seen
            Truth.assertThat(dropTargetHolder.startOffsets.first())
                .isEqualTo(acceptingStartEvent.offset())
            Truth.assertThat(dropTargetHolder.endedOffsets.first())
                .isEqualTo(acceptingEndEvent.offset())

            // Assert only accepting start and end were seen
            Truth.assertThat(dropTargetHolder.startOffsets.size)
                .isEqualTo(1)
            Truth.assertThat(dropTargetHolder.endedOffsets.size)
                .isEqualTo(1)
        }
    }

    @Test
    fun dispatchDragEvent_canReactToNodeMoves() {
        //     start                  end
        // ┌─────┐─────┐         ┌─────┐─────┐
        // │     │     │         │     │     │
        // └─────┘     │   --->  │     └─────┘
        // │           │         │           │
        // └───────────┘         └───────────┘

        val androidComposeView = findAndroidComposeView(container)!!
        var itemOffset by mutableStateOf(IntOffset.Zero)
        val dropTargetHolder = DropTargetModifierHolder(
            acceptsDragAndDrop = { true }
        )

        // At the center
        val initialEvent = DragEvent(
            action = DragEvent.ACTION_DRAG_STARTED,
            x = with(density) { HalfContainerSize.toPx() },
            y = with(density) { HalfContainerSize.toPx() },
        )
        // Close to top start
        val eventWhereItemWas = DragEvent(
            action = DragEvent.ACTION_DRAG_LOCATION,
            x = with(density) { HalfParentSize.toPx() },
            y = with(density) { HalfParentSize.toPx() },
        )
        // Close to top start
        val eventWhereItemUsedToBe = DragEvent(
            action = DragEvent.ACTION_DRAG_LOCATION,
            x = with(density) { HalfChildSize.toPx() },
            y = with(density) { HalfChildSize.toPx() },
        )
        // Close to bottom end
        val eventWhereItemIs = DragEvent(
            action = DragEvent.ACTION_DRAG_LOCATION,
            x = with(density) { (ContainerSize - HalfParentSize).toPx() },
            y = with(density) { (ContainerSize - HalfParentSize).toPx() },
        )

        // Set up UI
        countDown(from = 2) { latch ->
            rule.runOnUiThread {
                // Create UI with node placed in top start of parent
                container.setContent {
                    density = LocalDensity.current
                    Box(
                        modifier = Modifier
                            .requiredSize(ContainerSize)
                            .onGloballyPositioned {
                                // Start in the center
                                androidComposeView.dispatchDragEvent(initialEvent)
                                // Move into item
                                androidComposeView.dispatchDragEvent(eventWhereItemWas)

                                // Move item to bottom end
                                itemOffset = IntOffset(
                                    x = with(density) { (ContainerSize - ParentSize).roundToPx() },
                                    y = with(density) { (ContainerSize - ParentSize).roundToPx() },
                                )
                            }

                    ) {
                        Box(
                            modifier = Modifier
                                .offset { itemOffset }
                                .requiredSize(ParentSize)
                                .align(Alignment.TopStart)
                                .testDropTarget(dropTargetHolder)
                                .onGloballyPositioned { coordinates ->
                                    // Item has been moved
                                    if (coordinates.positionInParent() != Offset.Zero) {
                                        // Move where item used to be
                                        androidComposeView.dispatchDragEvent(eventWhereItemUsedToBe)

                                        // Move where item now is
                                        androidComposeView.dispatchDragEvent(eventWhereItemIs)
                                    }
                                    latch.countDown()
                                }
                        )
                    }
                }
            }
        }

        // Assertions
        Truth.assertThat(dropTargetHolder.startOffsets.first())
            .isEqualTo(initialEvent.offset())

        // Enter at top start parent
        Truth.assertThat(dropTargetHolder.enterOffsets.first())
            .isEqualTo(eventWhereItemWas.offset())
        Truth.assertThat(dropTargetHolder.moveOffsets.first())
            .isEqualTo(eventWhereItemWas.offset())

        // The next event seen in the item should be the one that entered after being moved
        Truth.assertThat(dropTargetHolder.enterOffsets[1])
            .isEqualTo(eventWhereItemIs.offset())
        Truth.assertThat(dropTargetHolder.moveOffsets[1])
            .isEqualTo(eventWhereItemIs.offset())

        // The event where the item used to be should never have been seen by the item
        Truth.assertThat(
            dropTargetHolder.enterOffsets.none { offset ->
                offset == eventWhereItemUsedToBe.offset()
            }
        )
            .isTrue()
        Truth.assertThat(
            dropTargetHolder.moveOffsets.none { offset ->
                offset == eventWhereItemUsedToBe.offset()
            }
        )
            .isTrue()
    }
}

/**
 * Creates a [DragEvent] with the specified coordinates
 */
private fun DragEvent(action: Int, x: Float, y: Float): DragEvent {
    val parcel = Parcel.obtain()
    parcel.writeInt(action)
    parcel.writeFloat(x)
    parcel.writeFloat(y)
    parcel.writeInt(0) // Result
    parcel.writeInt(0) // No ClipData
    parcel.writeInt(0) // No Clip Description

    parcel.setDataPosition(0)
    return DragEvent.CREATOR.createFromParcel(parcel)
}

private fun DragEvent.offset() = Offset(
    x = x,
    y = y
)

private fun countDown(from: Int, block: (CountDownLatch) -> Unit) {
    val countDownLatch = CountDownLatch(from)
    block(countDownLatch)
    Truth.assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
}

private fun Modifier.testDropTarget(holder: DropTargetModifierHolder) = this then holder.modifier
private class DropTargetModifierHolder(
    private val acceptsDragAndDrop: () -> Boolean
) {
    val startOffsets = mutableListOf<Offset>()
    val enterOffsets = mutableListOf<Offset>()
    val moveOffsets = mutableListOf<Offset>()
    val dropOffsets = mutableListOf<Offset>()
    val exitOffsets = mutableListOf<Offset>()
    val endedOffsets = mutableListOf<Offset>()

    @OptIn(ExperimentalFoundationApi::class)
    val modifier = Modifier.dragAndDropTarget(
        onStarted = {
            val accepts = acceptsDragAndDrop()
            if (accepts) startOffsets.add(
                Offset(x = it.dragEvent.x, y = it.dragEvent.y)
            )
            accepts
        },
        onEntered = {
            enterOffsets.add(
                Offset(x = it.dragEvent.x, y = it.dragEvent.y)
            )
        },
        onMoved = {
            moveOffsets.add(
                Offset(x = it.dragEvent.x, y = it.dragEvent.y)
            )
        },
        onDropped = {
            dropOffsets.add(
                Offset(x = it.dragEvent.x, y = it.dragEvent.y)
            )
            true
        },
        onExited = {
            exitOffsets.add(
                Offset(x = it.dragEvent.x, y = it.dragEvent.y)
            )
        },
        onEnded = {
            endedOffsets.add(
                Offset(x = it.dragEvent.x, y = it.dragEvent.y)
            )
        }
    )
}
