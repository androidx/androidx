/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.runtime.HostDefaultKey
import androidx.compose.runtime.HostDefaultProvider
import androidx.lifecycle.viewmodel.compose.ViewModelStoreOwnerHostDefaultKey
import androidx.navigationevent.compose.NavigationEventDispatcherOwnerHostDefaultKey

internal class HostDefaultProviderImpl(
    private val platformContext: PlatformContext
) : HostDefaultProvider {
    // Note: https://youtrack.jetbrains.com/issue/KT-85051
    // Potentially unsafe casts are intentional here.
    @Suppress("UNCHECKED_CAST")
    override fun <T> getHostDefault(key: HostDefaultKey<T>): T = when (key) {
        NavigationEventDispatcherOwnerHostDefaultKey ->
            platformContext.architectureComponentsOwner.navigationEventDispatcherOwner
        ViewModelStoreOwnerHostDefaultKey ->
            platformContext.architectureComponentsOwner.viewModelStoreOwner
        else -> null
    } as T
}