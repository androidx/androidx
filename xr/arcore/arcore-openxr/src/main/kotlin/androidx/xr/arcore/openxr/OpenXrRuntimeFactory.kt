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
package androidx.xr.arcore.openxr

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.interfaces.Feature
import androidx.xr.runtime.internal.LibraryNotLinkedException
import androidx.xr.runtime.internal.PerceptionRuntimeFactory
import kotlin.coroutines.CoroutineContext

/** Factory for creating instances of [OpenXrRuntime]. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
// TODO: b/452158733 - Make this class internal once YTXR has been migrated.
public class OpenXrRuntimeFactory() : PerceptionRuntimeFactory {
    private companion object {
        private const val LIBRARY_NAME: String = "androidx.xr.arcore.openxr"
    }

    override val requirements: Set<Feature> = setOf(Feature.FULLSTACK, Feature.OPEN_XR)

    override fun createRuntime(
        context: Context,
        coroutineContext: CoroutineContext,
    ): PerceptionRuntime {
        try {
            System.loadLibrary(LIBRARY_NAME)
        } catch (_: UnsatisfiedLinkError) {
            throw LibraryNotLinkedException(LIBRARY_NAME)
        }
        val timeSource = OpenXrTimeSource()
        val perceptionManager = OpenXrPerceptionManager(timeSource)
        return OpenXrRuntime(context, perceptionManager, timeSource)
    }
}
