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

package androidx.xr.runtime

import android.content.Context
import androidx.xr.runtime.interfaces.Feature
import androidx.xr.runtime.internal.JxrRuntime
import androidx.xr.runtime.internal.PerceptionRuntimeFactory
import kotlinx.coroutines.CoroutineScope

/** Stub factory for creating a [StubPerceptionRuntime] for testing purposes. */
internal class StubPerceptionRuntimeFactory : PerceptionRuntimeFactory {
    internal companion object {
        /**
         * Will be thrown by the [StubPerceptionRuntime] during [StubPerceptionRuntime.initialize].
         */
        internal var lifecycleCreateException: Exception? = null
        internal var hasCreatePermission: Boolean = true
    }

    override val requirements: Set<Feature> = emptySet()

    override fun createRuntime(context: Context, coroutineScope: CoroutineScope): JxrRuntime {
        println(hasCreatePermission)
        return StubPerceptionRuntime(hasCreatePermission)
    }
}
