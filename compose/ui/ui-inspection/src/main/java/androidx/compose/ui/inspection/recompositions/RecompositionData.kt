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

package androidx.compose.ui.inspection.recompositions

/** The [RecompositionData] stored per @Composable. */
open class RecompositionData {
    // The number of recompositions
    var count = 0
        private set

    // The number of recomposition skips
    var skips = 0
        private set

    // True, if the last count increment was skipped
    var lastCountWasSkipped = false
        private set

    open fun incrementCount() {
        count++
        lastCountWasSkipped = false
    }

    fun incrementSkip() {
        count--
        skips++
        lastCountWasSkipped = true
    }
}
