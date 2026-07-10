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

package com.android.mechanics.compose.modifier

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import com.android.mechanics.GestureContext
import com.android.mechanics.ManagedMotionValue
import com.android.mechanics.MotionValueCollection
import com.android.mechanics.spec.MotionSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TRAVERSAL_NODE_KEY = "MotionDriverNode"

/** Finds the nearest [MotionDriver] (or null) that was registered via a [motionDriver] modifier. */
private fun DelegatableNode.findMotionDriverOrNull(): MotionDriver? {
    return findNearestAncestor(TRAVERSAL_NODE_KEY) as? MotionDriver
}

/** Finds the nearest [MotionDriver] that was registered via a [motionDriver] modifier. */
internal fun DelegatableNode.findMotionDriver(): MotionDriver {
    return checkNotNull(findMotionDriverOrNull()) {
        "Did you forget to add the `motionDriver()` modifier to a parent Composable?"
    }
}

/**
 * A central interface for driving animations based on layout constraints.
 *
 * A `MotionDriver` is attached to a layout node using the [motionDriver] modifier. Descendant nodes
 * can then find this driver to create animations whose target values are derived from the driver's
 * layout `Constraints`. This allows for coordinated animations within a component tree that react
 * to a parent's size changes, such as expanding or collapsing.
 */
internal interface MotionDriver {
    /** The [GestureContext] associated with this motion. */
    val gestureContext: GestureContext

    /**
     * The current vertical state of the layout, indicating if it's minimized, maximized, or in
     * transition.
     */
    val verticalState: State

    enum class State {
        MinValue,
        Transition,
        MaxValue,
    }

    /**
     * Calculates the positional offset from the `MotionDriver`'s layout to the current layout.
     *
     * This function should be called from within a `Placeable.PlacementScope` (such as a `layout`
     * block) by a descendant of the `motionDriver` modifier. It's useful for determining the
     * descendant's position relative to the driver's coordinate system, which can then be used as
     * an input for animations or other positional logic.
     *
     * @return The [Offset] of the current layout within the `MotionDriver`'s coordinate space.
     */
    fun Placeable.PlacementScope.driverOffset(): Offset

    /**
     * Creates and registers a [ManagedMotionValue] that animates based on layout constraints.
     *
     * The value will automatically update its output whenever the `MotionDriver`'s `maxHeight`
     * constraint changes.
     *
     * @param spec A factory for the [MotionSpec] that governs the animation.
     * @param label A string identifier for debugging purposes.
     * @return A [ManagedMotionValue] that provides the animated output.
     */
    fun maxHeightDriven(spec: () -> MotionSpec, label: String? = null): ManagedMotionValue
}

/**
 * Creates and registers a [MotionDriver] for this layout.
 *
 * This allows descendant modifiers or layouts to find this `MotionDriver` (using
 * [findMotionDriver]) and observe its state, which is derived from layout changes (e.g., expanding
 * or collapsing).
 *
 * @param gestureContext The [GestureContext] to be made available through this [MotionDriver].
 * @param label An optional label for debugging and inspector tooling.
 */
fun Modifier.motionDriver(gestureContext: GestureContext, label: String? = null): Modifier =
    this then MotionDriverElement(gestureContext = gestureContext, label = label)

private data class MotionDriverElement(val gestureContext: GestureContext, val label: String?) :
    ModifierNodeElement<MotionDriverNode>() {
    override fun create(): MotionDriverNode =
        MotionDriverNode(gestureContext = gestureContext, label = label)

    override fun update(node: MotionDriverNode) {
        check(node.gestureContext == gestureContext) { "Cannot change the gestureContext" }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "motionDriver"
        properties["label"] = label
    }
}

private class MotionDriverNode(override val gestureContext: GestureContext, label: String?) :
    Modifier.Node(),
    TraversableNode,
    LayoutModifierNode,
    MotionDriver,
    CompositionLocalConsumerModifierNode {
    override val traverseKey: Any = TRAVERSAL_NODE_KEY
    override var verticalState: MotionDriver.State by mutableStateOf(MotionDriver.State.MinValue)

    private var driverCoordinates: LayoutCoordinates? = null
    private var lookAheadHeight: Int = 0
    private var input by mutableFloatStateOf(0f)
    private val motionValues = MotionValueCollection(::input, gestureContext, label = label)

    override fun onAttach() {
        coroutineScope.launch(Dispatchers.Main.immediate) { motionValues.keepRunning() }
    }

    override fun maxHeightDriven(spec: () -> MotionSpec, label: String?): ManagedMotionValue {
        return motionValues.create(spec, label)
    }

    override fun Placeable.PlacementScope.driverOffset(): Offset {
        val driverCoordinates = requireNotNull(driverCoordinates) { "No driver coordinates" }
        val childCoordinates = requireNotNull(coordinates) { "No child coordinates" }
        return driverCoordinates.localPositionOf(childCoordinates)
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)

        if (isLookingAhead) {
            // In the lookahead pass, we capture the target height of the layout.
            // This is assumed to be the max value that the layout will animate to.
            lookAheadHeight = placeable.height
        } else {
            verticalState =
                when (placeable.height) {
                    0 -> MotionDriver.State.MinValue
                    lookAheadHeight -> MotionDriver.State.MaxValue
                    else -> MotionDriver.State.Transition
                }

            input = constraints.maxHeight.toFloat()
        }

        return layout(width = placeable.width, height = placeable.height) {
            driverCoordinates = coordinates
            placeable.place(IntOffset.Zero)
        }
    }
}
