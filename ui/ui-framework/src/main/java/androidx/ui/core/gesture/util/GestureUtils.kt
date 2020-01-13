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

package androidx.ui.core.gesture.util

import androidx.ui.core.PointerInputChange
import androidx.ui.unit.IntPxSize

/**
 * Utility method that determines if any pointers are currently in [bounds].
 *
 * A pointer is considered in bounds if it is currently down and it's current
 * position is within the provided [bounds]
 *
 * @return True if at least one pointer is in bounds.
 */
fun List<PointerInputChange>.anyPointersInBounds(bounds: IntPxSize) =
    any {
        it.current.down &&
                it.current.position!!.x.value >= 0 &&
                it.current.position!!.x < bounds.width &&
                it.current.position!!.y.value >= 0 &&
                it.current.position!!.y < bounds.height
    }