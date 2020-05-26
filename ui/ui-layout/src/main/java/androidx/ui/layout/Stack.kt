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
import androidx.compose.Immutable
import androidx.compose.Stable
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
 * Use [StackScope.gravity] modifier to define the position of the target element inside the
 * [Stack] box.
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SimpleStack
 */
@Composable
fun Stack(
    modifier: Modifier = Modifier,
    children: @Composable StackScope.() -> Unit
) {
    val stackChildren: @Composable () -> Unit = { StackScope().children() }

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
                placeable.placeAbsolute(position.x, position.y)
            }
        }
    }
}

/**
 * A StackScope provides a scope for the children of a [Stack].
 */
@LayoutScopeMarker
@Immutable
class StackScope {
    /**
     * Pull the content element to a specific [Alignment] within the [Stack].
     */
    @Stable
    fun Modifier.gravity(align: Alignment) = this + StackGravityModifier(align)

    /**
     * Size the element to match the size of the [Stack] after all other content elements have
     * been measured.
     *
     * The element using this modifier does not take part in defining the size of the [Stack].
     * Instead, it matches the size of the [Stack] after all other children (not using
     * matchParentSize() modifier) have been measured to obtain the [Stack]'s size.
     * In contrast, a general-purpose [Modifier.fillMaxSize] modifier, which makes an element
     * occupy all available space, will take part in defining the size of the [Stack]. Consequently,
     * using it for an element inside a [Stack] will make the [Stack] itself always fill the
     * available space.
     */
    @Stable
    fun Modifier.matchParentSize() = this + StretchGravityModifier

    internal companion object {
        @Stable
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
