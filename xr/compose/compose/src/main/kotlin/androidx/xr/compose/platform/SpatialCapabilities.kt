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

package androidx.xr.compose.platform

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.xr.runtime.Session
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.scene

/**
 * Provides the current [SpatialCapabilities] that are currently available to the application.
 *
 * The [SpatialCapabilities] represents a set of inherent permissions that the application may have
 * depending on the context. For example, in home space mode, the app may not have the ability to
 * create spatial UI; however, in full space mode, the application may have this capability.
 */
public val LocalSpatialCapabilities: ProvidableCompositionLocal<SpatialCapabilities> =
    compositionLocalWithComputedDefaultOf {
        LocalComposeXrOwners.currentValue?.spatialCapabilities ?: SpatialCapabilities.NoCapabilities
    }

/**
 * Provides information and functionality related to the spatial capabilities of the application.
 */
public sealed interface SpatialCapabilities {
    /**
     * Indicates whether the application may create spatial UI elements (e.g. SpatialPanel).
     *
     * This is a State-based value that should trigger recomposition in composable functions.
     */
    public val isSpatialUiEnabled: Boolean

    /**
     * Indicates whether the application may create 3D objects.
     *
     * This is a State-based value that should trigger recomposition in composable functions.
     */
    public val isContent3dEnabled: Boolean

    /**
     * Indicates whether the application may set the environment.
     *
     * This is a State-based value that should trigger recomposition in composable functions.
     */
    public val isAppEnvironmentEnabled: Boolean

    /**
     * Indicates whether the application may control the passthrough state.
     *
     * This is a State-based value that should trigger recomposition in composable functions.
     */
    public val isPassthroughControlEnabled: Boolean

    /**
     * Indicates whether the application may use spatial audio.
     *
     * This is a State-based value that should trigger recomposition in composable functions.
     */
    public val isSpatialAudioEnabled: Boolean

    public companion object {
        public val NoCapabilities: SpatialCapabilities =
            StaticSpatialCapabilities(
                isSpatialUiEnabled = false,
                isContent3dEnabled = false,
                isAppEnvironmentEnabled = false,
                isPassthroughControlEnabled = false,
                isSpatialAudioEnabled = false,
            )
    }
}

private class StaticSpatialCapabilities(
    override val isSpatialUiEnabled: Boolean,
    override val isContent3dEnabled: Boolean,
    override val isAppEnvironmentEnabled: Boolean,
    override val isPassthroughControlEnabled: Boolean,
    override val isSpatialAudioEnabled: Boolean,
) : SpatialCapabilities

internal class SessionSpatialCapabilities(session: Session) : SpatialCapabilities {
    private var capabilities by
        mutableStateOf(session.scene.spatialCapabilities).apply {
            session.scene.addSpatialCapabilitiesChangedListener { value = it }
        }

    override val isSpatialUiEnabled: Boolean
        get() = capabilities.contains(SpatialCapability.SPATIAL_UI)

    override val isContent3dEnabled: Boolean
        get() = capabilities.contains(SpatialCapability.SPATIAL_3D_CONTENT)

    override val isAppEnvironmentEnabled: Boolean
        get() = capabilities.contains(SpatialCapability.APP_ENVIRONMENT)

    override val isPassthroughControlEnabled: Boolean
        get() = capabilities.contains(SpatialCapability.PASSTHROUGH_CONTROL)

    override val isSpatialAudioEnabled: Boolean
        get() = capabilities.contains(SpatialCapability.SPATIAL_AUDIO)
}
