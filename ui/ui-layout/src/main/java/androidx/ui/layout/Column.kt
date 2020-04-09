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

package androidx.ui.layout

import androidx.annotation.FloatRange
import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.Measured
import androidx.ui.core.Modifier
import androidx.ui.core.VerticalAlignmentLine
import androidx.ui.unit.IntPx

/**
 * A layout composable that places its children in a vertical sequence. For a layout composable
 * that places its children in a horizontal sequence, see [Row].
 *
 * The layout model is able to assign children heights according to their weights provided
 * using the [ColumnScope.weight] modifier. If a child is not provided a weight, it will be
 * asked for its preferred height before the sizes of the children with weights are calculated
 * proportionally to their weight based on the remaining available space.
 *
 * When none of its children have weights, a [Column] will be as small as possible to fit its
 * children one on top of the other. In order to change the size of the [Column], use the
 * [LayoutHeight] modifiers; e.g. to make it fill the available height [LayoutWidth.Fill] can be
 * used. If at least one child of a [Column] has a [weight][ColumnScope.weight],
 * the [Column] will fill the available space, so there is no need for [LayoutWidth.Fill]. However,
 * if [Column]'s size should be limited, the [LayoutHeight] or [LayoutHeight.Max] layout
 * modifiers should be applied.
 *
 * When the size of the [Column] is larger than the sum of its children sizes, an [arrangement]
 * can be specified to define the positioning of the children inside the [Column]. See
 * [Arrangement] for available positioning behaviors; a custom arrangement can also be defined
 * using the constructor of [Arrangement].
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SimpleColumn
 *
 * @param modifier The modifier to be applied to the Column.
 * @param arrangement The vertical arrangement of the layout's children.
 *
 * @see Column
 */
@Composable
fun Column(
    modifier: Modifier = Modifier,
    arrangement: Arrangement.Vertical = Arrangement.Top,
    children: @Composable() ColumnScope.() -> Unit
) {
    RowColumnImpl(
        orientation = LayoutOrientation.Vertical,
        modifier = modifier,
        arrangement = arrangement,
        crossAxisAlignment = CrossAxisAlignment.Start,
        crossAxisSize = SizeMode.Wrap,
        children = { ColumnScope.children() }
    )
}

@Deprecated(
    "ColumnAlign is deprecated. Please use Alignment instead.",
    ReplaceWith("Alignment", "androidx.ui.core.Alignment")
)
enum class ColumnAlign {
    Start,
    Center,
    End
}

/**
 * Scope for the children of [Column].
 */
@LayoutScopeMarker
object ColumnScope {
    /**
     * Position the element horizontally within the [Column] according to [align].
     */
    fun Modifier.gravity(align: Alignment.Horizontal) = this + GravityModifier(align)

    @Deprecated(
        "gravity(ColumnAlign) is deprecated. Please use gravity instead.",
        ReplaceWith("gravity(align)")
    )
    @Suppress("Deprecation")
    fun Modifier.gravity(align: ColumnAlign) = this + when (align) {
        ColumnAlign.Start -> GravityModifier(Alignment.Start)
        ColumnAlign.Center -> GravityModifier(Alignment.CenterHorizontally)
        ColumnAlign.End -> GravityModifier(Alignment.End)
    }

    /**
     * Position the element horizontally such that its [alignmentLine] aligns with sibling elements
     * also configured to [alignWithSiblings] with the same [alignmentLine].
     *
     * Example usage:
     * @sample androidx.ui.layout.samples.SimpleRelativeToSiblingsInColumn
     */
    fun Modifier.alignWithSiblings(alignmentLine: VerticalAlignmentLine) =
        this + SiblingsAlignedModifier.WithAlignmentLine(alignmentLine)

    /**
     * Size the element's height proportional to its [weight] relative to other weighted sibling
     * elements in the [Column]. The parent will divide the vertical space remaining after measuring
     * unweighted child elements and distribute it according to this weight.
     *
     * @sample androidx.ui.layout.samples.SimpleColumn
     */
    fun Modifier.weight(
        @FloatRange(from = 0.0, fromInclusive = false) weight: Float,
        fill: Boolean = true
    ): Modifier {
        require(weight > 0.0) { "invalid weight $weight; must be greater than zero" }
        return this + LayoutWeightImpl(weight, fill)
    }

    /**
     * Position the element horizontally such that the alignment line for the content as
     * determined by [alignmentLineBlock] aligns with sibling elements also configured to
     * [alignWithSiblings] with an [alignmentLineBlock].
     *
     * Example usage:
     * @sample androidx.ui.layout.samples.SimpleRelativeToSiblings
     */
    fun Modifier.alignWithSiblings(
        alignmentLineBlock: (Measured) -> IntPx
    ) = this + SiblingsAlignedModifier.WithAlignmentLineBlock(alignmentLineBlock)
}
