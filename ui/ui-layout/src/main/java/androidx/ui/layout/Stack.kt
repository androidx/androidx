/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.layout

import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.Measurable
import androidx.ui.core.Modifier
import androidx.ui.core.ParentDataModifier
import androidx.ui.core.Placeable
import androidx.ui.unit.Density
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.ipx
import androidx.ui.unit.isFinite
import androidx.ui.unit.max

/**
 * A composable that positions its children relative to its edges.
 * The component is useful for drawing children that overlap. The children will always be
 * drawn in the order they are specified in the body of the [Stack].
 * Use [LayoutGravity] options to define how to position a target component inside the [Stack] box.
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SimpleStack
 */
@Composable
fun Stack(
    modifier: Modifier = Modifier.None,
    children: @Composable() StackScope.() -> Unit
) {
    val stackChildren: @Composable() () -> Unit = { StackScope().children() }

    Layout(stackChildren, modifier = modifier) { measurables, constraints, layoutDirection ->
        val placeables = arrayOfNulls<Placeable>(measurables.size)
        // First measure aligned children to get the size of the layout.
        val childConstraints = constraints.copy(minWidth = 0.ipx, minHeight = 0.ipx)
        (0 until measurables.size).filter { i -> !measurables[i].stretch }.forEach { i ->
            placeables[i] = measurables[i].measure(childConstraints)
        }
        val (stackWidth, stackHeight) = with(placeables.filterNotNull()) {
            Pair(
                max(maxBy { it.width.value }?.width ?: IntPx.Zero, constraints.minWidth),
                max(maxBy { it.height.value }?.height ?: IntPx.Zero, constraints.minHeight)
            )
        }

        // Now measure stretch children.
        (0 until measurables.size).filter { i -> measurables[i].stretch }.forEach { i ->
            // infinity check is needed for intrinsic measurements
            val minWidth = if (stackWidth.isFinite()) stackWidth else IntPx.Zero
            val minHeight = if (stackHeight.isFinite()) stackHeight else IntPx.Zero
            placeables[i] = measurables[i].measure(
                Constraints(minWidth, stackWidth, minHeight, stackHeight)
            )
        }

        // Position the children.
        layout(stackWidth, stackHeight) {
            (0 until measurables.size).forEach { i ->
                val measurable = measurables[i]
                val childData = measurable.stackChildData
                val placeable = placeables[i]!!

                val position = childData.alignment.align(
                    IntPxSize(
                        stackWidth - placeable.width,
                        stackHeight - placeable.height
                    ),
                    layoutDirection
                )
                placeable.place(position.x, position.y)
            }
        }
    }
}

/**
 * A StackScope provides a scope for the children of a [Stack].
 */
@LayoutScopeMarker
@Suppress("unused") // Note: Gravity object provides a scope only but is never used itself
class StackScope {
    /**
     * A layout modifier within a [Stack] that makes the target component to occupy the whole
     * space occupied by the [Stack]. Components using this layout modifier do not define the
     * size of the [Stack] and are positioned within the stack after its size is calculated to
     * wrap the non-stretch components.
     */
    val LayoutGravity.Stretch: ParentDataModifier get() = StretchGravityModifier

    /**
     * A layout modifier that defines that child should be positioned top-start inside the
     * [Stack]. This resolves to top-left position for left-to-right layout direction, and
     * top-right position for right-to-left layout direction.
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
    */
    val LayoutGravity.TopStart: ParentDataModifier get() = TopStartGravityModifier

    /**
     * A layout modifier that defines that child should be positioned top-center inside the [Stack].
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
     */
    val LayoutGravity.TopCenter: ParentDataModifier get() = TopCenterGravityModifier

    /**
     * A layout modifier that defines that child should be positioned top-end inside the
     * [Stack]. This resolves to top-right position for left-to-right layout direction, and
     * top-left position for right-to-left layout direction.
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
     */
    val LayoutGravity.TopEnd: ParentDataModifier get() = TopEndGravityModifier

    /**
     * A layout modifier that defines that child should be positioned center-start inside the
     * [Stack]. This resolves to center-left position for left-to-right layout direction, and
     * center-right position for right-to-left layout direction.
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
     */
    val LayoutGravity.CenterStart: ParentDataModifier get() = CenterStartGravityModifier

    /**
     * A layout modifier that defines that child should be positioned in the center of the [Stack].
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
     */
    val LayoutGravity.Center: ParentDataModifier get() = CenterGravityModifier

    /**
     * A layout modifier that defines that child should be positioned center-end inside the
     * [Stack]. This resolves to center-right position for left-to-right layout direction, and
     * center-left position for right-to-left layout direction.
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
     */
    val LayoutGravity.CenterEnd: ParentDataModifier get() = CenterEndGravityModifier

    /**
     * A layout modifier that defines that child should be positioned bottom-start inside the
     * [Stack]. This resolves to bottom-left position for left-to-right layout direction, and
     * bottom-right position for right-to-left layout direction.
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
     */
    val LayoutGravity.BottomStart: ParentDataModifier get() = BottomStartGravityModifier

    /**
     * A layout modifier that defines that child should be positioned bottom-center inside the
     * [Stack].
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
     */
    val LayoutGravity.BottomCenter: ParentDataModifier get() = BottomCenterGravityModifier

    /**
     * A layout modifier that defines that child should be positioned bottom-end inside the
     * [Stack]. This resolves to bottom-right position for left-to-right layout direction, and
     * bottom-left position for right-to-left layout direction.
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
     */
    val LayoutGravity.BottomEnd: ParentDataModifier get() = BottomEndGravityModifier

    internal companion object {
        val TopStartGravityModifier: ParentDataModifier = StackGravityModifier(Alignment.TopStart)
        val TopCenterGravityModifier: ParentDataModifier = StackGravityModifier(Alignment.TopCenter)
        val TopEndGravityModifier: ParentDataModifier = StackGravityModifier(Alignment.TopEnd)
        val CenterStartGravityModifier: ParentDataModifier =
            StackGravityModifier(Alignment.CenterStart)
        val CenterGravityModifier: ParentDataModifier = StackGravityModifier(Alignment.Center)
        val CenterEndGravityModifier: ParentDataModifier =
            StackGravityModifier(Alignment.CenterEnd)
        val BottomStartGravityModifier: ParentDataModifier =
            StackGravityModifier(Alignment.BottomStart)
        val BottomCenterGravityModifier: ParentDataModifier =
            StackGravityModifier(Alignment.BottomCenter)
        val BottomEndGravityModifier: ParentDataModifier =
            StackGravityModifier(Alignment.BottomEnd)
        val StretchGravityModifier: ParentDataModifier =
            StackGravityModifier(Alignment.Center, true)
    }
}

private data class StackChildData(
    val alignment: Alignment,
    val stretch: Boolean = false
)

private val Measurable.stackChildData: StackChildData
    get() = (parentData as? StackChildData) ?: StackChildData(Alignment.TopStart)
private val Measurable.stretch: Boolean
    get() = stackChildData.stretch

private data class StackGravityModifier(
    val alignment: Alignment,
    val stretch: Boolean = false
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): StackChildData {
        return ((parentData as? StackChildData) ?: StackChildData(alignment, stretch))
    }
}