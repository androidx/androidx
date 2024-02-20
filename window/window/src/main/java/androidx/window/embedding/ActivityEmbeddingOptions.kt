/*
 * Copyright 2023 The Android Open Source Project
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
@file:JvmName("ActivityEmbeddingOptions")

package androidx.window.embedding

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.os.Bundle
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.core.ExperimentalWindowApi

// TODO(b/295993745): Migrate to use bundle
/**
 * Sets the target launching [ActivityStack] to the given [android.app.ActivityOptions].
 *
 * If the device doesn't support setting launching, [UnsupportedOperationException] will be thrown.
 *
 * @param context The [android.content.Context] that is going to be used for launching
 * activity with this [android.app.ActivityOptions], which is usually be the [android.app.Activity]
 * of the app that hosts the task.
 * @param activityStack The target [ActivityStack] for launching.
 * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion] is less than 5.
 */
@ExperimentalWindowApi
@RequiresWindowSdkExtension(5)
fun ActivityOptions.setLaunchingActivityStack(
    context: Context,
    activityStack: ActivityStack
): ActivityOptions = let {
    ActivityEmbeddingController.getInstance(context)
        .setLaunchingActivityStack(this, activityStack.token)
}

// TODO(b/295993745): Migrate to use bundle
/**
 * Sets [android.app.ActivityOptions] target launching [ActivityStack] to match the one that the
 * provided [activity] is in. That is, the [ActivityStack] of the given [activity] is the
 * [ActivityStack] used for launching.
 *
 * If the device doesn't support setting target launching [ActivityStack] or no available
 * [ActivityStack] can be found from the given [activity], [UnsupportedOperationException] will be
 * thrown.
 *
 * @param activity The existing [android.app.Activity] which [ActivityStack] should be used.
 * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion] is less than 5.
 */
@ExperimentalWindowApi
@RequiresWindowSdkExtension(5)
fun ActivityOptions.setLaunchingActivityStack(activity: Activity): ActivityOptions {
    val activityStack =
        ActivityEmbeddingController.getInstance(activity).getActivityStack(activity)
    return if (activityStack != null) {
        setLaunchingActivityStack(activity, activityStack)
    } else {
        throw UnsupportedOperationException("No available ActivityStack found. " +
            "The given activity may not be embedded.")
    }
}

/**
 * Puts [OverlayCreateParams] to [ActivityOptions] bundle to create a singleton-per-task overlay
 * [ActivityStack].
 *
 * To launch an overlay [ActivityStack], callers should call [Activity.startActivity] with the
 * [ActivityOptions] contains [OverlayCreateParams].
 * Below sample shows how to launch an overlay [ActivityStack].
 *
 * If there's an existing overlay [ActivityStack] shown, the existing overlay container may be
 * dismissed or updated based on [OverlayCreateParams.tag] and [activity] because of following
 * constraints:
 *   1. A task can hold only one overlay container at most.
 *   2. An overlay [ActivityStack] tag is unique per process.
 *
 * Belows are possible scenarios:
 *
 * 1. If there's an overlay container with the same `tag` as [OverlayCreateParams.tag] in the same
 *   task as [activity], the overlay container's [OverlayAttributes]
 *   will be updated to [OverlayCreateParams.overlayAttributes].
 *
 * 2. If there's an overlay container with different `tag` from [OverlayCreateParams.tag] in the
 *   same task as [activity], the existing overlay container will be dismissed, and a new overlay
 *   container will be launched with the new [OverlayCreateParams].
 *
 * 3. If there's an overlay container with the same `tag` as [OverlayCreateParams.tag] in a
 *   different task from [activity], the existing overlay container in the other task will be
 *   dismissed, and a new overlay container will be launched in the task of [activity].
 *
 * Note that the second and the third scenarios may happen at the same time if
 * [activity]'s task holds an overlay container and [OverlayCreateParams.tag] matches an overlay
 * container in a different task.
 *
 * @sample androidx.window.samples.embedding.launchOverlayActivityStackSample
 *
 * @param activity The [Activity] that is going to be used for launching activity with this
 * [ActivityOptions], which is usually be the [Activity] of the app that hosts the task.
 * @param overlayCreateParams The parameter container to create an overlay [ActivityStack]
 * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion] is less than 5.
 */
@RequiresWindowSdkExtension(5)
fun Bundle.setOverlayCreateParams(
    activity: Activity,
    overlayCreateParams: OverlayCreateParams
): Bundle = OverlayController.getInstance(activity).setOverlayCreateParams(
    this,
    overlayCreateParams
)
