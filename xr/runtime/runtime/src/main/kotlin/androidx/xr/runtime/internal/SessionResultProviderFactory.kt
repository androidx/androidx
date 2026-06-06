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

package androidx.xr.runtime.internal

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.xr.runtime.interfaces.Service
import kotlinx.coroutines.CoroutineScope

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface SessionResultProviderFactory : Service {
    /**
     * Creates a [SessionResultProvider] instance with a specified [CoroutineScope].
     *
     * The provided [coroutineScope] will be used for any asynchronous operations initiated by the
     * provider.
     *
     * @param context The host [Context].
     * @param coroutineScope The [CoroutineScope] for the provider to use.
     */
    public fun createProvider(
        context: Context,
        coroutineScope: CoroutineScope,
    ): SessionResultProvider
}
