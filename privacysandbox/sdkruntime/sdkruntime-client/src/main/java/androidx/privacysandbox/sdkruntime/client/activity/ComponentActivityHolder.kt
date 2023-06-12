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

package androidx.privacysandbox.sdkruntime.client.activity

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.Lifecycle
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder

/**
 * Simple implementation of [ActivityHolder] for [ComponentActivity].
 */
internal class ComponentActivityHolder(
    private val activity: ComponentActivity
) : ActivityHolder {
    override fun getActivity(): Activity = activity

    override fun getOnBackPressedDispatcher(): OnBackPressedDispatcher =
        activity.onBackPressedDispatcher

    override val lifecycle: Lifecycle
        get() = activity.lifecycle
}