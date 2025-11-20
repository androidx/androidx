/*
 * Copyright 2024 The Android Open Source Project
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

import android.app.Activity
import androidx.annotation.RestrictTo
import kotlin.coroutines.CoroutineContext

/** Factory for creating instances of Runtime. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface PerceptionRuntimeFactory : Service {
    /**
     * Creates a [JxrRuntime] instance with a specified [CoroutineContext].
     *
     * The provided [coroutineContext] will be used for any asynchronous operations initiated by the
     * runtime.
     *
     * @param activity The host [Activity].
     * @param coroutineContext The [CoroutineContext] for the runtime to use.
     */
    public fun createRuntime(activity: Activity, coroutineContext: CoroutineContext): JxrRuntime
}
