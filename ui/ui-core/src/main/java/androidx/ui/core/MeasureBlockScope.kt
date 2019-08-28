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
 * The receiver scope of a layout's measure lambda. The return value of the
 * measure lambda is [LayoutResult], which should be returned by [layout]
 */
interface MeasureBlockScope : DensityReceiver {
    /**
     * Sets the size and alignment lines of the measured layout, and assigns the positioning block.
     * The [positioningBlock] is a lambda used for positioning children. [Placeable.place] should
     * be called on children inside [positioningBlock].
     * The alignment lines can be used by the parent layouts to decide layout, and can be queried
     * using the [Placeable.get] operator. Note that alignment lines will be inherited by parent
     * layouts, such that indirect parents will be able to query them as well.
     *
     * @param width the measured width of the layout
     * @param height the measured height of the layout
     * @param alignmentLines the alignment lines defined by the layout
     * @param positioningBlock block defining the children positioning of the current layout
     */
    fun layout(
        width: IntPx,
        height: IntPx,
        vararg alignmentLines: Pair<AlignmentLine, IntPx>,
        positioningBlock: PositioningBlockScope.() -> Unit
    ): LayoutResult
}
