/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.subspace.layout

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Used to specify the arrangement of the layout's children in layouts like [SpatialRow] or
 * [SpatialColumn] in the main axis direction (horizontal and vertical, respectively).
 *
 * [SpatialRow] supports horizontal arrangements similar to [Row]: ![Row
 * arrangements](https://developer.android.com/images/reference/androidx/compose/foundation/layout/row_arrangement_visualization.gif)
 *
 * [SpatialColumn] supports horizontal arrangements similar to [Column]: ![Column
 * arrangements](https://developer.android.com/images/reference/androidx/compose/foundation/layout/column_arrangement_visualization.gif)
 */
public object SpatialArrangement {
    /**
     * Used to specify the horizontal arrangement of the layout's children in layouts like
     * [SpatialRow].
     */
    @Stable
    @JvmDefaultWithCompatibility
    public interface Horizontal {
        /** Spacing that should be added between any two adjacent layout children. */
        public val spacing: Dp
            get() = 0.dp

        /**
         * Horizontally places the layout children.
         *
         * @param totalSize Available space that can be occupied by the children, in pixels.
         * @param sizes An array of sizes of all children, in pixels.
         * @param layoutDirection A layout direction, left-to-right or right-to-left, of the parent
         *   layout that should be taken into account when determining positions of the children.
         * @param outPositions An array of the size of [sizes] that returns the calculated
         *   positions. Position of each child is from the left edge of the parent to center of the
         *   child, in pixels.
         */
        public fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            layoutDirection: LayoutDirection,
            outPositions: IntArray,
        )
    }

    /**
     * Used to specify the vertical arrangement of the layout's children in layouts like
     * [SpatialColumn].
     */
    @Stable
    @JvmDefaultWithCompatibility
    public interface Vertical {
        /** Spacing that should be added between any two adjacent layout children. */
        public val spacing: Dp
            get() = 0.dp

        /**
         * Vertically places the layout children.
         *
         * @param totalSize Available space that can be occupied by the children, in pixels.
         * @param sizes An array of sizes of all children, in pixels.
         * @param outPositions An array of the size of [sizes] that returns the calculated
         *   positions. Position of each child is from the top edge of the parent to center of the
         *   child, in pixels.
         */
        public fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray)
    }

    /**
     * Used to specify the horizontal arrangement of the layout's children in horizontal layouts
     * like [SpatialRow], or the vertical arrangement of the layout's children in vertical layouts
     * like [SpatialColumn].
     */
    @Stable
    @JvmDefaultWithCompatibility
    public interface AxisIndependent : Horizontal, Vertical {
        /** Spacing that should be added between any two adjacent layout children. */
        override val spacing: Dp
            get() = 0.dp
    }

    /**
     * All children should be arranged at the start of the row (left if the layout direction is LTR,
     * right otherwise). Visually: 123#### for LTR and ####321 for RTL
     */
    @Stable
    public val Start: Horizontal =
        object : Horizontal {
            override fun Density.arrange(
                totalSize: Int,
                sizes: IntArray,
                layoutDirection: LayoutDirection,
                outPositions: IntArray,
            ) {
                if (layoutDirection == LayoutDirection.Ltr) {
                    placeLeftOrTop(size = sizes, outPosition = outPositions, reverseInput = false)
                } else {
                    placeRightOrBottom(
                        totalSize = totalSize,
                        size = sizes,
                        outPosition = outPositions,
                        reverseInput = true,
                    )
                }
            }

            override fun toString() = "SpatialArrangement#Start"
        }

    /**
     * All children should be arranged at the end of the row (right if the layout direction is LTR,
     * right otherwise). Visually: ####123 for LTR and 321#### for RTL
     */
    public val End: Horizontal =
        object : Horizontal {
            override fun Density.arrange(
                totalSize: Int,
                sizes: IntArray,
                layoutDirection: LayoutDirection,
                outPositions: IntArray,
            ) {
                if (layoutDirection == LayoutDirection.Ltr) {
                    placeRightOrBottom(
                        totalSize = totalSize,
                        size = sizes,
                        outPosition = outPositions,
                        reverseInput = false,
                    )
                } else {
                    placeLeftOrTop(size = sizes, outPosition = outPositions, reverseInput = true)
                }
            }

            override fun toString() = "SpatialArrangement#End"
        }

    /**
     * All children should be arranged at the top of the column. Visually: (top) 123#### (bottom)
     */
    public val Top: Vertical =
        object : Vertical {
            override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
                placeLeftOrTop(size = sizes, outPosition = outPositions, reverseInput = false)
            }

            override fun toString() = "SpatialArrangement#Top"
        }

    /**
     * All children should be arranged at the bottom of the column. Visually: (top) ####123 (bottom)
     */
    public val Bottom: Vertical =
        object : Vertical {
            override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
                placeRightOrBottom(
                    totalSize = totalSize,
                    size = sizes,
                    outPosition = outPositions,
                    reverseInput = false,
                )
            }

            override fun toString() = "SpatialArrangement#Bottom"
        }

    /**
     * All children should be arranged at the center of the row or column. Visually: ##123## for LTR
     * and ##321## for RTL
     */
    public val Center: AxisIndependent =
        object : AxisIndependent {
            override val spacing: Dp
                get() = super.spacing

            override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
                placeCenter(
                    totalSize = totalSize,
                    size = sizes,
                    outPosition = outPositions,
                    reverseInput = false,
                )
            }

            override fun Density.arrange(
                totalSize: Int,
                sizes: IntArray,
                layoutDirection: LayoutDirection,
                outPositions: IntArray,
            ) {
                if (layoutDirection == LayoutDirection.Ltr) {
                    placeCenter(
                        totalSize = totalSize,
                        size = sizes,
                        outPosition = outPositions,
                        reverseInput = false,
                    )
                } else {
                    placeCenter(
                        totalSize = totalSize,
                        size = sizes,
                        outPosition = outPositions,
                        reverseInput = true,
                    )
                }
            }

            override fun toString() = "SpatialArrangement#Center"
        }

    /**
     * There should be equal space between the children and no space before the first child or after
     * the last child. Visually: 1##2##3 for LTR or 3##2##1 for RTL
     */
    public val SpaceBetween: AxisIndependent =
        object : AxisIndependent {
            override val spacing: Dp
                get() = super.spacing

            override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
                placeSpaceBetween(
                    totalSize = totalSize,
                    size = sizes,
                    outPosition = outPositions,
                    reverseInput = false,
                )
            }

            override fun Density.arrange(
                totalSize: Int,
                sizes: IntArray,
                layoutDirection: LayoutDirection,
                outPositions: IntArray,
            ) {
                if (layoutDirection == LayoutDirection.Ltr) {
                    placeSpaceBetween(
                        totalSize = totalSize,
                        size = sizes,
                        outPosition = outPositions,
                        reverseInput = false,
                    )
                } else {
                    placeSpaceBetween(
                        totalSize = totalSize,
                        size = sizes,
                        outPosition = outPositions,
                        reverseInput = true,
                    )
                }
            }

            override fun toString() = "SpatialArrangement#SpaceBetween"
        }

    /**
     * There should be equal space around each child. Visually: #1##2##3# for LTR and #3##2##1# for
     * RTL
     */
    public val SpaceAround: AxisIndependent =
        object : AxisIndependent {
            override val spacing: Dp
                get() = super.spacing

            override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
                placeSpaceAround(
                    totalSize = totalSize,
                    size = sizes,
                    outPosition = outPositions,
                    reverseInput = false,
                )
            }

            override fun Density.arrange(
                totalSize: Int,
                sizes: IntArray,
                layoutDirection: LayoutDirection,
                outPositions: IntArray,
            ) {
                if (layoutDirection == LayoutDirection.Ltr) {
                    placeSpaceAround(
                        totalSize = totalSize,
                        size = sizes,
                        outPosition = outPositions,
                        reverseInput = false,
                    )
                } else {
                    placeSpaceAround(
                        totalSize = totalSize,
                        size = sizes,
                        outPosition = outPositions,
                        reverseInput = true,
                    )
                }
            }

            override fun toString() = "SpatialArrangement#SpaceAround"
        }

    /**
     * There should be equal space between the children and before the first child and after the
     * last child. Visually: #1#2#3# for LTR and #3#2#1# for RTL
     */
    public val SpaceEvenly: AxisIndependent =
        object : AxisIndependent {
            override val spacing: Dp
                get() = super.spacing

            override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
                placeSpaceEvenly(
                    totalSize = totalSize,
                    size = sizes,
                    outPosition = outPositions,
                    reverseInput = false,
                )
            }

            override fun Density.arrange(
                totalSize: Int,
                sizes: IntArray,
                layoutDirection: LayoutDirection,
                outPositions: IntArray,
            ) {
                if (layoutDirection == LayoutDirection.Ltr) {
                    placeSpaceEvenly(
                        totalSize = totalSize,
                        size = sizes,
                        outPosition = outPositions,
                        reverseInput = false,
                    )
                } else {
                    placeSpaceEvenly(
                        totalSize = totalSize,
                        size = sizes,
                        outPosition = outPositions,
                        reverseInput = true,
                    )
                }
            }

            override fun toString() = "SpatialArrangement#SpaceEvenly"
        }

    /**
     * Children are placed next to each other with fixed [space] between them along the main axis.
     */
    public fun spacedBy(space: Dp): AxisIndependent =
        SpacedAligned(space = space, rtlMirror = true, axisMultiplier = 0) {
            occupied,
            totalSize,
            layoutDirection ->
            SpatialAlignment.CenterHorizontally.offset(
                width = occupied,
                space = totalSize,
                layoutDirection = layoutDirection,
            )
        }

    /**
     * Children are placed next to each other with fixed [space] between them horizontally and
     * aligned them according to the [spatialAlignment] given.
     */
    public fun spacedBy(space: Dp, spatialAlignment: SpatialAlignment.Horizontal): Horizontal =
        SpacedAligned(space = space, rtlMirror = true, axisMultiplier = 1) {
            occupied,
            totalSize,
            layoutDirection ->
            spatialAlignment.offset(
                width = occupied,
                space = totalSize,
                layoutDirection = layoutDirection,
            )
        }

    /**
     * Children are placed next to each other with fixed [space] between them vertically and align
     * them according to the [spatialAlignment] given.
     */
    public fun spacedBy(space: Dp, spatialAlignment: SpatialAlignment.Vertical): Vertical =
        SpacedAligned(space = space, rtlMirror = false, axisMultiplier = -1) {
            occupied,
            totalSize,
            _ ->
            spatialAlignment.offset(height = occupied, space = totalSize)
        }

    /**
     * Children placed next to each other horizontally and align them according to the
     * [spatialAlignment] given.
     */
    public fun aligned(spatialAlignment: SpatialAlignment.Horizontal): Horizontal =
        SpacedAligned(space = 0.dp, rtlMirror = true, axisMultiplier = 1) {
            occupied,
            totalSize,
            layoutDirection ->
            spatialAlignment.offset(
                width = occupied,
                space = totalSize,
                layoutDirection = layoutDirection,
            )
        }

    /**
     * Children placed next to each other vertically and align them according to the
     * [spatialAlignment].
     */
    public fun aligned(spatialAlignment: SpatialAlignment.Vertical): Vertical =
        SpacedAligned(space = 0.dp, rtlMirror = false, axisMultiplier = -1) { occupied, totalSize, _
            ->
            spatialAlignment.offset(height = occupied, space = totalSize)
        }

    /** Used to specify arrangement which doesn't change with layout direction. */
    @Immutable
    public object Absolute {
        /**
         * All children should be arranged at the left of the [SpatialRow]. Unlike
         * [SpatialArrangement.Start], when layout direction is RTL, children will not be mirrored.
         * Visually: 123####
         */
        public val Left: Horizontal =
            object : Horizontal {
                override fun Density.arrange(
                    totalSize: Int,
                    sizes: IntArray,
                    layoutDirection: LayoutDirection,
                    outPositions: IntArray,
                ) {
                    placeLeftOrTop(size = sizes, outPosition = outPositions, reverseInput = false)
                }

                override fun toString() = "SpatialAbsoluteArrangement#Left"
            }

        /**
         * All children should be arranged at the center of the [SpatialRow]. Unlike
         * [SpatialArrangement.Center], when layout direction is RTL, children will not be mirrored.
         * Visually: ##123##
         */
        public val Center: Horizontal =
            object : Horizontal {
                override fun Density.arrange(
                    totalSize: Int,
                    sizes: IntArray,
                    layoutDirection: LayoutDirection,
                    outPositions: IntArray,
                ) {
                    placeCenter(
                        totalSize = totalSize,
                        size = sizes,
                        outPosition = outPositions,
                        reverseInput = false,
                    )
                }

                override fun toString() = "SpatialAbsoluteArrangement#Center"
            }

        /**
         * All children should be arranged at the right of the [SpatialRow]. Unlike
         * [SpatialArrangement.End], when layout direction is RTL, children will not be mirrored.
         * Visually: ####123
         */
        public val Right: Horizontal =
            object : Horizontal {
                override fun Density.arrange(
                    totalSize: Int,
                    sizes: IntArray,
                    layoutDirection: LayoutDirection,
                    outPositions: IntArray,
                ) {
                    placeRightOrBottom(
                        totalSize = totalSize,
                        size = sizes,
                        outPosition = outPositions,
                        reverseInput = false,
                    )
                }

                override fun toString() = "SpatialAbsoluteArrangement#Right"
            }

        /**
         * There should be equal space between the children and before the first child and after the
         * last child. Unlike [SpatialArrangement.SpaceEvenly], when layout direction is RTL,
         * children will not be mirrored. Visually: #1#2#3#
         */
        public val SpaceEvenly: Horizontal =
            object : Horizontal {
                override fun Density.arrange(
                    totalSize: Int,
                    sizes: IntArray,
                    layoutDirection: LayoutDirection,
                    outPositions: IntArray,
                ) {
                    placeSpaceEvenly(
                        totalSize = totalSize,
                        size = sizes,
                        outPosition = outPositions,
                        reverseInput = false,
                    )
                }

                override fun toString() = "SpatialAbsoluteArrangement#SpaceEvenly"
            }

        /**
         * There should be equal space between the children and no space before the first child or
         * after the last child. Unlike [SpatialArrangement.SpaceBetween], when layout direction is
         * RTL, children will not be mirrored. Visually: 1##2##3
         */
        public val SpaceBetween: Horizontal =
            object : Horizontal {
                override fun Density.arrange(
                    totalSize: Int,
                    sizes: IntArray,
                    layoutDirection: LayoutDirection,
                    outPositions: IntArray,
                ) {
                    placeSpaceBetween(
                        totalSize = totalSize,
                        size = sizes,
                        outPosition = outPositions,
                        reverseInput = false,
                    )
                }

                override fun toString() = "SpatialAbsoluteArrangement#SpaceBetween"
            }

        /**
         * There should be equal space around each child. Unlike [SpatialArrangement.SpaceAround],
         * when layout direction is RTL, children will not be mirrored. Visually: #1##2##3##4#
         */
        public val SpaceAround: Horizontal =
            object : Horizontal {
                override fun Density.arrange(
                    totalSize: Int,
                    sizes: IntArray,
                    layoutDirection: LayoutDirection,
                    outPositions: IntArray,
                ) {
                    placeSpaceAround(
                        totalSize = totalSize,
                        size = sizes,
                        outPosition = outPositions,
                        reverseInput = false,
                    )
                }

                override fun toString() = "SpatialAbsoluteArrangement#SpaceAround"
            }

        /**
         * Children are placed next to each other with fixed [space] between them horizontally and
         * align them according to the [spatialAlignment] given. Unlike
         * [SpatialArrangement.spacedBy], when layout direction is RTL, children will not be
         * mirrored.
         */
        public fun spacedBy(space: Dp, spatialAlignment: SpatialAlignment.Horizontal): Horizontal {
            return SpacedAligned(space = space, rtlMirror = false, axisMultiplier = 1) {
                occupied,
                totalSize,
                layoutDirection ->
                spatialAlignment.offset(
                    width = occupied,
                    space = totalSize,
                    layoutDirection = layoutDirection,
                )
            }
        }

        /**
         * Children placed next to each other horizontally and align them according to the
         * [spatialAlignment] given. Unlike [SpatialArrangement.spacedBy], when layout direction is
         * RTL, children will not be mirrored.
         */
        public fun aligned(spatialAlignment: SpatialAlignment.Horizontal): Horizontal {
            return SpacedAligned(space = 0.dp, rtlMirror = false, axisMultiplier = 1) {
                occupied,
                totalSize,
                layoutDirection ->
                spatialAlignment.offset(width = occupied, space = totalSize, layoutDirection)
            }
        }
    }

    /**
     * Arrangement with spacing between adjacent children and alignment for the spaced group. Should
     * not be instantiated directly, use [spacedBy] instead.
     *
     * @param space The fixed space [Dp] to be placed between adjacent children.
     * @param rtlMirror If true, the order of children will be reversed when the layout direction is
     *   RTL. This is typically true for standard [SpatialArrangement] and false for
     *   [SpatialArrangement.Absolute] variants.
     * @param axisMultiplier Multiplier for the alignment offset. Typically `1` for horizontal (used
     *   by `SpatialRow`), `-1` for vertical (used by `SpatialColumn`) and `0` if the group should
     *   only be centered without an additional alignment-specific offset (e.g. for
     *   `SpatialArrangement.spacedBy(space: Dp)` which is axis-independent).
     * @param alignment A [CalculateSpatialAlignmentOffset] lambda to calculate the alignment offset
     *   for the entire group of children after spacing is applied. This offset is then influenced
     *   by the [axisMultiplier]. Can be null if no specific alignment beyond centering (when
     *   axisMultiplier is 0) is needed.
     */
    @Immutable
    internal data class SpacedAligned(
        val space: Dp,
        val rtlMirror: Boolean,
        val axisMultiplier: Int,
        val alignment: CalculateSpatialAlignmentOffset?,
    ) : AxisIndependent {

        override val spacing: Dp = space

        override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
            arrange(
                totalSize = totalSize,
                sizes = sizes,
                layoutDirection = LayoutDirection.Ltr,
                outPositions = outPositions,
            )
        }

        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            layoutDirection: LayoutDirection,
            outPositions: IntArray,
        ) {
            if (sizes.isEmpty()) return
            val spacePx = space.roundToPx()

            // Keep track of the occupied space as we iterate through the children.
            var occupied = 0
            // Store the space to be added before the next child.
            var lastSpace = 0
            // Determine if the layout should be reversed based on RTL and mirroring.
            val reversed = rtlMirror && layoutDirection == LayoutDirection.Rtl

            // First pass: Calculate the start position of each child's container (including space).
            // The positions here are from the start of the layout area.
            sizes.forEachIndexed(reversed) { index, it ->
                // Position the child at the current `occupied` space.
                // Ensure it doesn't overflow `totalSize`.
                outPositions[index] = min(occupied, totalSize - it)
                // Calculate the space to add after this child, ensuring it fits.
                lastSpace = min(spacePx, totalSize - outPositions[index] - it)
                // Update `occupied` space to include the child and the following space.
                occupied = outPositions[index] + it + lastSpace
            }

            // Second pass: Adjust positions to be the center of each child.
            sizes.forEachIndexed(reversed) { index, it ->
                outPositions[index] = (outPositions[index] + it / 2.0).fastRoundToInt()
            }

            // Correct the total occupied space by removing the last added space
            // (as there's no child after the last one).
            occupied -= lastSpace

            // If an alignment is specified and there's remaining space, apply the alignment.
            if (alignment != null && occupied < totalSize) {
                // Calculate the shift for the entire group of children.
                // This is based on centering the group in the remaining space,
                // then applying the alignment-specific offset.
                val groupPosition =
                    ((totalSize - occupied) / 2f +
                            (axisMultiplier *
                                alignment.invoke(occupied, totalSize, layoutDirection)))
                        .roundToInt()
                // Apply the calculated group shift to each child's position.
                for (index in outPositions.indices) {
                    outPositions[index] += groupPosition
                }
            }
        }
    }

    /** A functional interface for calculating an alignment offset */
    internal fun interface CalculateSpatialAlignmentOffset {
        /**
         * @param occupied The total size occupied by the children being aligned.
         * @param totalSize The total available size within which the group is being aligned.
         * @param layoutDirection The current layout direction.
         * @return The offset to apply to the group for alignment. The offset is typically
         *   calculated from the center of the layout. A positive offset might shift towards the end
         *   (e.g., right/top) and a negative offset towards the start (e.g., left/bottom),
         *   depending on the `axisMultiplier` in `SpacedAligned`.
         */
        fun invoke(occupied: Int, totalSize: Int, layoutDirection: LayoutDirection): Int
    }

    internal fun placeRightOrBottom(
        totalSize: Int,
        size: IntArray,
        outPosition: IntArray,
        reverseInput: Boolean,
    ) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        var current = totalSize - consumedSize
        size.forEachIndexed(reverseInput) { index, it ->
            outPosition[index] = (current + it / 2.0).fastRoundToInt()
            current += it
        }
    }

    internal fun placeLeftOrTop(size: IntArray, outPosition: IntArray, reverseInput: Boolean) {
        var current = 0
        size.forEachIndexed(reverseInput) { index, it ->
            outPosition[index] = (current + it / 2.0).fastRoundToInt()
            current += it
        }
    }

    internal fun placeCenter(
        totalSize: Int,
        size: IntArray,
        outPosition: IntArray,
        reverseInput: Boolean,
    ) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        var current = (totalSize - consumedSize).toFloat() / 2
        size.forEachIndexed(reverseInput) { index, it ->
            outPosition[index] = (current + it / 2.0).fastRoundToInt()
            current += it.toFloat()
        }
    }

    internal fun placeSpaceEvenly(
        totalSize: Int,
        size: IntArray,
        outPosition: IntArray,
        reverseInput: Boolean,
    ) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        val gapSize = (totalSize - consumedSize).toFloat() / (size.size + 1)
        var current = gapSize
        size.forEachIndexed(reverseInput) { index, it ->
            outPosition[index] = (current + it / 2.0).fastRoundToInt()
            current += it.toFloat() + gapSize
        }
    }

    internal fun placeSpaceBetween(
        totalSize: Int,
        size: IntArray,
        outPosition: IntArray,
        reverseInput: Boolean,
    ) {
        if (size.isEmpty()) return

        val consumedSize = size.fold(0) { a, b -> a + b }
        val noOfGaps = maxOf(size.lastIndex, 1)
        val gapSize = (totalSize - consumedSize).toFloat() / noOfGaps

        var current = 0f
        if (reverseInput && size.size == 1) {
            // If the layout direction is right-to-left and there is only one gap,
            // we start current with the gap size. That forces the single item to be right-aligned.
            current = gapSize
        }
        size.forEachIndexed(reverseInput) { index, it ->
            outPosition[index] = (current + it / 2.0).fastRoundToInt()
            current += it.toFloat() + gapSize
        }
    }

    internal fun placeSpaceAround(
        totalSize: Int,
        size: IntArray,
        outPosition: IntArray,
        reverseInput: Boolean,
    ) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        val gapSize =
            if (size.isNotEmpty()) {
                (totalSize - consumedSize).toFloat() / size.size
            } else {
                0f
            }
        var current = gapSize / 2
        size.forEachIndexed(reverseInput) { index, it ->
            outPosition[index] = (current + it / 2.0).fastRoundToInt()
            current += it.toFloat() + gapSize
        }
    }

    private inline fun IntArray.forEachIndexed(reversed: Boolean, action: (Int, Int) -> Unit) {
        if (!reversed) {
            forEachIndexed(action)
        } else {
            for (i in (size - 1) downTo 0) {
                action(i, get(i))
            }
        }
    }
}
