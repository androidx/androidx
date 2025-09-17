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

package androidx.compose.foundation.layout

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.internal.JvmDefaultWithCompatibility
import androidx.compose.foundation.layout.internal.requirePrecondition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection

/**
 * A layout composable that places its children in a horizontal sequence. For a layout composable
 * that places its children in a vertical sequence, see [Column]. Note that by default items do not
 * scroll; see `Modifier.horizontalScroll` to add this behavior. For a horizontally scrollable list
 * that only composes and lays out the currently visible items see `LazyRow`.
 *
 * The [Row] layout is able to assign children widths according to their weights provided using the
 * [RowScope.weight] modifier. If a child is not provided a weight, it will be asked for its
 * preferred width before the sizes of the children with weights are calculated proportionally to
 * their weight based on the remaining available space. Note that if the [Row] is horizontally
 * scrollable or part of a horizontally scrollable container, any provided weights will be
 * disregarded as the remaining available space will be infinite.
 *
 * When none of its children have weights, a [Row] will be as small as possible to fit its children
 * one next to the other. In order to change the width of the [Row], use the [Modifier.width]
 * modifiers; e.g. to make it fill the available width [Modifier.fillMaxWidth] can be used. If at
 * least one child of a [Row] has a [weight][RowScope.weight], the [Row] will fill the available
 * width, so there is no need for [Modifier.fillMaxWidth]. However, if [Row]'s size should be
 * limited, the [Modifier.width] or [Modifier.size] layout modifiers should be applied.
 *
 * When the size of the [Row] is larger than the sum of its children sizes, a
 * [horizontalArrangement] can be specified to define the positioning of the children inside the
 * [Row]. See [Arrangement] for available positioning behaviors; a custom arrangement can also be
 * defined using the constructor of [Arrangement]. Below is an illustration of different horizontal
 * arrangements:
 *
 * ![Row
 * arrangements](https://developer.android.com/images/reference/androidx/compose/foundation/layout/row_arrangement_visualization.gif)
 *
 * Example usage:
 *
 * @sample androidx.compose.foundation.layout.samples.SimpleRow
 *
 * Note that if two or more Text components are placed in a [Row], normally they should be aligned
 * by their first baselines. [Row] as a general purpose container does not do it automatically so
 * developers need to handle this manually. This is achieved by adding a [RowScope.alignByBaseline]
 * modifier to every such Text component. By default this modifier aligns by [FirstBaseline]. If,
 * however, you need to align Texts by [LastBaseline] for example, use a more general
 * [RowScope.alignBy] modifier.
 *
 * See example of using Texts inside the Row:
 *
 * @sample androidx.compose.foundation.layout.samples.SimpleAlignByInRow
 * @param modifier The modifier to be applied to the Row.
 * @param horizontalArrangement The horizontal arrangement of the layout's children.
 * @param verticalAlignment The vertical alignment of the layout's children.
 * @param content The content of the Row
 * @see Column
 * @see [androidx.compose.foundation.lazy.LazyRow]
 */
@Composable
inline fun Row(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit,
) {
    val measurePolicy = rowMeasurePolicy(horizontalArrangement, verticalAlignment)
    Layout(
        content = { RowScopeInstance.content() },
        measurePolicy = measurePolicy,
        modifier = modifier,
    )
}

/** MeasureBlocks to use when horizontalArrangement and verticalAlignment are not provided. */
@PublishedApi
internal val DefaultRowMeasurePolicy: MeasurePolicy =
    RowMeasurePolicy(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.Top)

@PublishedApi
@Composable
internal fun rowMeasurePolicy(
    horizontalArrangement: Arrangement.Horizontal,
    verticalAlignment: Alignment.Vertical,
): MeasurePolicy =
    if (horizontalArrangement == Arrangement.Start && verticalAlignment == Alignment.Top) {
        DefaultRowMeasurePolicy
    } else {
        remember(horizontalArrangement, verticalAlignment) {
            RowMeasurePolicy(
                horizontalArrangement = horizontalArrangement,
                verticalAlignment = verticalAlignment,
            )
        }
    }

internal data class RowMeasurePolicy(
    private val horizontalArrangement: Arrangement.Horizontal,
    private val verticalAlignment: Alignment.Vertical,
) : MeasurePolicy, RowColumnMeasurePolicy {
    override fun Placeable.mainAxisSize() = width

    override fun Placeable.crossAxisSize() = height

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        return measure(
            constraints.minWidth,
            constraints.minHeight,
            constraints.maxWidth,
            constraints.maxHeight,
            horizontalArrangement.spacing.roundToPx(),
            this,
            measurables,
            arrayOfNulls(measurables.size),
            0,
            measurables.size,
        )
    }

    override fun populateMainAxisPositions(
        mainAxisLayoutSize: Int,
        childrenMainAxisSize: IntArray,
        mainAxisPositions: IntArray,
        measureScope: MeasureScope,
    ) {
        with(horizontalArrangement) {
            measureScope.arrange(
                mainAxisLayoutSize,
                childrenMainAxisSize,
                measureScope.layoutDirection,
                mainAxisPositions,
            )
        }
    }

    override fun placeHelper(
        placeables: Array<Placeable?>,
        measureScope: MeasureScope,
        beforeCrossAxisAlignmentLine: Int,
        mainAxisPositions: IntArray,
        mainAxisLayoutSize: Int,
        crossAxisLayoutSize: Int,
        crossAxisOffset: IntArray?,
        currentLineIndex: Int,
        startIndex: Int,
        endIndex: Int,
    ): MeasureResult {
        return with(measureScope) {
            layout(mainAxisLayoutSize, crossAxisLayoutSize) {
                placeables.forEachIndexed { i, placeable ->
                    val crossAxisPosition =
                        getCrossAxisPosition(
                            placeable!!,
                            placeable.rowColumnParentData,
                            crossAxisLayoutSize,
                            beforeCrossAxisAlignmentLine,
                        )
                    placeable.place(mainAxisPositions[i], crossAxisPosition)
                }
            }
        }
    }

    override fun createConstraints(
        mainAxisMin: Int,
        crossAxisMin: Int,
        mainAxisMax: Int,
        crossAxisMax: Int,
        isPrioritizing: Boolean,
    ): Constraints {
        return createRowConstraints(
            isPrioritizing,
            mainAxisMin,
            crossAxisMin,
            mainAxisMax,
            crossAxisMax,
        )
    }

    private fun getCrossAxisPosition(
        placeable: Placeable,
        parentData: RowColumnParentData?,
        crossAxisLayoutSize: Int,
        beforeCrossAxisAlignmentLine: Int,
    ): Int {
        val childCrossAlignment = parentData?.crossAxisAlignment
        return childCrossAlignment?.align(
            size = crossAxisLayoutSize,
            layoutDirection = LayoutDirection.Ltr,
            placeable = placeable,
            beforeCrossAxisAlignmentLine = beforeCrossAxisAlignmentLine,
        ) ?: verticalAlignment.align(placeable.height, crossAxisLayoutSize)
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ) =
        IntrinsicMeasureBlocks.HorizontalMinWidth(
            measurables,
            height,
            horizontalArrangement.spacing.roundToPx(),
        )

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ) =
        IntrinsicMeasureBlocks.HorizontalMinHeight(
            measurables,
            width,
            horizontalArrangement.spacing.roundToPx(),
        )

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ) =
        IntrinsicMeasureBlocks.HorizontalMaxWidth(
            measurables,
            height,
            horizontalArrangement.spacing.roundToPx(),
        )

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ) =
        IntrinsicMeasureBlocks.HorizontalMaxHeight(
            measurables,
            width,
            horizontalArrangement.spacing.roundToPx(),
        )
}

internal fun createRowConstraints(
    isPrioritizing: Boolean,
    mainAxisMin: Int,
    crossAxisMin: Int,
    mainAxisMax: Int,
    crossAxisMax: Int,
): Constraints {
    return if (!isPrioritizing) {
        Constraints(
            maxWidth = mainAxisMax,
            maxHeight = crossAxisMax,
            minWidth = mainAxisMin,
            minHeight = crossAxisMin,
        )
    } else {
        Constraints.fitPrioritizingWidth(
            maxWidth = mainAxisMax,
            maxHeight = crossAxisMax,
            minWidth = mainAxisMin,
            minHeight = crossAxisMin,
        )
    }
}

/** Scope for the children of [Row]. */
@LayoutScopeMarker
@Immutable
@JvmDefaultWithCompatibility
interface RowScope {
    /**
     * Size the element's width proportional to its [weight] relative to other weighted sibling
     * elements in the [Row]. The parent will divide the horizontal space remaining after measuring
     * unweighted child elements and distribute it according to this weight. When [fill] is true,
     * the element will be forced to occupy the whole width allocated to it. Otherwise, the element
     * is allowed to be smaller - this will result in [Row] being smaller, as the unused allocated
     * width will not be redistributed to other siblings.
     *
     * @param weight The proportional width to give to this element, as related to the total of all
     *   weighted siblings. Must be positive.
     * @param fill When `true`, the element will occupy the whole width allocated.
     */
    @Stable
    fun Modifier.weight(
        @FloatRange(from = 0.0, fromInclusive = false) weight: Float,
        fill: Boolean = true,
    ): Modifier

    /**
     * Align the element vertically within the [Row]. This alignment will have priority over the
     * [Row]'s `verticalAlignment` parameter.
     *
     * Example usage:
     *
     * @sample androidx.compose.foundation.layout.samples.SimpleAlignInRow
     */
    @Stable fun Modifier.align(alignment: Alignment.Vertical): Modifier

    /**
     * Position the element vertically such that its [alignmentLine] aligns with sibling elements
     * also configured to `alignBy`. `alignBy` is a form of [align], so both modifiers will not work
     * together if specified for the same layout. `alignBy` can be used to align two layouts by
     * baseline inside a [Row], using `alignBy(FirstBaseline)`. Within a [Row], all components with
     * `alignBy` will align vertically using the specified [HorizontalAlignmentLine]s or values
     * provided using the other `alignBy` overload, forming a sibling group. At least one element of
     * the sibling group will be placed as it had [Alignment.Top] align in [Row], and the alignment
     * of the other siblings will be then determined such that the alignment lines coincide. Note
     * that if only one element in a [Row] has the `alignBy` modifier specified the element will be
     * positioned as if it had [Alignment.Top] align.
     *
     * Example usage:
     *
     * @sample androidx.compose.foundation.layout.samples.SimpleAlignByInRow
     * @see alignByBaseline
     */
    @Stable fun Modifier.alignBy(alignmentLine: HorizontalAlignmentLine): Modifier

    /**
     * Position the element vertically such that its first baseline aligns with sibling elements
     * also configured to `alignByBaseline` or `alignBy`. This modifier is a form of [align], so
     * both modifiers will not work together if specified for the same layout. `alignByBaseline` is
     * a particular case of `alignBy`. See `alignBy` for more details.
     *
     * Example usage:
     *
     * @sample androidx.compose.foundation.layout.samples.SimpleAlignByInRow
     * @see alignBy
     */
    @Stable fun Modifier.alignByBaseline(): Modifier

    /**
     * Position the element vertically such that the alignment line for the content as determined by
     * [alignmentLineBlock] aligns with sibling elements also configured to `alignBy`. `alignBy` is
     * a form of [align], so both modifiers will not work together if specified for the same layout.
     * Within a [Row], all components with `alignBy` will align vertically using the specified
     * [HorizontalAlignmentLine]s or values obtained from [alignmentLineBlock], forming a sibling
     * group. At least one element of the sibling group will be placed as it had [Alignment.Top]
     * align in [Row], and the alignment of the other siblings will be then determined such that the
     * alignment lines coincide. Note that if only one element in a [Row] has the `alignBy` modifier
     * specified the element will be positioned as if it had [Alignment.Top] align.
     *
     * Example usage:
     *
     * @sample androidx.compose.foundation.layout.samples.SimpleAlignByInRow
     */
    @Stable fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier
}

internal object RowScopeInstance : RowScope {
    @Stable
    override fun Modifier.weight(weight: Float, fill: Boolean): Modifier {
        requirePrecondition(weight > 0.0) { "invalid weight; must be greater than zero" }
        return this.then(
            LayoutWeightElement(
                // Coerce Float.POSITIVE_INFINITY to Float.MAX_VALUE to avoid errors
                weight = weight.coerceAtMost(Float.MAX_VALUE),
                fill = fill,
            )
        )
    }

    @Stable
    override fun Modifier.align(alignment: Alignment.Vertical) =
        this.then(VerticalAlignElement(alignment))

    @Stable
    override fun Modifier.alignBy(alignmentLine: HorizontalAlignmentLine) =
        this.then(WithAlignmentLineElement(alignmentLine = alignmentLine))

    @Stable override fun Modifier.alignByBaseline() = alignBy(FirstBaseline)

    override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int) =
        this.then(WithAlignmentLineBlockElement(block = alignmentLineBlock))
}
