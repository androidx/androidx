/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.xr.compose.unit.VolumeConstraints

/**
 * Defines the measure and layout behavior of a [SubspaceLayout].
 *
 * [SubspaceLayout] and [SubspaceMeasurePolicy] are the way Compose XR layouts (such as SpatialBox,
 * SpatialColumn, etc.) are built, and they can also be used to achieve custom layouts.
 *
 * Based on [androidx.compose.ui.layout.MeasurePolicy].
 */
public fun interface SubspaceMeasurePolicy {
    /**
     * The function that defines the measurement and layout. Each [SubspaceMeasurable] in the
     * [measurables] list corresponds to a layout child of the layout, and children can be measured
     * using the [SubspaceMeasurable.measure] method. This method takes the [VolumeConstraints]
     * which the child should respect; different children can be measured with different
     * constraints.
     *
     * [SubspaceMeasureResult] objects are usually created using the [SubspaceMeasureScope.layout]
     * factory, which takes the calculated size of this layout, its alignment lines, and a block
     * defining the positioning of the children layouts.
     */
    public fun SubspaceMeasureScope.measure(
        measurables: List<SubspaceMeasurable>,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult
}
