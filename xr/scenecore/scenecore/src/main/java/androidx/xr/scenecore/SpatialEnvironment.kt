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

@file:Suppress("JVM_FIELD")

package androidx.xr.scenecore

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.SpatialEnvironment as RtSpatialEnvironment
import androidx.xr.runtime.internal.SpatialEnvironment.SpatialEnvironmentPreference as RtSpatialEnvironmentPreference
import java.util.concurrent.Executor
import java.util.function.Consumer

// TODO: Support a nullable Runtime in the ctor. This should allow the Runtime to "show up" later.

/**
 * The SpatialEnvironment is used to manage the XR background and passthrough. There is a single
 * instance of this class managed by each [Session] and it is accessible through Session.scene.
 *
 * The SpatialEnvironment is a composite of a stand-alone skybox, and of a
 * [glTF](https://www.khronos.org/Gltf)-specified geometry. A single skybox and a single glTF can be
 * set at the same time. Applications are encouraged to supply glTFs for ground and horizon
 * visibility.
 *
 * The XR background can be set to display one of three configurations:
 * 1) A combination of a skybox and glTF geometry.
 * 2) A Passthrough surface, where the XR background is a live feed from the device's outward facing
 *    cameras. At full opacity, this surface completely occludes the skybox and geometry.
 * 3) A mixed configuration where the passthrough surface is not at full opacity nor is it at zero
 *    opacity. The passthrough surface becomes semi-transparent and alpha blends with the skybox and
 *    geometry behind it.
 *
 * Note that methods in this class do not necessarily take effect immediately. Rather, they set a
 * preference that will be applied when the device enters a state where the XR background can be
 * changed.
 */
public class SpatialEnvironment(private val runtime: JxrPlatformAdapter) {

    private val rtEnvironment: RtSpatialEnvironment = runtime.spatialEnvironment

    /**
     * Represents the preferred spatial environment for the application.
     *
     * @param skybox The preferred skybox for the environment based on a pre-loaded EXR Image. If
     *   null, it will be all black.
     * @param geometry The preferred geometry for the environment based on a pre-loaded [GltfModel].
     *   If null, there will be no geometry.
     */
    public class SpatialEnvironmentPreference(
        public val skybox: ExrImage?,
        public val geometry: GltfModel?,
    ) {

        /**
         * The material to override a given mesh in the geometry. If null, the material will not
         * override any mesh.
         */
        internal var geometryMaterial: Material? = null
            private set

        /**
         * The name of the mesh to override with the material. If null, the material will not
         * override any mesh.
         */
        internal var geometryMeshName: String? = null
            private set

        /**
         * The name of the animation to play on the geometry. If null, the geometry will not play
         * any animation. Note that the animation will be played in loop.
         */
        internal var geometryAnimationName: String? = null
            private set

        /**
         * Represents the preferred spatial environment for the application.
         *
         * @param skybox The preferred skybox for the environment.
         * @param geometry The preferred geometry for the environment.
         * @param geometryMaterial The material to override a given mesh in the geometry.
         * @param geometryMeshName The name of the mesh to override with the material.
         * @param geometryAnimationName The name of the animation to play on the geometry.
         * @throws IllegalStateException if the material is not properly set up and if the geometry
         *   glTF model does not contain the mesh or the animation name.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        @JvmOverloads
        public constructor(
            skybox: ExrImage?,
            geometry: GltfModel?,
            geometryMaterial: Material?,
            geometryMeshName: String? = null,
            geometryAnimationName: String? = null,
        ) : this(skybox, geometry) {
            this.geometryMaterial = geometryMaterial
            this.geometryMeshName = geometryMeshName
            this.geometryAnimationName = geometryAnimationName
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SpatialEnvironmentPreference

            if (skybox != other.skybox) return false
            if (geometry != other.geometry) return false

            return true
        }

        override fun hashCode(): Int {
            var result = skybox?.hashCode() ?: 0
            result = 31 * result + (geometry?.hashCode() ?: 0)
            return result
        }
    }

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
        get() = rtEnvironment.preferredPassthroughOpacity
        set(value) {
            rtEnvironment.preferredPassthroughOpacity = value
        }

    /**
     * Gets the current passthrough opacity value visible to the user.
     *
     * Unlike the application's opacity preference returned by [preferredPassthroughOpacity], this
     * value can be overwritten by the system, and is not directly under the application's control.
     *
     * @return The current passthrough opacity value between 0.0f and 1.0f. A value of 0.0f means no
     *   passthrough is shown, and a value of 1.0f means the passthrough completely obscures the
     *   spatial environment geometry and skybox.
     */
    public val currentPassthroughOpacity: Float
        get() = rtEnvironment.currentPassthroughOpacity

    /**
     * Notifies an application when the user visible passthrough state changes, such as when the
     * application enters or exits passthrough or when the passthrough opacity changes.
     *
     * This [listener] will be called on the Application's main thread.
     *
     * @param listener The [Consumer<Float>] to be added to listen for passthrough opacity changes.
     */
    public fun addOnPassthroughOpacityChangedListener(listener: Consumer<Float>) {
        addOnPassthroughOpacityChangedListener(HandlerExecutor.mainThreadExecutor, listener)
    }

    /**
     * Notifies an application when the user visible passthrough state changes, such as when the
     * application enters or exits passthrough or when the passthrough opacity changes.
     *
     * This listener will be invoked on the given [Executor].
     *
     * @param executor The [Executor] to invoke the listener on.
     * @param listener The [Consumer<Float>] to be added to listen for passthrough opacity changes.
     */
    public fun addOnPassthroughOpacityChangedListener(
        executor: Executor,
        listener: Consumer<Float>,
    ) {
        rtEnvironment.addOnPassthroughOpacityChangedListener(executor, listener)
    }

    /**
     * Remove a listener previously added by [addOnPassthroughOpacityChangedListener].
     *
     * @param listener The previously-added [Consumer<Float>] listener to be removed.
     */
    public fun removeOnPassthroughOpacityChangedListener(listener: Consumer<Float>) {
        rtEnvironment.removeOnPassthroughOpacityChangedListener(listener)
    }

    /**
     * Checks if the application's preferred spatial environment set through
     * [preferredSpatialEnvironment] is active.
     *
     * Spatial environment preference set through [preferredSpatialEnvironment] are shown when this
     * is true, but passthrough or other objects in the scene could partially or totally occlude
     * them. When this is false, the default system environment will be active instead.
     *
     * @return True if the environment set by [preferredSpatialEnvironment] is active.
     */
    public val isPreferredSpatialEnvironmentActive: Boolean
        get() = rtEnvironment.isPreferredSpatialEnvironmentActive

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
        get() = rtEnvironment.preferredSpatialEnvironment?.toSpatialEnvironmentPreference()
        set(value) {
            rtEnvironment.preferredSpatialEnvironment = value?.toRtSpatialEnvironmentPreference()
        }

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
     * This listener will be invoked on the Application's main thread.
     *
     * @param listener The [Consumer<Boolean>] to be added to listen for spatial environment
     *   changes.
     */
    public fun addOnSpatialEnvironmentChangedListener(listener: Consumer<Boolean>) {
        addOnSpatialEnvironmentChangedListener(HandlerExecutor.mainThreadExecutor, listener)
    }

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
     * This listener will be invoked on the given [Executor].
     *
     * @param executor The [Executor] to invoke the listener on.
     * @param listener The [Consumer<Boolean>] to be added to listen for spatial environment
     *   changes.
     */
    public fun addOnSpatialEnvironmentChangedListener(
        executor: Executor,
        listener: Consumer<Boolean>,
    ) {
        rtEnvironment.addOnSpatialEnvironmentChangedListener(executor, listener)
    }

    /**
     * Remove a listener previously added by [addOnSpatialEnvironmentChangedListener].
     *
     * @param listener The previously-added [Consumer<Boolean>] listener to be removed.
     */
    public fun removeOnSpatialEnvironmentChangedListener(listener: Consumer<Boolean>) {
        rtEnvironment.removeOnSpatialEnvironmentChangedListener(listener)
    }

    public companion object {
        /**
         * Passed into [preferredPassthroughOpacity] to clear the application's passthrough opacity
         * preference and to let the system manage passthrough opacity.
         */
        public const val NO_PASSTHROUGH_OPACITY_PREFERENCE: Float =
            RtSpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
    }
}

internal fun SpatialEnvironment.SpatialEnvironmentPreference.toRtSpatialEnvironmentPreference():
    RtSpatialEnvironmentPreference {
    return RtSpatialEnvironmentPreference(
        skybox?.image,
        geometry?.model,
        geometryMaterial?.material,
        geometryMeshName,
        geometryAnimationName,
    )
}

internal fun RtSpatialEnvironmentPreference.toSpatialEnvironmentPreference():
    SpatialEnvironment.SpatialEnvironmentPreference {
    return SpatialEnvironment.SpatialEnvironmentPreference(
        skybox?.let { ExrImage(it) },
        geometry?.let { GltfModel(it) },
        geometryMaterial?.let { Material(it) },
        geometryMeshName,
        geometryAnimationName,
    )
}
