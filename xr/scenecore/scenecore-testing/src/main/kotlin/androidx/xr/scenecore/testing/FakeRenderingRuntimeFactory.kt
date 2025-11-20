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

package androidx.xr.scenecore.testing

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.Feature
import androidx.xr.runtime.internal.JxrRuntime
import androidx.xr.runtime.internal.RenderingRuntimeFactory
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime

/** Factory for creating test-only instances of [androidx.xr.scenecore.runtime.RenderingRuntime]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeRenderingRuntimeFactory() : RenderingRuntimeFactory {
    override val requirements: Set<Feature> = emptySet()

    override fun create(runtimes: List<JxrRuntime>, activity: Activity): RenderingRuntime =
        FakeRenderingRuntime(runtimes.filterIsInstance<SceneRuntime>().first())
}
