/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.xr.arcore.projected

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.internal.Feature
import androidx.xr.runtime.internal.PerceptionRuntimeFactory
import kotlin.coroutines.CoroutineContext

/** Factory for creating instances of [ProjectedRuntime]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ProjectedRuntimeFactory() : PerceptionRuntimeFactory {
    override val requirements: Set<Feature> = setOf(Feature.PROJECTED, Feature.FULLSTACK)

    override fun createRuntime(
        activity: Activity,
        coroutineContext: CoroutineContext,
    ): PerceptionRuntime {
        val timeSource = ProjectedTimeSource()
        val perceptionManager = ProjectedPerceptionManager(timeSource)
        val manager = ProjectedManager(activity, perceptionManager, timeSource, coroutineContext)
        return ProjectedRuntime(manager, perceptionManager)
    }
}
