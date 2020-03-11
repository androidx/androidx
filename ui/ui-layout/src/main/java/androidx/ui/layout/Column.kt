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
import androidx.ui.core.Modifier
import androidx.ui.core.ParentDataModifier
import androidx.ui.core.Placeable
import androidx.ui.core.VerticalAlignmentLine
import androidx.ui.unit.IntPx

/**
 * A layout composable that places its children in a vertical sequence. For a layout composable
 * that places its children in a horizontal sequence, see [Row].
 *
 * The layout model is able to assign children heights according to their weights provided
 * using the [ColumnScope.LayoutWeight] modifier. If a child is not provided a weight, it will be
 * asked for its preferred height before the sizes of the children with weights are calculated
 * proportionally to their weight based on the remaining available space.
 *
 * When none of its children have weights, a [Column] will be as small as possible to fit its
 * children one on top of the other. In order to change the size of the [Column], use the
 * [LayoutHeight] modifiers; e.g. to make it fill the available height [LayoutWidth.Fill] can be
 * used. If at least one child of a [Column] has a [weight][ColumnScope.LayoutWeight],
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
    modifier: Modifier = Modifier.None,
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

/**
 * Horizontal alignments for use with [ColumnScope.gravity].
 *
 * TODO: Unify with other alignment API
 */
enum class ColumnAlign {
    /**
     * Position the element along the starting edge of the [Column]. The start edge is determined
     * as either left or right by the column's layout direction.
     */
    Start,
    /**
     * Center the element horizontally within the [Column].
     */
    Center,
    /**
     * Position the element along the ending edge of the [Column]. The end edge is determined as
     * either left or right by the column's layout direction.
     */
    End
}

/**
 * Scope for the children of [Column].
 */
@LayoutScopeMarker
@Suppress("unused") // LayoutGravity is used for scoping, but not needed as receiver
object ColumnScope {
    /**
     * A layout modifier within a [Column] that positions its target component horizontally
     * such that its start edge is aligned to the start edge of the [Column].
     */
    @Deprecated(
        "Use Modifier.gravity(ColumnAlign.Start)",
        replaceWith = ReplaceWith(
            "Modifier.gravity(ColumnAlign.Start)",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.ColumnAlign"
        )
    )
    val LayoutGravity.Start: ParentDataModifier get() = StartGravityModifier

    /**
     * A layout modifier within a [Column] that positions its target component horizontally
     * such that its center is in the middle of the [Column].
     */
    @Deprecated(
        "Use Modifier.gravity(ColumnAlign.Center)",
        replaceWith = ReplaceWith(
            "Modifier.gravity(ColumnAlign.Center)",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.ColumnAlign"
        )
    )
    val LayoutGravity.Center: ParentDataModifier get() = CenterGravityModifier

    /**
     * A layout modifier within a [Column] that positions its target component horizontally
     * such that its end edge is aligned to the end edge of the [Column].
     */
    @Deprecated(
        "Use Modifier.gravity(ColumnAlign.End)",
        replaceWith = ReplaceWith(
            "Modifier.gravity(ColumnAlign.End)",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.ColumnAlign"
        )
    )
    val LayoutGravity.End: ParentDataModifier get() = EndGravityModifier

    /**
     * Position the element horizontally within the [Column] according to [align].
     */
    fun Modifier.gravity(align: ColumnAlign) = this + when (align) {
        ColumnAlign.Start -> StartGravityModifier
        ColumnAlign.Center -> CenterGravityModifier
        ColumnAlign.End -> EndGravityModifier
    }

    /**
     * A layout modifier within a [Column] that positions its target component horizontally
     * according to the specified [VerticalAlignmentLine], such that the position of the alignment
     * line coincides vertically with the alignment lines of all other siblings having their gravity
     * set to [LayoutGravity.RelativeToSiblings].
     * Within a [Column], all components with [LayoutGravity.RelativeToSiblings] will align
     * horizontally using the specified [AlignmentLine]s or values obtained from
     * [alignmentLineBlocks][ColumnScope.RelativeToSiblings], forming a sibling group.
     * At least one element of the sibling group will be placed as it had [ColumnScope.Start]
     * in [Column], and the alignment of the other siblings will be then determined such that
     * the alignment lines coincide. Note that if the target component is the only one with the
     * [RelativeToSiblings] modifier specified, then the component will be positioned
     * using [LayoutGravity.Start][ColumnScope.Start].
     *
     * Example usage:
     * @sample androidx.ui.layout.samples.SimpleRelativeToSiblingsInColumn
     */
    @Deprecated(
        "Use Modifier.alignWithSiblings",
        replaceWith = ReplaceWith(
            "Modifier.alignWithSiblings(alignmentLine)",
            "androidx.ui.core.Modifier"
        )
    )
    fun LayoutGravity.RelativeToSiblings(alignmentLine: VerticalAlignmentLine): ParentDataModifier =
        SiblingsAlignedModifier.WithAlignmentLine(alignmentLine)

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
     * A scoped modifier within a [Column] that sets the vertical weight of the layout.
     * A [Column] child that has a weight will be assigned a space proportional to its weight,
     * relative to the other siblings that have weights. This will be calculated by dividing
     * the remaining space after all the siblings without weights will have chosen their size.
     * Note that [LayoutWeight] will only work when applied to layouts that are direct children
     * of [Column], as these are the only parent layouts that know how to interpret its
     * significance. Also, the position of [LayoutWeight] within a modifier chain is not
     * important, as it will just act as data for the parent layout, which will know to measure
     * the child according to its weight. If more than one [LayoutWeight] is provided in a modifier
     * chain, the outermost (leftmost) one will be used.
     * When [fill] is set to true, the layout is forced to occupy the entire space assigned
     * to it by the parent.
     *
     * @sample androidx.ui.layout.samples.SimpleColumn
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
     * A layout modifier within a [Column] that positions its target component relative
     * to all other elements within the container which have [LayoutGravity.RelativeToSiblings].
     * The [alignmentLineBlock] accepts the [Placeable] of the targeted layout and returns the
     * vertical position along which the target should align such that it coincides horizontally
     * with the alignment lines of all other siblings with [LayoutGravity.RelativeToSiblings].
     * Within a [Column], all components with [LayoutGravity.RelativeToSiblings] will align
     * horizontally using the specified [AlignmentLine]s or values obtained from
     * [alignmentLineBlock]s, forming a sibling group. At least one element of the sibling group
     * will be placed as it had [LayoutGravity.Start], and the alignment of the other siblings will
     * be then determined such that the alignment lines coincide. Note that if the target component
     * is the only one with the [RelativeToSiblings] modifier specified, then the component will be
     * positioned horizontally using [LayoutGravity.Start] inside the [Column].
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
     * Position the element horizontally such that the alignment line for the content as
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

private val StartGravityModifier = GravityModifier(CrossAxisAlignment.Start)
private val CenterGravityModifier = GravityModifier(CrossAxisAlignment.Center)
private val EndGravityModifier = GravityModifier(CrossAxisAlignment.End)
