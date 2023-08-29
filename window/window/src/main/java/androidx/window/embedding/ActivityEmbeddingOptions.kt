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
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.core.ExperimentalWindowApi

/**
 * Sets the launching [ActivityStack] to the given [android.app.ActivityOptions].
 *
 * If the device doesn't support setting launching, [UnsupportedOperationException] will be thrown.
 *
 * @param context The [android.content.Context] that is going to be used for launching
 * activity with this [android.app.ActivityOptions], which is usually be the [android.app.Activity]
 * of the app that hosts the task.
 * @param activityStack The target [ActivityStack] for launching.
 * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion] is less than 3.
 */
@ExperimentalWindowApi
@RequiresWindowSdkExtension(3)
fun ActivityOptions.setLaunchingActivityStack(
    context: Context,
    activityStack: ActivityStack
): ActivityOptions = let {
    ActivityEmbeddingController.getInstance(context)
        .setLaunchingActivityStack(this, activityStack.token)
}

/**
 * Sets the launching [ActivityStack] to the [android.app.ActivityOptions] by the
 * given [activity]. That is, the [ActivityStack] of the given [activity] is the
 * [ActivityStack] used for launching.
 *
 * If the device doesn't support setting launching or no available [ActivityStack]
 * can be found from the given [activity], [UnsupportedOperationException] will be thrown.
 *
 * @param activity The existing [android.app.Activity] on the target [ActivityStack].
 * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion] is less than 3.
 */
@ExperimentalWindowApi
@RequiresWindowSdkExtension(3)
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
