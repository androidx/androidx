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

package androidx.camera.core.impl.utils

import android.util.Range

/** Utility class for [Range] related operations. */
public object RangeUtil {

    /**
     * Filters the fixed ranges (where upper equals lower) from this set.
     *
     * The output [Set] preserves the iteration order from the original receiver.
     */
    @JvmStatic
    public fun Set<Range<Int>>.filterFixedRanges(): Set<Range<Int>> =
        LinkedHashSet(filter { it.upper == it.lower })
}
