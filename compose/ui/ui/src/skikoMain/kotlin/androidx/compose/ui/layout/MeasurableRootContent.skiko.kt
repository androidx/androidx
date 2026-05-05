/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.layout

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.Constraints

/**
 * The interface through which composable content can be queried for its size preferences, such as
 * its intrinsic size.
 */
@ExperimentalComposeUiApi
interface MeasurableRootContent : IntrinsicMeasurable {
    /**
     * Measures the content with the given constraints and calls [block] on the resulting
     * [Measured].
     *
     * Returns the result of [block].
     *
     * It is recommended to not hold onto the [Measured] instance beyond the lifetime of the call
     * to [block].
     */
    fun <T> measuringIn(constraints: Constraints, block: (Measured) -> T): T
}
