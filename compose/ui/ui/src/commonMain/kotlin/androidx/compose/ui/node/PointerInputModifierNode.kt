/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNodeImpl
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.TouchBoundsExpansion.Companion.MAX_VALUE
import androidx.compose.ui.node.TouchBoundsExpansion.Companion.pack
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.jvm.JvmInline

/**
 * A [androidx.compose.ui.Modifier.Node] that receives [PointerInputChange]s, interprets them, and
 * consumes the aspects of the changes that it is react to such that other
 * [PointerInputModifierNode]s don't also react to them.
 *
 * This is the [androidx.compose.ui.Modifier.Node] equivalent of
 * [androidx.compose.ui.input.pointer.PointerInputFilter].
 *
 * @sample androidx.compose.ui.samples.PointerInputModifierNodeSample
 */
interface PointerInputModifierNode : DelegatableNode {
    /**
     * Invoked when pointers that previously hit this [PointerInputModifierNode] have changed. It is
     * expected that any [PointerInputChange]s that are used during this event and should not be
     * considered valid to be used in other nodes should be marked as consumed by calling
     * [PointerInputChange.consume].
     *
     * @param pointerEvent The list of [PointerInputChange]s with positions relative to this
     *   [PointerInputModifierNode].
     * @param pass The [PointerEventPass] in which this function is being called.
     * @param bounds The width and height associated with this [PointerInputModifierNode].
     * @see PointerInputChange
     * @see PointerEventPass
     */
    fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize)

    /**
     * Invoked to notify the handler that no more calls to [PointerInputModifierNode] will be made,
     * until at least new pointers exist. This can occur for a few reasons:
     * 1. Android dispatches ACTION_CANCEL to Compose.
     * 2. This [PointerInputModifierNode] is no longer associated with a LayoutNode.
     * 3. This [PointerInputModifierNode]'s associated LayoutNode is no longer in the composition
     *    tree.
     */
    fun onCancelPointerInput()

    /**
     * Intercept pointer input that children receive even if the pointer is out of bounds.
     *
     * If `true`, and a child has been moved out of this layout and receives an event, this will
     * receive that event. If `false`, a child receiving pointer input outside of the bounds of this
     * layout will not trigger any events in this.
     */
    fun interceptOutOfBoundsChildEvents(): Boolean = false

    /**
     * If `false`, then this [PointerInputModifierNode] will not allow siblings under it to respond
     * to events. If `true`, this will have the first chance to respond and the next sibling under
     * will then get a chance to respond as well. This trigger acts at the Layout level, so if any
     * [PointerInputModifierNode]s on a Layout has [sharePointerInputWithSiblings] set to `true`
     * then the Layout will share with siblings.
     */
    fun sharePointerInputWithSiblings(): Boolean = false

    /**
     * Invoked when the density (pixels per inch for the screen) changes. This can impact the
     * location of pointer input events (x and y) and can affect things like touch slop detection.
     *
     * Developers will need to restart the gesture detection handling pointer input in order for the
     * event locations to remain accurate.
     *
     * The default implementation will do that by calling [onCancelPointerInput].
     *
     * [SuspendingPointerInputModifierNode] offers a more specific interface to allow only
     * cancelling the coroutine for more control. See [SuspendingPointerInputModifierNodeImpl] for a
     * concrete example.
     */
    fun onDensityChange() {
        onCancelPointerInput()
    }

    /**
     * Invoked when the view configuration (touch slop size, minimum touch target, tap timing)
     * changes which means the composable UI the pointer input block is tied to has changed and the
     * new UI might impact the location of pointer input events (x and y).
     *
     * Developers will need to restart the gesture detection that handles pointer input in order for
     * the events locations to remain accurate.
     *
     * The default implementation will do that by calling [onCancelPointerInput].
     *
     * [SuspendingPointerInputModifierNode] offers a more specific interface to allow only
     * cancelling the coroutine for more control. See [SuspendingPointerInputModifierNodeImpl] for a
     * concrete example.
     */
    fun onViewConfigurationChange() {
        onCancelPointerInput()
    }

    /**
     * Override this value to expand the touch bounds of this [PointerInputModifierNode] by the
     * given value in align each edge. It only applies to this pointer input modifier and won't
     * impact other pointer input modifiers on the same [LayoutNode]. Also note that a pointer in
     * expanded touch bounds can't be intercepted by its parents and ancestors even if their
     * [interceptOutOfBoundsChildEvents] returns true.
     *
     * @see TouchBoundsExpansion
     */
    val touchBoundsExpansion: TouchBoundsExpansion
        get() = TouchBoundsExpansion.None
}

/**
 * Describes the expansion of a [PointerInputModifierNode]'s touch bounds along each edges. See
 * [TouchBoundsExpansion] factories and [Absolute] for convenient ways to build
 * [TouchBoundsExpansion].
 *
 * @see PointerInputModifierNode.touchBoundsExpansion
 */
@JvmInline
value class TouchBoundsExpansion internal constructor(private val packedValue: Long) {
    companion object {
        /**
         * Creates a [TouchBoundsExpansion] that's unaware of [LayoutDirection]. The `left`, `top`,
         * `right` and `bottom` represent the amount of pixels that the touch bounds is expanded
         * along the corresponding edge. Each value must be in the range of 0 to 32767 (inclusive).
         */
        fun Absolute(
            left: Int = 0,
            top: Int = 0,
            right: Int = 0,
            bottom: Int = 0
        ): TouchBoundsExpansion {
            requirePrecondition(left in 0..MAX_VALUE) {
                "Start must be in the range of 0 .. $MAX_VALUE"
            }
            requirePrecondition(top in 0..MAX_VALUE) {
                "Top must be in the range of 0 .. $MAX_VALUE"
            }
            requirePrecondition(right in 0..MAX_VALUE) {
                "End must be in the range of 0 .. $MAX_VALUE"
            }
            requirePrecondition(bottom in 0..MAX_VALUE) {
                "Bottom must be in the range of 0 .. $MAX_VALUE"
            }
            return TouchBoundsExpansion(pack(left, top, right, bottom, false))
        }

        /** Constant that represents no touch bounds expansion. */
        val None = TouchBoundsExpansion(0)

        internal fun pack(
            start: Int,
            top: Int,
            end: Int,
            bottom: Int,
            isLayoutDirectionAware: Boolean
        ): Long {
            return trimAndShift(start, 0) or
                trimAndShift(top, 1) or
                trimAndShift(end, 2) or
                trimAndShift(bottom, 3) or
                if (isLayoutDirectionAware) IS_LAYOUT_DIRECTION_AWARE else 0L
        }

        private const val MASK = 0x7FFF

        private const val SHIFT = 15

        internal const val MAX_VALUE = MASK

        private const val IS_LAYOUT_DIRECTION_AWARE = 1L shl 63

        // We stored all
        private fun unpack(packedValue: Long, position: Int): Int =
            (packedValue shr (position * SHIFT)).toInt() and MASK

        private fun trimAndShift(int: Int, position: Int): Long =
            (int and MASK).toLong() shl (position * SHIFT)
    }

    /**
     * The amount of pixels the touch bounds should be expanded along the start edge. When
     * [isLayoutDirectionAware] is `true`, it's applied to the left edge when [LayoutDirection] is
     * [LayoutDirection.Ltr] and vice versa. When [isLayoutDirectionAware] is `false`, it's always
     * applied to the left edge.
     */
    val start: Int
        get() = unpack(packedValue, 0)

    /** The amount of pixels the touch bounds should be expanded along the top edge. */
    val top: Int
        get() = unpack(packedValue, 1)

    /**
     * The amount of pixels the touch bounds should be expanded along the end edge. When
     * [isLayoutDirectionAware] is `true`, it's applied to the left edge when [LayoutDirection] is
     * [LayoutDirection.Ltr] and vice versa. When [isLayoutDirectionAware] is `false`, it's always
     * applied to the left edge.
     */
    val end: Int
        get() = unpack(packedValue, 2)

    /** The amount of pixels the touch bounds should be expanded along the bottom edge. */
    val bottom: Int
        get() = unpack(packedValue, 3)

    /**
     * Whether this [TouchBoundsExpansion] is aware of [LayoutDirection] or not. See [start] and
     * [end] for more details.
     */
    val isLayoutDirectionAware: Boolean
        get() = (packedValue and IS_LAYOUT_DIRECTION_AWARE) != 0L

    /** Returns the amount of pixels the touch bounds is expanded towards left. */
    internal fun computeLeft(layoutDirection: LayoutDirection): Int {
        return if (!isLayoutDirectionAware || layoutDirection == LayoutDirection.Ltr) {
            start
        } else {
            end
        }
    }

    /** Returns the amount of pixels the touch bounds is expanded towards right. */
    internal fun computeRight(layoutDirection: LayoutDirection): Int {
        return if (!isLayoutDirectionAware || layoutDirection == LayoutDirection.Ltr) {
            end
        } else {
            start
        }
    }
}

/**
 * Creates a [TouchBoundsExpansion] that's aware of [LayoutDirection]. See
 * [TouchBoundsExpansion.start] and [TouchBoundsExpansion.end] for more details about
 * [LayoutDirection].
 *
 * The `start`, `top`, `end` and `bottom` represent the amount of pixels that the touch bounds is
 * expanded along the corresponding edge. Each value must be in the range of 0 to 32767 (inclusive).
 */
fun TouchBoundsExpansion(
    start: Int = 0,
    top: Int = 0,
    end: Int = 0,
    bottom: Int = 0
): TouchBoundsExpansion {
    requirePrecondition(start in 0..MAX_VALUE) { "Start must be in the range of 0 .. $MAX_VALUE" }
    requirePrecondition(top in 0..MAX_VALUE) { "Top must be in the range of 0 .. $MAX_VALUE" }
    requirePrecondition(end in 0..MAX_VALUE) { "End must be in the range of 0 .. $MAX_VALUE" }
    requirePrecondition(bottom in 0..MAX_VALUE) { "Bottom must be in the range of 0 .. $MAX_VALUE" }
    return TouchBoundsExpansion(packedValue = pack(start, top, end, bottom, true))
}

internal val PointerInputModifierNode.isAttached: Boolean
    get() = node.isAttached

internal val PointerInputModifierNode.layoutCoordinates: LayoutCoordinates
    get() = requireCoordinator(Nodes.PointerInput)
