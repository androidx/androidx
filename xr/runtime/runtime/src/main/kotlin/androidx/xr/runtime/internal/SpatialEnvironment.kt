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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import java.util.Objects
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Interface for updating the background image/geometry and passthrough settings.
 *
 * <p>The application can set either / both a skybox and a glTF for geometry, then toggle their
 * visibility by enabling or disabling passthrough. The skybox and geometry will be remembered
 * across passthrough mode changes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface SpatialEnvironment {
    /**
     * Gets the current passthrough opacity value between 0 and 1 where 0.0f means no passthrough,
     * and 1.0f means full passthrough.
     *
     * This value can be overwritten by user-enabled or system-enabled passthrough and will not
     * always match the opacity value returned by [preferredPassthroughOpacity].
     */
    public val currentPassthroughOpacity: Float

    /**
     * The preferred spatial environment for the application.
     *
     * If no preference has ever been set by the application, this will be null.
     *
     * Setting this property only sets the preference and does not cause an immediate change unless
     * [isPreferredSpatialEnvironmentActive] is already true. Once the device enters a state where
     * the XR background can be changed and the
     * [SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT] capability is available, the
     * preferred spatial environment for the application will be automatically displayed.
     *
     * Setting the preference to null will disable the preferred spatial environment for the
     * application, meaning the default system environment will be displayed instead.
     *
     * If the given [SpatialEnvironmentPreference] is not null, but all of its properties are null,
     * then the spatial environment will consist of a black skybox and no geometry.
     *
     * See [isPreferredSpatialEnvironmentActive] or the [addOnSpatialEnvironmentChangedListener]
     * listeners to know when this preference becomes active.
     */
    public var preferredSpatialEnvironment: SpatialEnvironmentPreference?

    /**
     * The application's preferred passthrough opacity.
     *
     * Upon construction, the default value is [NO_PASSTHROUGH_OPACITY_PREFERENCE], which means "no
     * application preference". The application's preferred passthrough opacity can be set between
     * 0.0f and 1.0f.
     *
     * Setting the application preference does not guarantee that the value will be immediately
     * applied and visible to the user. The actual passthrough opacity value is controlled by the
     * system in response to a combination of this preference and user actions outside the
     * application. Generally, this preference is honored when the application has the
     * [SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL] capability.
     *
     * The value should be between 0.0f (passthrough disabled) and 1.0f (passthrough fully obscures
     * the spatial environment). Values within 0.01f of 0.0 or 1.0 are snapped to those values.
     * Values outside [0.0f, 1.0f] are clamped. Other values result in semi-transparent passthrough
     * that is alpha blended with the spatial environment. Setting this property to
     * NO_PASSTHROUGH_OPACITY_PREFERENCE clears the application's preference, allowing the system to
     * manage passthrough opacity.
     *
     * The actual value visible to the user can be observed by calling [currentPassthroughOpacity]
     * or by registering a listener with [addOnPassthroughOpacityChangedListener].
     */
    public var preferredPassthroughOpacity: Float

    /**
     * Notifies an application when the passthrough state changes, such as when the application
     * enters or exits passthrough or when the passthrough opacity changes. This [listener] will be
     * called on the provided [executor].
     */
    public fun addOnPassthroughOpacityChangedListener(executor: Executor, listener: Consumer<Float>)

    /** Remove a listener previously added by [addOnPassthroughOpacityChangedListener]. */
    public fun removeOnPassthroughOpacityChangedListener(listener: Consumer<Float>)

    /**
     * Returns true if the environment set by [preferredSpatialEnvironment] is active.
     *
     * Spatial environment preferences set through [preferredSpatialEnvironment] are shown when this
     * is true, but passthrough or other objects in the scene could partially or totally occlude
     * them. When this is false, the default system environment will be active instead.
     */
    public val isPreferredSpatialEnvironmentActive: Boolean

    /**
     * Notifies an application whether or not the preferred spatial environment for the application
     * is active.
     *
     * The environment will try to transition to the application environment when a non-null
     * preference is set through [preferredSpatialEnvironment] and the application has the
     * [SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT] capability. The environment
     * preferences will otherwise not be active.
     *
     * The listener consumes a boolean value that is true if the environment preference is active
     * when the listener is notified.
     *
     * This [listener] will be invoked on the provided [executor].
     */
    public fun addOnSpatialEnvironmentChangedListener(
        executor: Executor,
        listener: Consumer<Boolean>,
    )

    /** Remove a listener previously added by [addOnSpatialEnvironmentChangedListener]. */
    public fun removeOnSpatialEnvironmentChangedListener(listener: Consumer<Boolean>)

    /**
     * A class that represents the user's preferred spatial environment.
     *
     * @param geometry the preferred geometry for the environment based on a pre-loaded glTF model.
     *   If null, there will be no geometry.
     * @param skybox the preferred skybox for the environment based on a pre-loaded EXR Image. If
     *   null, it will be all black.
     * @param geometryMaterial the material to override a given mesh in the geometry. If null, the
     *   material will not override any mesh.
     * @param geometryMeshName the name of the mesh to override with the material. If null, the
     *   material will not override any mesh.
     * @param geometryAnimationName the name of the animation to play on the geometry. If null, the
     *   geometry will not play any animation. Note that the animation will be played in loop.
     */
    public class SpatialEnvironmentPreference
    @JvmOverloads
    constructor(
        public val skybox: ExrImageResource?,
        public val geometry: GltfModelResource?,
        public val geometryMaterial: MaterialResource? = null,
        public val geometryMeshName: String? = null,
        public val geometryAnimationName: String? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SpatialEnvironmentPreference) return false

            return skybox == other.skybox &&
                geometry == other.geometry &&
                geometryMaterial == other.geometryMaterial &&
                geometryMeshName == other.geometryMeshName &&
                geometryAnimationName == other.geometryAnimationName
        }

        override fun hashCode(): Int {
            return Objects.hash(skybox, geometry)
        }
    }

    public companion object {
        /**
         * Passed into [preferredPassthroughOpacity] to clear the application's passthrough opacity
         * preference and to let the system manage passthrough opacity.
         */
        public const val NO_PASSTHROUGH_OPACITY_PREFERENCE: Float = Float.NEGATIVE_INFINITY
    }
}
