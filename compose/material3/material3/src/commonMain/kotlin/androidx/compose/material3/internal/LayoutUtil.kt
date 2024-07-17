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

package androidx.compose.material3.internal

import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.LayoutIdParentData
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints

internal val IntrinsicMeasurable.layoutId: Any?
    get() = (parentData as? LayoutIdParentData)?.layoutId

internal val Placeable?.widthOrZero: Int
    get() = this?.width ?: 0

internal val Placeable?.heightOrZero: Int
    get() = this?.height ?: 0

internal fun Int.subtractConstraintSafely(other: Int): Int {
    if (this == Constraints.Infinity) {
        return this
    }
    return this - other
}
