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

package androidx.xr.scenecore.testing

import androidx.annotation.FloatRange
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.testing.internal.FakeSceneRuntime as InternalFakeSceneRuntime

/**
 * A test utility for accessing and inspecting the spatial data associated with the
 * [SpatialEnvironment].
 */
public class SpatialEnvironmentTester internal constructor() {

    internal companion object {
        /** The test data accessor for the [SpatialEnvironment]. */
        internal val instance = SpatialEnvironmentTester()
    }

    /**
     * Simulates a change in the user-visible passthrough opacity, notifying all registered
     * listeners.
     *
     * This affects the value returned by [SpatialEnvironment.currentPassthroughOpacity] and
     * triggers callbacks registered via [SpatialEnvironment.addPassthroughOpacityChangedListener].
     *
     * A value of 0.0f means no passthrough is shown, and a value of 1.0f means the passthrough
     * completely obscures the spatial environment geometry and skybox.
     *
     * @param opacity The current passthrough opacity value between 0.0f and 1.0f.
     */
    public fun triggerPassthroughOpacityChanged(@FloatRange(from = 0.0, to = 1.0) opacity: Float) {
        require(opacity in 0.0f..1.0f) { "Opacity must be between 0.0f and 1.0f, but was $opacity" }
        checkNotNull(InternalFakeSceneRuntime.instance)
            .spatialEnvironment
            .passthroughOpacityChangedListenerMap
            .toList()
            .forEach { (consumer, executor) -> executor.execute { consumer.accept(opacity) } }
    }

    /**
     * Simulates a change in the active state of the preferred spatial environment, notifying all
     * registered listeners.
     *
     * This affects the value returned by [SpatialEnvironment.isPreferredSpatialEnvironmentActive]
     * and triggers callbacks registered via
     * [SpatialEnvironment.addSpatialEnvironmentChangedListener].
     *
     * @param active True if the environment preference is active.
     */
    public fun triggerSpatialEnvironmentChanged(active: Boolean) {
        checkNotNull(InternalFakeSceneRuntime.instance)
            .spatialEnvironment
            .spatialEnvironmentChangedListenerMap
            .toList()
            .forEach { (consumer, executor) -> executor.execute { consumer.accept(active) } }
    }
}
