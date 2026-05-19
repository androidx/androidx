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

import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.PanelClippingConfig
import androidx.xr.scenecore.Scene
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.SpatialModeChangeEvent
import androidx.xr.scenecore.SpatialVisibility
import androidx.xr.scenecore.testing.internal.FakeSceneRuntime as InternalFakeSceneRuntime
import androidx.xr.scenecore.toSpatialCapabilities

/**
 * A test utility for accessing and inspecting the data associated with the
 * [androidx.xr.scenecore.Scene].
 */
public class SceneTester internal constructor(private val runtime: InternalFakeSceneRuntime) {

    /**
     * The current clipping configuration of all panels in the [Scene].
     *
     * This property reflects the result of setting the [Scene.panelClippingConfig] value.
     *
     * @see PanelClippingConfig
     */
    public val panelClippingConfig: PanelClippingConfig
        get() = PanelClippingConfig(runtime.enabledPanelDepthTest)

    /**
     * Simulates the spatial capabilities of the [Session].
     *
     * This property's value changes in response to calls to [Scene.requestFullSpaceMode] and
     * [Scene.requestHomeSpaceMode]. Any listeners registered via
     * [Scene.addSpatialCapabilitiesChangedListener] are triggered upon a change in this value.
     */
    public var spatialCapabilities: Set<SpatialCapability>
        get() = runtime.spatialCapabilities.toSpatialCapabilities()
        set(value) {
            runtime.spatialCapabilities = value.toRtSpatialCapabilities()
        }

    /**
     * Simulates a runtime spatial mode change by dispatching the provided [spatialModeChangeEvent]
     * to the [Scene].
     *
     * This will trigger the custom listener registered via [Scene.setSpatialModeChangedListener].
     * If no custom listener is registered, or if it has been cleared via
     * [Scene.clearSpatialModeChangedListener], this invokes the default internal listener which
     * automatically applies the recommended pose and scale from the event to the [Scene.keyEntity].
     *
     * Note: The listener is executed asynchronously on the [java.util.concurrent.Executor]
     * configured for the listener (defaults to the main thread).
     *
     * @param spatialModeChangeEvent The simulated [SpatialModeChangeEvent] containing the
     *   recommended pose and scale to dispatch.
     */
    public fun triggerSpatialModeChanged(spatialModeChangeEvent: SpatialModeChangeEvent) {
        runtime.spatialModeChangeListener?.let {
            val recommendedPose = spatialModeChangeEvent.recommendedPose
            val recommendedScale =
                Vector3(
                    spatialModeChangeEvent.recommendedScale,
                    spatialModeChangeEvent.recommendedScale,
                    spatialModeChangeEvent.recommendedScale,
                )
            it.onSpatialModeChanged(recommendedPose, recommendedScale)
        }
    }

    /**
     * Invoking this function simulates a runtime visibility change. This is useful for testing how
     * the application handles transitions between different visibility states (e.g., releasing
     * resources when HIDDEN).
     *
     * Invoking this function will trigger any listeners registered via
     * [Scene.addSpatialVisibilityChangedListener].
     */
    public fun triggerSpatialVisibilityChanged(spatialVisibility: SpatialVisibility) {
        runtime.spatialVisibilityChangedMap.forEach { (consumer, executor) ->
            executor.execute { consumer.accept(spatialVisibility.toRtSpatialVisibility()) }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SceneTester

        return runtime == other.runtime
    }

    override fun hashCode(): Int {
        return runtime.hashCode()
    }
}
