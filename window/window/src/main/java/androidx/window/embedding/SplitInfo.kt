/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.embedding

import android.app.Activity
import androidx.window.core.ExperimentalWindowApi

/** Describes a split pair of two containers with activities. */
@ExperimentalWindowApi
class SplitInfo internal constructor(
    val primaryActivityStack: ActivityStack,
    val secondaryActivityStack: ActivityStack,
    val splitRatio: Float
) {
    operator fun contains(activity: Activity): Boolean {
        return primaryActivityStack.contains(activity) ||
            secondaryActivityStack.contains(activity)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SplitInfo) return false

        if (primaryActivityStack != other.primaryActivityStack) return false
        if (secondaryActivityStack != other.secondaryActivityStack) return false
        if (splitRatio != other.splitRatio) return false

        return true
    }

    override fun hashCode(): Int {
        var result = primaryActivityStack.hashCode()
        result = 31 * result + secondaryActivityStack.hashCode()
        result = 31 * result + splitRatio.hashCode()
        return result
    }
}