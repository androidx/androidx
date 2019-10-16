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

package androidx.ui.core

/**
 * A [Modifier.Element] that changes the way a UI component is measured and laid out.
 */
interface LayoutModifier : Modifier.Element {
    /**
     * Modifies [constraints] for performing measurement of the modified layout element.
     */
    fun DensityScope.modifyConstraints(constraints: Constraints): Constraints

    /**
     * Returns the container size of a modified layout element given the original container
     * measurement [constraints] and the measured [childSize].
     */
    fun DensityScope.modifySize(constraints: Constraints, childSize: IntPxSize): IntPxSize

    /**
     * Determines the modified minimum intrinsic width of [measurable].
     * See [Measurable.minIntrinsicWidth].
     */
    fun DensityScope.minIntrinsicWidthOf(measurable: Measurable, height: IntPx): IntPx

    /**
     * Determines the modified maximum intrinsic width of [measurable].
     * See [Measurable.maxIntrinsicWidth].
     */
    fun DensityScope.maxIntrinsicWidthOf(measurable: Measurable, height: IntPx): IntPx

    /**
     * Determines the modified minimum intrinsic height of [measurable].
     * See [Measurable.minIntrinsicHeight].
     */
    fun DensityScope.minIntrinsicHeightOf(measurable: Measurable, width: IntPx): IntPx

    /**
     * Determines the modified maximum intrinsic height of [measurable].
     * See [Measurable.maxIntrinsicHeight].
     */
    fun DensityScope.maxIntrinsicHeightOf(measurable: Measurable, width: IntPx): IntPx

    /**
     * Returns the position of a modified child of size [childSize] within a container of
     * size [containerSize].
     */
    fun DensityScope.modifyPosition(
        childPosition: IntPxPosition,
        childSize: IntPxSize,
        containerSize: IntPxSize
    ): IntPxPosition

    /**
     * Returns the modified position of [line] given its unmodified [value].
     */
    fun DensityScope.modifyAlignmentLine(
        line: AlignmentLine,
        value: IntPx?
    ): IntPx?

    /**
     * Provides a parentData given the [parentData] already provided through the modifier's chain.
     */
    fun DensityScope.modifyParentData(parentData: Any?): Any?
}
