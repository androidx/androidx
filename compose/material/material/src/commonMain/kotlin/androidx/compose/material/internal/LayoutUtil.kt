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

package androidx.compose.material.internal

import androidx.compose.ui.unit.Constraints

/**
 * Subtracts one value from another, where both values represent constraints used in layout.
 *
 * Notably:
 * - if [this] is [Constraints.Infinity], the result stays [Constraints.Infinity]
 * - the result is coerced to be non-negative
 */
internal fun Int.subtractConstraintSafely(other: Int): Int {
    if (this == Constraints.Infinity) {
        return this
    }
    return (this - other).coerceAtLeast(0)
}
