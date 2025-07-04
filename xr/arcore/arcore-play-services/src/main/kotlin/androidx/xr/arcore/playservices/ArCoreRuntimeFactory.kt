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

package androidx.xr.arcore.playservices

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.Feature
import androidx.xr.runtime.internal.Runtime
import androidx.xr.runtime.internal.RuntimeFactory

/** Factory for creating instances of [ArCoreXrRuntime]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ArCoreRuntimeFactory() : RuntimeFactory {
    override val requirements: Set<Feature> = setOf(Feature.FULLSTACK)

    override fun createRuntime(activity: Activity): Runtime {
        // b/396235304 -- Provide a way to configure the session.
        val timeSource = ArCoreTimeSource()
        val perceptionManager = ArCorePerceptionManager(timeSource)
        val manager = ArCoreManager(activity, perceptionManager, timeSource)

        return ArCoreRuntime(manager, perceptionManager)
    }
}
