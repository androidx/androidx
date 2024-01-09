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
import android.content.Context
import android.os.Bundle
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
     * Started from [WindowSdkExtensions.extensionVersion] 5, this method can also obtain
     * standalone [ActivityStack], which is not associated with any [SplitInfo]. For example,
     * an [ActivityStack] launched with [ActivityRule.alwaysExpand], or an overlay [ActivityStack]
     * launched by [setLaunchingActivityStack] with [OverlayCreateParams].
     *
     * @param activity The [Activity] to check.
     * @return the [ActivityStack] that this [activity] is part of, or `null` if there is no such
     * [ActivityStack].
     */
    @ExperimentalWindowApi
    fun getActivityStack(activity: Activity): ActivityStack? =
        backend.getActivityStack(activity)

    /**
     * Sets the launching [ActivityStack] to the given [Bundle].
     *
     * Apps can launch an [Activity] into the [ActivityStack] associated with [token] by
     * [Activity.startActivity].
     *
     * @param options The [Bundle] to be updated.
     * @param token The token of the [ActivityStack] to be set.
     */
    @RequiresWindowSdkExtension(5)
    internal fun setLaunchingActivityStack(
        options: Bundle,
        activityStack: ActivityStack
    ): Bundle = backend.setLaunchingActivityStack(options, activityStack)

    /**
     * Finishes a set of [activityStacks][ActivityStack] from the lowest to the highest z-order
     * regardless of the order of `activityStack` passed in the input parameter.
     *
     * If a remaining activityStack from a split participates in other splits with
     * other activityStacks, the remaining activityStack might split with other activityStacks.
     * For example, if activityStack A splits with activityStack B and C, and activityStack C covers
     * activityStack B, finishing activityStack C might make the split of activityStack A and B
     * show.
     *
     * If all split-associated activityStacks are finished, the remaining activityStack will
     * be expanded to fill the parent task container. This is useful to expand the primary
     * container as the sample linked below shows.
     *
     * **Note** that it's caller's responsibility to check whether this API is supported by checking
     * [WindowSdkExtensions.extensionVersion] is greater than or equal to 5. If not, an alternative
     * approach to finishing all containers above a particular activity can be to launch it again
     * with flag [android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP].
     *
     * @param activityStacks The set of [ActivityStack] to be finished.
     * @throws UnsupportedOperationException if extension version is less than 5.
     * @sample androidx.window.samples.embedding.expandPrimaryContainer
     */
    @ExperimentalWindowApi
    @RequiresWindowSdkExtension(5)
    fun finishActivityStacks(activityStacks: Set<ActivityStack>) {
        backend.finishActivityStacks(activityStacks)
    }

    /**
     * Sets the [EmbeddingConfiguration] of the Activity Embedding environment that defines how the
     * embedded Activities behaves.
     *
     * The [EmbeddingConfiguration] can be supported only if the vendor API level of the target
     * device is equals or higher than required API level. Otherwise, it would be no-op when setting
     * the [EmbeddingConfiguration] on a target device that has lower API level.
     *
     * In addition, the existing configuration in the library won't be overwritten if the properties
     * of the given [embeddingConfiguration] are undefined. Only the configuration properties that
     * are explicitly set will be updated.
     *
     * **Note** that it is recommended to be configured in the [androidx.startup.Initializer] or
     * [android.app.Application.onCreate], so that the [EmbeddingConfiguration] is applied early
     * in the application startup, before any activities complete initialization. The
     * [EmbeddingConfiguration] updates afterward may or may not apply to already running
     * activities.
     *
     * @param embeddingConfiguration The [EmbeddingConfiguration]
     */
    @ExperimentalWindowApi
    @RequiresWindowSdkExtension(5)
    fun setEmbeddingConfiguration(embeddingConfiguration: EmbeddingConfiguration) {
        backend.setEmbeddingConfiguration(embeddingConfiguration)
    }

    /**
     * Triggers calculator functions set through [SplitController.setSplitAttributesCalculator] and
     * [OverlayController.setOverlayAttributesCalculator] to update attributes for visible
     * [activityStacks][ActivityStack].
     *
     * This method can be used when the application wants to update the embedding presentation based
     * on the application state.
     *
     * This method is not needed for changes that are driven by window and device state changes or
     * new activity starts, because those will invoke the calculator functions
     * automatically.
     *
     * Visible [activityStacks][ActivityStack] are usually the last element of [SplitInfo]
     * list which was received from the callback registered in [SplitController.splitInfoList] and
     * an active overlay [ActivityStack] if exists.
     *
     * The call will be no-op if there is no visible [activityStacks][ActivityStack] or there's no
     * calculator set.
     *
     * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion]
     *                                       is less than 3.
     * @see androidx.window.embedding.OverlayController.setOverlayAttributesCalculator
     * @see androidx.window.embedding.SplitController.setSplitAttributesCalculator
     */
    @RequiresWindowSdkExtension(3)
    fun invalidateVisibleActivityStacks() {
        backend.invalidateVisibleActivityStacks()
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
