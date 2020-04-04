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
import androidx.ui.core.AlignmentLine
import androidx.ui.core.HorizontalAlignmentLine
import androidx.ui.core.Modifier
import androidx.ui.core.ParentDataModifier
import androidx.ui.core.Placeable
import androidx.ui.unit.IntPx

/**
 * A layout composable that places its children in a horizontal sequence. For a layout composable
 * that places its children in a vertical sequence, see [Column].
 *
 * The layout model is able to assign children widths according to their weights provided
 * using the [RowScope.LayoutWeight] modifier. If a child is not provided a weight, it will be
 * asked for its preferred width before the sizes of the children with weights are calculated
 * proportionally to their weight based on the remaining available space.
 *
 * When none of its children have weights, a [Row] will be as small as possible to fit its
 * children one next to the other. In order to change the size of the [Row], use the
 * [LayoutWidth] modifiers; e.g. to make it fill the available width [LayoutWidth.Fill] can be used.
 * If at least one child of a [Row] has a [weight][RowScope.LayoutWeight], the [Row] will
 * fill the available space, so there is no need for [LayoutWidth.Fill]. However, if [Row]'s
 * size should be limited, the [LayoutWidth] or [LayoutWidth.Max] layout modifiers should be
 * applied.
 *
 * When the size of the [Row] is larger than the sum of its children sizes, an [arrangement]
 * can be specified to define the positioning of the children inside the [Row]. See [Arrangement]
 * for available positioning behaviors; a custom arrangement can also be defined using the
 * constructor of [Arrangement].
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SimpleRow
 *
 * @param modifier The modifier to be applied to the Row.
 * @param arrangement The horizontal arrangement of the layout's children.
 *
 * @see Column
 */
@Composable
fun Row(
    modifier: Modifier = Modifier,
    arrangement: Arrangement.Horizontal = Arrangement.Start,
    children: @Composable() RowScope.() -> Unit
) {
    RowColumnImpl(
        orientation = LayoutOrientation.Horizontal,
        modifier = modifier,
        arrangement = arrangement,
        crossAxisAlignment = CrossAxisAlignment.Start,
        crossAxisSize = SizeMode.Wrap,
        children = { RowScope.children() }
    )
}

/**
 * Vertical alignments for use with [RowScope.gravity].
 *
 * TODO: Unify with other alignment API
 */
enum class RowAlign {
    Top,
    Center,
    Bottom
}

/**
 * Scope for the children of [Row].
 */
@LayoutScopeMarker
@Suppress("unused") // LayoutGravity is used for scoping, but not needed as receiver
object RowScope {
    /**
     * A layout modifier within a [Row] that positions its target component vertically
     * such that its top edge is aligned to the top edge of the [Row].
     */
    @Deprecated(
        "Use Modifier.gravity(RowAlign.Top)",
        replaceWith = ReplaceWith(
            "Modifier.gravity(RowAlign.Top)",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.ColumnAlign"
        )
    )
    val LayoutGravity.Top: ParentDataModifier get() = TopGravityModifier

    /**
     * A layout modifier within a Row that positions target component vertically
     * such that its center is in the middle of the [Row].
     */
    @Deprecated(
        "Use Modifier.gravity(RowAlign.Center)",
        replaceWith = ReplaceWith(
            "Modifier.gravity(RowAlign.Center)",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.ColumnAlign"
        )
    )
    val LayoutGravity.Center: ParentDataModifier get() = CenterGravityModifier

    /**
     * A layout modifier within a Row that positions target component vertically
     * such that its bottom edge is aligned to the bottom edge of the [Row].
     */
    @Deprecated(
        "Use Modifier.gravity(RowAlign.Bottom)",
        replaceWith = ReplaceWith(
            "Modifier.gravity(RowAlign.Bottom)",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.ColumnAlign"
        )
    )
    val LayoutGravity.Bottom: ParentDataModifier get() = BottomGravityModifier

    /**
     * Position the element vertically within the [Row] according to [align].
     */
    fun Modifier.gravity(align: RowAlign) = this + when (align) {
        RowAlign.Top -> TopGravityModifier
        RowAlign.Center -> CenterGravityModifier
        RowAlign.Bottom -> BottomGravityModifier
    }

    /**
     * A layout modifier within a [Row] that positions its target component vertically
     * according to the specified [HorizontalAlignmentLine], such that the position of the alignment
     * line coincides horizontally with the alignment lines of all other siblings having their
     * gravity set to [LayoutGravity.RelativeToSiblings].
     * Within a [Row], all components with [LayoutGravity.RelativeToSiblings] will align
     * vertically using the specified [AlignmentLine]s or values obtained from
     * [alignmentLineBlocks][RowScope.RelativeToSiblings], forming a sibling group.
     * At least one element of the sibling group will be placed as it had [LayoutGravity.Top]
     * in [Row], and the alignment of the other siblings will be then determined such that
     * the alignment lines coincide. Note that if the target component is the only one with the
     * [RelativeToSiblings] modifier specified, then the component will be positioned
     * using [LayoutGravity.Top][RowScope.Top].
     *
     * Example usage:
     * @sample androidx.ui.layout.samples.SimpleRelativeToSiblingsInRow
     */
    @Deprecated(
        "Use Modifier.alignWithSiblings",
        replaceWith = ReplaceWith(
            "Modifier.alignWithSiblings(alignmentLine)",
            "androidx.ui.core.Modifier"
        )
    )
    fun LayoutGravity.RelativeToSiblings(
        alignmentLine: HorizontalAlignmentLine
    ): ParentDataModifier = SiblingsAlignedModifier.WithAlignmentLine(alignmentLine)

    /**
     * Position the element vertically such that its [alignmentLine] aligns with sibling elements
     * also configured to [alignWithSiblings] with the same [alignmentLine].
     *
     * Example usage:
     * @sample androidx.ui.layout.samples.SimpleRelativeToSiblingsInRow
     */
    fun Modifier.alignWithSiblings(alignmentLine: HorizontalAlignmentLine) =
        this + SiblingsAlignedModifier.WithAlignmentLine(alignmentLine)

    /**
     * A scoped modifier within a [Row] that sets the horizontal weight of the layout.
     * A [Row] child that has a weight will be assigned a space proportional to its weight,
     * relative to the other siblings that have weights. This will be calculated by dividing
     * the remaining space after all the siblings without weights will have chosen their size.
     * Note that [LayoutWeight] will only work when applied to layouts that are direct children
     * of [Row], as these are the only parent layouts that know how to interpret its
     * significance. Also, the position of [LayoutWeight] within a modifier chain is not
     * important, as it will just act as data for the parent layout, which will know to measure
     * the child according to its weight. If more than one [LayoutWeight] is provided in a modifier
     * chain, the outermost (leftmost) one will be used.
     * When [fill] is set to true, the layout is forced to occupy the entire space assigned
     * to it by the parent.
     *
     * @sample androidx.ui.layout.samples.SimpleRow
     */
    @Deprecated(
        "Use Modifier.weight",
        replaceWith = ReplaceWith(
            "Modifier.weight(weight, fill)",
            "androidx.ui.core.Modifier"
        )
    )
    fun LayoutWeight(
        @FloatRange(from = 0.0, fromInclusive = false) weight: Float,
        fill: Boolean = true
    ): ParentDataModifier {
        require(weight > 0.0) { "Weight values should be strictly greater than zero." }
        return LayoutWeightImpl(weight, fill)
    }

    /**
     * Size the element's width proportional to its [weight] relative to other weighted sibling
     * elements in the [Row]. The parent will divide the horizontal space remaining after measuring
     * unweighted child elements and distribute it according to this weight.
     */
    fun Modifier.weight(
        @FloatRange(from = 0.0, fromInclusive = false) weight: Float,
        fill: Boolean = true
    ): Modifier {
        require(weight > 0.0) { "invalid weight $weight; must be greater than zero" }
        return this + LayoutWeightImpl(weight, fill)
    }

    /**
     * A layout modifier within a [Row] that positions its target component relative
     * to all other elements within the parent that have [LayoutGravity.RelativeToSiblings].
     * The [alignmentLineBlock] accepts the [Placeable] of the targeted layout and returns the
     * horizontal position along which the target should align such that it coincides vertically
     * with the alignment lines of all other siblings with [LayoutGravity.RelativeToSiblings].
     * Within a [Row], all components with [LayoutGravity.RelativeToSiblings] will align vertically
     * using the specified [AlignmentLine]s or values obtained from [alignmentLineBlock]s,
     * forming a sibling group. At least one element of the sibling group will be placed as if it
     * had [LayoutGravity.Top], and the alignment of the other siblings will be then determined
     * such that the alignment lines coincide. Note that if the target component is the only one
     * with the [RelativeToSiblings] modifier specified, then the component will be positioned
     * vertically using [LayoutGravity.Top] inside the [Row].
     *
     * Example usage:
     * @sample androidx.ui.layout.samples.SimpleRelativeToSiblings
     */
    @Deprecated(
        "Use Modifier.alignWithSiblings",
        replaceWith = ReplaceWith(
            "Modifier.alignWithSiblings(alignmentLineBlock)",
            "androidx.ui.core.Modifier"
        )
    )
    @Suppress("unused")
    fun LayoutGravity.RelativeToSiblings(
        alignmentLineBlock: (Placeable) -> IntPx
    ): ParentDataModifier = SiblingsAlignedModifier.WithAlignmentLineBlock(alignmentLineBlock)

    /**
     * Position the element vertically such that the alignment line for the content as
     * determined by [alignmentLineBlock] aligns with sibling elements also configured to
     * [alignWithSiblings] with an [alignmentLineBlock].
     *
     * Example usage:
     * @sample androidx.ui.layout.samples.SimpleRelativeToSiblings
     */
    fun Modifier.alignWithSiblings(
        alignmentLineBlock: (Placeable) -> IntPx
    ) = this + SiblingsAlignedModifier.WithAlignmentLineBlock(alignmentLineBlock)
}

private val TopGravityModifier = GravityModifier(CrossAxisAlignment.Start)
private val CenterGravityModifier = GravityModifier(CrossAxisAlignment.Center)
private val BottomGravityModifier = GravityModifier(CrossAxisAlignment.End)
