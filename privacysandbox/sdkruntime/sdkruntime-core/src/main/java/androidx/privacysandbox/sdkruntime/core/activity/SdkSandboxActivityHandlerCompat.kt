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

package androidx.privacysandbox.sdkruntime.core.activity

import android.app.Activity
import android.app.sdksandbox.sdkprovider.SdkSandboxActivityHandler

/**
 * This is used to notify the SDK when an [Activity] is created for it.
 * @see SdkSandboxActivityHandler
 */
interface SdkSandboxActivityHandlerCompat {

    /**
     * Notifies SDK when an [Activity] gets created.
     *
     * This function is called synchronously from the main thread of the [Activity] that is getting
     * created.
     *
     * SDK is expected to call [Activity.setContentView] to the passed [Activity] object to populate
     * the view.
     *
     * @param activityHolder the [ActivityHolder] which holds the [Activity] which gets created
     * @see SdkSandboxActivityHandler.onActivityCreated
     */
    fun onActivityCreated(activityHolder: ActivityHolder)
}