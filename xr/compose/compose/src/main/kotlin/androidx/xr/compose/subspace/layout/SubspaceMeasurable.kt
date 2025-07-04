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
 * A part of the composition layout that can be measured.
 *
 * Based on [androidx.compose.ui.layout.Measurable].
 */
public interface SubspaceMeasurable {
    /**
     * Measures the layout with [VolumeConstraints], returning a [SubspacePlaceable] layout that has
     * its new size.
     */
    public fun measure(constraints: VolumeConstraints): SubspacePlaceable

    /**
     * Adjusts layout with a new [ParentLayoutParamsAdjustable].
     *
     * This is useful in implementations of [SubspaceMeasurePolicy.measure] to provide child
     * measurables with the context objects they need to inform their parent of their layout
     * preferences using [ParentLayoutParamsModifier.adjustParams].
     *
     * ```kotlin
     * // Child composable modifiers can modify MyParams which can be read by the parent.
     * var myParams = MyParams().also { measurable.adjustParams(it) }
     * ```
     */
    public fun adjustParams(params: ParentLayoutParamsAdjustable)
}
