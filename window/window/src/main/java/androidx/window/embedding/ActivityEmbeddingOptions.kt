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
import androidx.annotation.RestrictTo
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.embedding.OverlayController.Companion.OVERLAY_FEATURE_VERSION

/**
 * Sets the target launching [ActivityStack] to the given [Bundle].
 *
 * The [Bundle] then could be used to launch an [Activity] to the top of the [ActivityStack] through
 * [Activity.startActivity]. If there's a bundle used for customizing how the [Activity] should be
 * started by [ActivityOptions.toBundle] or [androidx.core.app.ActivityOptionsCompat.toBundle], it's
 * suggested to use the bundle to call this method.
 *
 * It is suggested to use a visible [ActivityStack] reported by [SplitController.splitInfoList] or
 * [OverlayController.overlayInfo], or the launching activity will be launched on the default target
 * if the [activityStack] no longer exists in the host task. The default target could be the top of
 * the visible split's secondary [ActivityStack], or the top of the host task.
 *
 * Below samples are use cases to specify the launching [ActivityStack].
 *
 * @sample androidx.window.samples.embedding.launchingOnPrimaryActivityStack
 * @sample androidx.window.samples.embedding.launchingOnOverlayActivityStack
 * @param context The [android.content.Context] that is going to be used for launching activity with
 *   this [Bundle], which is usually be the [android.app.Activity] of the app that hosts the task.
 * @param activityStack The target [ActivityStack] for launching.
 * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion] is less than 5.
 */
@RequiresWindowSdkExtension(5)
fun Bundle.setLaunchingActivityStack(context: Context, activityStack: ActivityStack): Bundle =
    ActivityEmbeddingController.getInstance(context).setLaunchingActivityStack(this, activityStack)

/**
 * Puts [OverlayCreateParams] to [Bundle] to create a singleton-per-task overlay [ActivityStack].
 *
 * The [Bundle] then could be used to launch an [Activity] to the [ActivityStack] through
 * [Activity.startActivity]. If there's a bundle used for customizing how the [Activity] should be
 * started by [ActivityOptions.toBundle] or [androidx.core.app.ActivityOptionsCompat.toBundle], it's
 * suggested to use the bundle to call this method.
 *
 * Below sample shows how to launch an overlay [ActivityStack].
 *
 * If there's an existing overlay [ActivityStack] shown, the existing overlay container may be
 * dismissed or updated based on [OverlayCreateParams.tag] and [activity] because of following
 * constraints:
 * 1. A task can hold only one overlay container at most.
 * 2. An overlay [ActivityStack] tag is unique per process.
 *
 * Belows are possible scenarios:
 * 1. If there's an overlay container with the same `tag` as [OverlayCreateParams.tag] in the same
 *    task as [activity], the overlay container's [OverlayAttributes] will be updated to
 *    [OverlayCreateParams.overlayAttributes], and the activity will be launched on the top of the
 *    overlay [ActivityStack].
 * 2. If there's an overlay container with different `tag` from [OverlayCreateParams.tag] in the
 *    same task as [activity], the existing overlay container will be dismissed, and a new overlay
 *    container will be launched with the new [OverlayCreateParams].
 * 3. If there's an overlay container with the same `tag` as [OverlayCreateParams.tag] in a
 *    different task from [activity], the existing overlay container in the other task will be
 *    dismissed, and a new overlay container will be launched in the task of [activity].
 *
 * Note that the second and the third scenarios may happen at the same time if [activity]'s task
 * holds an overlay container and [OverlayCreateParams.tag] matches an overlay container in a
 * different task.
 *
 * @sample androidx.window.samples.embedding.launchOverlayActivityStackSample
 * @param activity The [Activity] that is going to be used for launching activity with this
 *   [ActivityOptions], which is usually be the [Activity] of the app that hosts the task.
 * @param overlayCreateParams The parameter container to create an overlay [ActivityStack]
 * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion] is less than 6.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
fun Bundle.setOverlayCreateParams(
    activity: Activity,
    overlayCreateParams: OverlayCreateParams
): Bundle =
    OverlayController.getInstance(activity).setOverlayCreateParams(this, overlayCreateParams)
