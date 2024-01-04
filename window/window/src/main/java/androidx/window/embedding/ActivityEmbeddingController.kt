/*
 * Copyright 2022 The Android Open Source Project
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
import android.app.ActivityOptions
import android.content.Context
import android.os.IBinder
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.core.ExperimentalWindowApi

/**
 * The controller that allows checking the current [Activity] embedding status.
 */
class ActivityEmbeddingController internal constructor(private val backend: EmbeddingBackend) {
    /**
     * Checks if the [activity] is embedded and its presentation may be customized by the host
     * process of the task this [activity] is associated with.
     *
     * @param activity the [Activity] to check.
     */
    // TODO(b/204399167) Migrate to a Flow
    fun isActivityEmbedded(activity: Activity): Boolean =
        backend.isActivityEmbedded(activity)

    /**
     * Returns the [ActivityStack] that this [activity] is part of when it is being organized in the
     * embedding container and associated with a [SplitInfo]. Returns `null` if there is no such
     * [ActivityStack].
     *
     * @param activity The [Activity] to check.
     * @return the [ActivityStack] that this [activity] is part of, or `null` if there is no such
     * [ActivityStack].
     */
    @ExperimentalWindowApi
    fun getActivityStack(activity: Activity): ActivityStack? =
        backend.getActivityStack(activity)

    /**
     * Sets the launching [ActivityStack] to the given [android.app.ActivityOptions].
     *
     * @param options The [android.app.ActivityOptions] to be updated.
     * @param token The token of the [ActivityStack] to be set.
     */
    @RequiresWindowSdkExtension(3)
    internal fun setLaunchingActivityStack(
        options: ActivityOptions,
        token: IBinder
    ): ActivityOptions {
        return backend.setLaunchingActivityStack(options, token)
    }

    /**
     * Finishes a set of [activityStacks][ActivityStack] from the lowest to the highest z-order
     * regardless of the order of [ActivityStack] set.
     *
     * If the remaining [ActivityStack] from a split participates in other splits with other
     * `activityStacks`, they might be showing instead. For example, if activityStack A splits with
     * activityStack B and C, and activityStack C covers activityStack B, finishing activityStack C
     * might make the split of activityStack A and B show.
     *
     * If all associated `activityStacks` of a [ActivityStack] are finished, the [ActivityStack]
     * will be expanded to fill the parent task container. This is useful to expand the primary
     * container as the sample linked below shows.
     *
     * **Note** that it's caller's responsibility to check whether this API is supported by checking
     * [WindowSdkExtensions.extensionVersion] is greater than or equal to 3. If not, an alternative
     * approach to finishing all containers above a particular activity can be to launch it again
     * with flag [android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP].
     *
     * @param activityStacks The set of [ActivityStack] to be finished.
     * @throws UnsupportedOperationException if extension version is less than 3.
     * @sample androidx.window.samples.embedding.expandPrimaryContainer
     */
    @ExperimentalWindowApi
    @RequiresWindowSdkExtension(3)
    fun finishActivityStacks(activityStacks: Set<ActivityStack>) {
        backend.finishActivityStacks(activityStacks)
    }

    companion object {
        /**
         * Obtains an instance of [ActivityEmbeddingController].
         *
         * @param context the [Context] to initialize the controller with
         */
        @JvmStatic
        fun getInstance(context: Context): ActivityEmbeddingController {
            val backend = EmbeddingBackend.getInstance(context)
            return ActivityEmbeddingController(backend)
        }
    }
}
