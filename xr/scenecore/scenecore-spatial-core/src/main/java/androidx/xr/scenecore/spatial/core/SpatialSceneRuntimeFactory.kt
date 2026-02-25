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

package androidx.xr.scenecore.spatial.core

import android.app.Activity
import androidx.xr.runtime.interfaces.Feature
import androidx.xr.runtime.internal.SceneRuntimeFactory
import androidx.xr.scenecore.runtime.SceneRuntime
import java.util.concurrent.Executors

/**
 * Factory for creating instances of [androidx.xr.scenecore.runtime.SceneRuntime] for devices that
 * support the [Feature.SPATIAL] feature.
 */
internal class SpatialSceneRuntimeFactory() : SceneRuntimeFactory {
    override val requirements: Set<Feature> =
        setOf(Feature.FULLSTACK, Feature.OPEN_XR, Feature.SPATIAL)

    override fun create(activity: Activity): SceneRuntime =
        SpatialSceneRuntime.create(
            activity,
            Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "JXRRuntimeSession") },
        )
}
