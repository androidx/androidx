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
import android.os.IBinder
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP

/**
 * A container that holds a stack of activities, overlapping and bound to the same rectangle on the
 * screen.
 */
class ActivityStack @RestrictTo(LIBRARY_GROUP) constructor(
    /**
     * The [Activity] list in this application's process that belongs to this [ActivityStack].
     *
     * Note that Activities that are running in other processes will not be contained in this
     * list. They can be in any position in terms of ordering relative to the activities in the
     * list.
     */
    internal val activitiesInProcess: List<Activity>,
    /**
     * Whether there is no [Activity] running in this [ActivityStack].
     *
     * Note that [activitiesInProcess] only report [Activity] in the process used to create this
     * ActivityStack. That said, if this ActivityStack only contains activities from other
     * process(es), [activitiesInProcess] will return an empty list, but this method will return
     * `false`.
     */
    val isEmpty: Boolean,
    /**
     * A token uniquely identifying this `ActivityStack`.
     */
    internal val token: IBinder,
) {

    /**
     * Whether this [ActivityStack] contains the [activity].
     */
    operator fun contains(activity: Activity): Boolean {
        return activitiesInProcess.contains(activity)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActivityStack) return false

        if (activitiesInProcess != other.activitiesInProcess) return false
        if (isEmpty != other.isEmpty) return false
        if (token != other.token) return false

        return true
    }

    override fun hashCode(): Int {
        var result = activitiesInProcess.hashCode()
        result = 31 * result + isEmpty.hashCode()
        result = 31 * result + token.hashCode()
        return result
    }

    override fun toString(): String =
        "ActivityStack{" +
            "activitiesInProcess=$activitiesInProcess" +
            ", isEmpty=$isEmpty" +
            ", token=$token" +
            "}"
}
