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
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * A holder for the [Activity] created for SDK.
 *
 * This is passed to SDKs through [SdkSandboxActivityHandlerCompat.onActivityCreated] to notify SDKs
 * about the created [Activity].
 *
 * SDK can add [LifecycleObserver]s into it to observe the [Activity] lifecycle state.
 */
interface ActivityHolder : LifecycleOwner {
    /**
     * The [Activity] created for SDK.
     */
    fun getActivity(): Activity

    /**
     * The [OnBackPressedDispatcher] for the created [Activity].
     */
    fun getOnBackPressedDispatcher(): OnBackPressedDispatcher
}
