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

import com.google.r4a.Children

/**
 * Measure operations to be used with [MeasureBox].
 * Used to mask away intrinsics inside [MeasureBox].
 */
// TODO(mount/popam): Decide if this should not exist in core -- it may be good to define this
// in ui-framework and have a private API used by framework.
interface MeasureOperations {
    /**
     * Compose [children] into the [MeasureBox] and return a list of [Measurable]s within
     * the children. Composition stops at [MeasureBox] children. Further composition requires
     * calling [Measurable.measure].
     */
    // TODO(popam): prevent collect from happening before every intrinsic measurement
    fun collect(@Children children: () -> Unit): List<Measurable>

    /**
     * Measure the child [Measurable] with a specific set of [Constraints]. The result
     * is a [Placeable], which can be used inside the [layout] method to position the child.
     */
    fun measure(measurable: Measurable, constraints: Constraints): Placeable

    /**
     * Sets the width and height of the current layout. The lambda is used to perform the
     * calls to [Placeable.place], defining the positions of the children relative to the current
     * layout.
     */
    fun layout(width: Dimension, height: Dimension, block: () -> Unit)
}