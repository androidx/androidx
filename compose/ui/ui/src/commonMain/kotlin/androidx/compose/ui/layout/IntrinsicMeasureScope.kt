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

package androidx.compose.ui.layout

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * The receiver scope of a layout's intrinsic measurements lambdas.
 */
interface IntrinsicMeasureScope : Density {
    /**
     * The [LayoutDirection] of the `Layout` or `LayoutModifier` using the measure scope
     * to measure their children.
     */
    val layoutDirection: LayoutDirection

    /**
     * This indicates whether the ongoing measurement is for lookahead pass.
     * [IntrinsicMeasureScope] implementations, especially [MeasureScope] implementations should
     * override this flag to reflect whether the measurement is intended for lookahead pass.
     *
     * @sample androidx.compose.ui.samples.animateContentSizeAfterLookaheadPass
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalComposeUiApi
    @ExperimentalComposeUiApi
    val isLookingAhead: Boolean
        get() = false
}