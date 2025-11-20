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

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.SpatialEnvironment
import androidx.xr.scenecore.runtime.SpatialEnvironment.Companion.NO_PASSTHROUGH_OPACITY_PREFERENCE
import androidx.xr.scenecore.runtime.SpatialEnvironmentExt
import androidx.xr.scenecore.runtime.SpatialEnvironmentFeature
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Interface for updating the background image/geometry and passthrough settings.
 *
 * The application can set either / both a skybox and a glTF for geometry, then toggle their
 * visibility by enabling or disabling passthrough. The skybox and geometry will be remembered
 * across passthrough mode changes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeSpatialEnvironment : SpatialEnvironment, SpatialEnvironmentExt {
    private var _currentPassthroughOpacity: Float = 0.0f
    private var _isPreferredSpatialEnvironmentActive: Boolean = false
    private var renderingFeature: SpatialEnvironmentFeature? = null

    /**
     * A map storing the registered passthrough opacity listeners, keyed by their associated
     * [Executor].
     *
     * This collection is exposed for testing purposes. It allows tests to inspect the registered
     * listeners or to manually trigger callbacks by iterating over the map and invoking the
     * [Consumer] values. This is useful for verifying that components react correctly to simulated
     * passthrough opacity changes.
     *
     * Note that because the `Executor` is used as the key, only one listener can be associated with
     * each unique `Executor` instance.
     */
    public val passthroughOpacityChangedListenerMap: MutableMap<Executor, Consumer<Float>> =
        mutableMapOf(
            Executor { command -> command.run() } to
                Consumer<Float> { opacity -> _currentPassthroughOpacity = opacity }
        )

    /**
     * A map storing the registered spatial environment state listeners, keyed by their associated
     * [Executor].
     *
     * <p>This collection is exposed for testing purposes. It allows tests to inspect the registered
     * listeners or to manually trigger callbacks by iterating over the map and invoking the
     * [Consumer] values. This is useful for verifying that components react correctly to simulated
     * changes in the active state of the spatial environment.
     *
     * <p>Note that because the `Executor` is used as the key, only one listener can be associated
     * with each unique `Executor` instance.
     */
    public val spatialEnvironmentChangedListenerMap: MutableMap<Executor, Consumer<Boolean>> =
        mutableMapOf(
            Executor { command -> command.run() } to
                Consumer<Boolean> { active -> _isPreferredSpatialEnvironmentActive = active }
        )

    override val currentPassthroughOpacity: Float
        get() = _currentPassthroughOpacity

    override var preferredSpatialEnvironment: SpatialEnvironment.SpatialEnvironmentPreference?
        get() {
            if (renderingFeature == null) {
                throw UnsupportedOperationException(
                    "Did you forget to add scenecore-spatial-rendering in dependencies?"
                )
            }

            return renderingFeature!!.preferredSpatialEnvironment
        }
        set(value) {
            if (renderingFeature == null) {
                throw UnsupportedOperationException(
                    "Did you forget to add scenecore-spatial-rendering in dependencies?"
                )
            }

            renderingFeature!!.preferredSpatialEnvironment = value
        }

    override var preferredPassthroughOpacity: Float = NO_PASSTHROUGH_OPACITY_PREFERENCE

    override fun addOnPassthroughOpacityChangedListener(
        executor: Executor,
        listener: Consumer<Float>,
    ) {
        passthroughOpacityChangedListenerMap[executor] = listener
    }

    override fun removeOnPassthroughOpacityChangedListener(listener: Consumer<Float>) {
        passthroughOpacityChangedListenerMap.values.remove(listener)
    }

    override val isPreferredSpatialEnvironmentActive: Boolean
        get() = _isPreferredSpatialEnvironmentActive

    override fun addOnSpatialEnvironmentChangedListener(
        executor: Executor,
        listener: Consumer<Boolean>,
    ) {
        spatialEnvironmentChangedListenerMap[executor] = listener
    }

    override fun removeOnSpatialEnvironmentChangedListener(listener: Consumer<Boolean>) {
        spatialEnvironmentChangedListenerMap.values.remove(listener)
    }

    override fun onRenderingFeatureReady(feature: SpatialEnvironmentFeature) {
        renderingFeature = feature
    }
}
