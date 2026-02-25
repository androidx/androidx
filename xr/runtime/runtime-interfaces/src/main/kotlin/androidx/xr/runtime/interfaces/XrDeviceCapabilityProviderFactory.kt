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

package androidx.xr.runtime.interfaces

import android.content.Context
import androidx.annotation.RestrictTo
import kotlin.coroutines.CoroutineContext

/** Factory for creating instances of an [XrDeviceCapabilityProvider]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface XrDeviceCapabilityProviderFactory : Service {

    /**
     * Creates an [XrDeviceCapabilityProvider].
     *
     * @param context The [Context] used to select an appropriate [XrDeviceCapabilityProvider].
     * @param coroutineContext The [CoroutineContext] used to execute background operations.
     */
    public fun create(
        context: Context,
        coroutineContext: CoroutineContext,
    ): XrDeviceCapabilityProvider
}
