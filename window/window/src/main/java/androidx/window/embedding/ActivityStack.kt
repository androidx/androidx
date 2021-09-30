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
class ActivityStack(internal val activities: List<Activity>) {
    operator fun contains(activity: Activity): Boolean {
        return activities.contains(activity)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActivityStack) return false

        if (activities != other.activities) return false

        return true
    }

    override fun hashCode(): Int {
        return activities.hashCode()
    }
}