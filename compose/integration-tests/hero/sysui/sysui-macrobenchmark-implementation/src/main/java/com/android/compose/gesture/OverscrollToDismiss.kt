/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.compose.gesture

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.android.mechanics.DistanceGestureContext
import com.android.mechanics.MotionValue
import com.android.mechanics.debug.DebugMotionValueNode
import com.android.mechanics.effects.MagneticDetach
import com.android.mechanics.effects.MagneticDetach.Defaults.AttachDetachState
import com.android.mechanics.spec.InputDirection
import com.android.mechanics.spec.SemanticKey
import com.android.mechanics.spec.builder.ComposeMotionBuilderContext
import com.android.mechanics.spec.builder.fixedSpatialValueSpec
import com.android.mechanics.spec.builder.motionBuilderContext
import com.android.mechanics.spec.builder.spatialMotionSpec
import com.android.mechanics.spec.with
import com.android.mechanics.spring.SpringParameters
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** "Swipe to dismiss" effect that supports nested scrolling. */
fun Modifier.overscrollToDismiss(
    orientation: Orientation = Orientation.Horizontal,
    enabled: Boolean = true,
    onDismissed: () -> Unit,
) = this.then(OverscrollToDismissElement(orientation, enabled, onDismissed))

private data class OverscrollToDismissElement(
    val orientation: Orientation,
    val enabled: Boolean,
    val onDismissed: () -> Unit,
) : ModifierNodeElement<OverscrollToDismissNode>() {
    override fun create(): OverscrollToDismissNode {
        return OverscrollToDismissNode(orientation, enabled, onDismissed)
    }

    override fun update(node: OverscrollToDismissNode) {
        node.update(orientation, enabled, onDismissed)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "overscrollToDismiss"
        properties["enabled"] = enabled
        properties["orientation"] = orientation
    }
}

private class OverscrollToDismissNode(
    orientation: Orientation,
    enabled: Boolean,
    var onDismissed: () -> Unit,
) :
    DelegatingNode(),
    LayoutModifierNode,
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode,
    NestedDraggable,
    NestedDraggable.Controller {

    // This implementation always tracks the gesture from 0..x, independent of whether the
    // overscroll is positive or negative. Thus, the output needs to be multiplied by this sign to
    // compute the actual value.
    private var overscrollSign: Float = 0f
    private val gestureContext =
        DistanceGestureContext(0f, InputDirection.Max, directionChangeSlop = 1f)

    private var dragState: DragState by mutableStateOf(DragState.Idle)

    enum class DragState {
        Idle,
        Dragging,
        Dismissed,
    }

    private lateinit var motionValue: MotionValue

    private lateinit var motionBuilderContext: ComposeMotionBuilderContext

    private var delegateNode =
        delegate(NestedDraggableRootNode(this, orientation, null, enabled, true))

    fun update(orientation: Orientation, enabled: Boolean, onDismissed: () -> Unit) {
        this.onDismissed = onDismissed
        delegateNode.update(this, orientation, null, enabled, true)
    }

    private var contentBoxWidth = 0
    private var motionValueJob: Job? = null

    override fun onAttach() {
        motionBuilderContext = motionBuilderContext()
        val spec = derivedStateOf {
            with(motionBuilderContext) {
                when (dragState) {
                    DragState.Idle -> fixedSpatialValueSpec(0f, SnapBackSpring)
                    DragState.Dragging -> spatialMotionSpec { after(0f, MagneticDetach()) }
                    DragState.Dismissed ->
                        fixedSpatialValueSpec(
                            contentBoxWidth.toFloat(),
                            SnapBackSpring,
                            listOf(isDismissedState with true),
                        )
                }
            }
        }

        motionValue =
            MotionValue(
                input = { gestureContext.dragOffset },
                gestureContext = gestureContext,
                spec = spec::value,
            )
        delegate(DebugMotionValueNode(motionValue))
        onObservedReadsChanged()
        motionValueJob = coroutineScope.launch { keepRunningUntilDismissed() }
    }

    override fun onObservedReadsChanged() {
        observeReads {
            gestureContext.directionChangeSlop = currentValueOf(LocalViewConfiguration).touchSlop
        }
    }

    override fun onDetach() {
        motionValueJob?.cancel()
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        contentBoxWidth = placeable.measuredWidth
        return layout(placeable.measuredWidth, placeable.measuredHeight) {
            placeable.place((motionValue.output * overscrollSign).toInt(), 0)
        }
    }

    override val autoStopNestedDrags: Boolean
        get() = true

    override fun onDragStarted(
        position: Offset,
        sign: Float,
        pointersDown: Int,
        pointerType: PointerType?,
    ): NestedDraggable.Controller {
        overscrollSign = sign
        gestureContext.reset(dragOffset = motionValue.output, direction = InputDirection.Max)
        dragState = DragState.Dragging

        return this
    }

    override fun shouldConsumeNestedPreScroll(sign: Float): Boolean {
        return motionValue[isDismissedState] ?: false
    }

    override fun onDrag(delta: Float): Float {
        val previousOffset = gestureContext.dragOffset
        val currentOffset = (gestureContext.dragOffset + delta * overscrollSign).coerceAtLeast(0f)
        gestureContext.dragOffset = currentOffset
        return (currentOffset - previousOffset) * overscrollSign
    }

    override suspend fun onDragStopped(velocity: Float, awaitFling: suspend () -> Unit): Float {
        val currentState = motionValue[AttachDetachState]
        with(requireDensity()) {
            val isFlingInOppositeDirection =
                abs(velocity) > AbortVelocity.toPx() && velocity.sign != overscrollSign

            val settleAttached =
                currentState == MagneticDetach.State.Attached ||
                    (currentState == MagneticDetach.State.Detached && isFlingInOppositeDirection)

            dragState = if (settleAttached) DragState.Idle else DragState.Dismissed
        }
        return velocity
    }

    private suspend fun keepRunningUntilDismissed() {
        motionValue.keepRunningWhile {
            val isDismissed = get(isDismissedState) ?: false
            !(isDismissed && isStable)
        }
        onDismissed()
    }

    companion object {
        val isDismissedState = SemanticKey<Boolean>("isDismissed")
        val AbortVelocity = 100.dp // dp/s
        val SnapBackSpring = SpringParameters(stiffness = 550f, dampingRatio = 0.95f)
    }
}
