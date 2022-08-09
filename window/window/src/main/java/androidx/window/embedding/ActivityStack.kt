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

/**
 * A container that holds a stack of activities, overlapping and bound to the same rectangle on the
 * screen.
 */
@ExperimentalWindowApi
class ActivityStack(
    /**
     * The [Activity] list in this application's process that belongs to this ActivityStack.
     *
     * Note that Activities that are running in other processes do not contain in this [Activity]
     * list. They can be in any position in terms of ordering relative to the activities in the
     * list.
     */
    internal val activities: List<Activity>,
    private val isEmpty: Boolean = false
) {

    operator fun contains(activity: Activity): Boolean {
        return activities.contains(activity)
    }

    /**
     * Returns `true` if there's no [Activity] running in this ActivityStack.
     *
     * Note that [activities] only report Activity in the process used to create this
     * ActivityStack. That said, if this ActivityStack only contains activities from other
     * process(es), [activities] will return empty list, and this method will return `false`.
     */
    fun isEmpty(): Boolean {
        return isEmpty
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActivityStack) return false

        return activities == other.activities && isEmpty == other.isEmpty
    }

    override fun hashCode(): Int {
        var result =
            if (isEmpty) {
                1
            } else {
                0
            }
        result = 31 * result + activities.hashCode()
        return result
    }

    override fun toString(): String =
        "ActivityStack{" +
            "activities=$activities" +
            ", isEmpty=$isEmpty" +
            "}"
}