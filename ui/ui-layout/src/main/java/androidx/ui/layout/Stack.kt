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

import androidx.ui.core.Constraints
import androidx.ui.core.IntPx
import androidx.ui.core.IntPxSize
import androidx.ui.core.Measurable
import androidx.ui.core.Placeable
import androidx.ui.core.looseMin
import androidx.ui.core.max
import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.DensityScope
import androidx.ui.core.Layout
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Modifier
import androidx.ui.core.isFinite

/**
 * A composable that positions its children relative to its edges.
 * The component is useful for drawing children that overlap. The children will always be
 * drawn in the order they are specified in the body of the [Stack].
 * Use [Gravity] options to define how to position a target component inside the [Stack] box.
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

    Layout(stackChildren, modifier = modifier) { measurables, constraints ->
        val placeables = arrayOfNulls<Placeable>(measurables.size)
        // First measure aligned children to get the size of the layout.
        (0 until measurables.size).filter { i -> !measurables[i].stretch }.forEach { i ->
            placeables[i] = measurables[i].measure(constraints.looseMin())
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
                    )
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
    val Gravity.Stretch: LayoutModifier get() = StretchGravityModifier

    /**
     * A layout modifier that defines that child should be positioned top-left inside the [Stack].
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
    */
    val Gravity.TopLeft: LayoutModifier get() = TopLeftGravityModifier

    /**
     * A layout modifier that defines that child should be positioned top-center inside the [Stack].
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
     */
    val Gravity.TopCenter: LayoutModifier get() = TopCenterGravityModifier

    /**
     * A layout modifier that defines that child should be positioned top-right inside the [Stack].
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
     */
    val Gravity.TopRight: LayoutModifier get() = TopRightGravityModifier

    /**
     * A layout modifier that defines that child should be positioned center-left inside the
     * [Stack].
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
     */
    val Gravity.CenterLeft: LayoutModifier get() = CenterLeftGravityModifier

    /**
     * A layout modifier that defines that child should be positioned in the center of the [Stack].
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
     */
    val Gravity.Center: LayoutModifier get() = CenterGravityModifier

    /**
     * A layout modifier that defines that child should be positioned center-right inside the
     * [Stack].
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
     */
    val Gravity.CenterRight: LayoutModifier get() = CenterRightGravityModifier

    /**
     * A layout modifier that defines that child should be positioned bottom-left inside the
     * [Stack].
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
     */
    val Gravity.BottomLeft: LayoutModifier get() = BottomLeftGravityModifier

    /**
     * A layout modifier that defines that child should be positioned bottom-center inside the
     * [Stack].
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
     */
    val Gravity.BottomCenter: LayoutModifier get() = BottomCenterGravityModifier

    /**
     * A layout modifier that defines that child should be positioned bottom-right inside the
     * [Stack].
     * If the [Stack] wraps its content (by not being constrained to a min size by its own
     * parent), a child with this gravity option will contribute to the size of the [Stack].
     */
    val Gravity.BottomRight: LayoutModifier get() = BottomRightGravityModifier

    internal companion object {
        val TopLeftGravityModifier: LayoutModifier = StackGravityModifier(Alignment.TopLeft)
        val TopCenterGravityModifier: LayoutModifier = StackGravityModifier(Alignment.TopCenter)
        val TopRightGravityModifier: LayoutModifier = StackGravityModifier(Alignment.TopRight)
        val CenterLeftGravityModifier: LayoutModifier = StackGravityModifier(Alignment.CenterLeft)
        val CenterGravityModifier: LayoutModifier = StackGravityModifier(Alignment.Center)
        val CenterRightGravityModifier: LayoutModifier = StackGravityModifier(Alignment.CenterRight)
        val BottomLeftGravityModifier: LayoutModifier = StackGravityModifier(Alignment.BottomLeft)
        val BottomCenterGravityModifier: LayoutModifier =
            StackGravityModifier(Alignment.BottomCenter)
        val BottomRightGravityModifier: LayoutModifier = StackGravityModifier(Alignment.BottomRight)
        val StretchGravityModifier: LayoutModifier =
            StackGravityModifier(Alignment.Center, true)
    }
}

private data class StackChildData(
    val alignment: Alignment,
    val stretch: Boolean = false
)

private val Measurable.stackChildData: StackChildData
    get() = (parentData as? StackChildData) ?: StackChildData(Alignment.TopLeft)
private val Measurable.stretch: Boolean
    get() = stackChildData.stretch

private data class StackGravityModifier(
    val alignment: Alignment,
    val stretch: Boolean = false
) : LayoutModifier {
    override fun DensityScope.modifyParentData(parentData: Any?): StackChildData {
        return ((parentData as? StackChildData) ?: StackChildData(alignment, stretch))
    }
}