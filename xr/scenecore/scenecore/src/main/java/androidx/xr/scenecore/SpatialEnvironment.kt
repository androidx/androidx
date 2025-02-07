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

@file:Suppress("JVM_FIELD", "Deprecation")

package androidx.xr.scenecore

import android.util.Log
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialEnvironment.SetPassthroughOpacityPreferenceResult as RtSetPassthroughOpacityPreferenceResult
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialEnvironment.SetSpatialEnvironmentPreferenceResult as RtSetSpatialEnvironmentPreferenceResult
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialEnvironment.SpatialEnvironmentPreference as RtSpatialEnvironmentPreference
import com.google.errorprone.annotations.CanIgnoreReturnValue
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

// TODO: Support a nullable Runtime in the ctor. This should allow the Runtime to "show up" later.

/**
 * The SpatialEnvironment is used to manage the XR background and passthrough. There is a single
 * instance of this class managed by each SceneCore Session (which is bound to an [Activity].)
 *
 * The SpatialEnvironment is a composite of a stand-alone skybox, and glTF-specified geometry. A
 * single skybox and a single glTF can be set at the same time. Applications are encouraged to
 * supply glTFs for ground and horizon visibility.
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

    private val TAG = "SpatialEnvironment"

    private val rtEnvironment: JxrPlatformAdapter.SpatialEnvironment = runtime.spatialEnvironment

    // These two fields are only used by the deprecated setSkybox() and setGeometry() methods.
    // TODO: b/370015943 - Remove after clients migrate to the SpatialEnvironmentPreference APIs.
    private val deprecatedSkybox = AtomicReference<ExrImage?>()
    private val deprecatedGeometry = AtomicReference<GltfModel?>()

    // TODO: b/370484799 - Remove this once all clients move away from it.
    /** Describes if/how the User can view their real-world physical environment. */
    @Deprecated(message = "Use isSpatialEnvironmentPreferenceActive() instead.")
    public class PassthroughMode internal constructor(public val value: Int) {
        public companion object {
            /** The state at startup. The application cannot set this state. No longer used. */
            @JvmField public val Uninitialized: PassthroughMode = PassthroughMode(0)
            /**
             * The user's passthrough is not composed into their view. Environment skyboxes and
             * geometry are only visible in this state.
             */
            @JvmField public val Disabled: PassthroughMode = PassthroughMode(1)
            /** The user's passthrough is visible at full or partial opacity. */
            @JvmField public val Enabled: PassthroughMode = PassthroughMode(2)
        }
    }

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

    // TODO: b/370484799 - Remove this once all clients migrate to the opacity Preference APIs.
    /**
     * Sets the preference for passthrough.
     *
     * Calling with DISABLED is equivalent to calling setPassthroughOpacityPreference(0.0f) and
     * calling with ENABLED is equivalent to calling setPassthroughOpacityPreference(1.0f). Calling
     * with UNINITIALIZED is ignored. See [setPassthroughOpacityPreference] for more details.
     */
    @Deprecated(message = "Use setPassthroughOpacityPreference instead.")
    public fun setPassthrough(passthroughMode: PassthroughMode) {
        when (passthroughMode) {
            PassthroughMode.Uninitialized -> return // Do nothing. This isn't allowed.
            PassthroughMode.Disabled -> setPassthroughOpacityPreference(0.0f)
            PassthroughMode.Enabled -> setPassthroughOpacityPreference(1.0f)
        }
    }

    // TODO: b/370484799 - Remove this once all clients migrate to the opacity Preference APIs.
    /**
     * Sets the preference for passthrough. This is equivalent to calling
     * [setPassthroughOpacityPreference] with the given opacity value.
     */
    @Deprecated(message = "Use setPassthroughOpacityPreference instead.")
    public fun setPassthroughOpacity(passthroughOpacity: Float) {
        setPassthroughOpacityPreference(passthroughOpacity)
    }

    // TODO: b/370484799 - Remove this once all clients migrate to the opacity Preference APIs.
    /** Gets the current preference for passthrough mode. */
    @Deprecated(message = "Use getCurrentPassthroughOpacity instead.")
    public fun getPassthroughMode(): PassthroughMode {
        if (getCurrentPassthroughOpacity() > 0.0f) {
            return PassthroughMode.Enabled
        } else {
            return PassthroughMode.Disabled
        }
    }

    // TODO: b/370484799 - Remove this once all clients migrate to the opacity Preference APIs.
    /**
     * Gets the current passthrough opacity. This may be different than the passthrough opacity
     * preference.
     */
    @Deprecated(message = "Use getCurrentPassthroughOpacity instead.")
    public fun getPassthroughOpacity(): Float {
        return getCurrentPassthroughOpacity()
    }

    /**
     * Sets the application's preferred passthrough opacity between 0.0f and 1.0f. Upon
     * construction, the default value is null, which means "no application preference".
     *
     * Setting the application preference through this method does not guarantee that the value will
     * be immediately applied and visible to the user. The actual passthrough opacity value is
     * controlled by the system in response to a combination of the application's preference and
     * user actions outside the application. Generally, the application's preference will be shown
     * to the user when the application has the
     * [SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL] capability. The current value
     * visible to the user can be observed by calling [getCurrentPassthroughOpacity] or by
     * registering a listener with [addOnPassthroughOpacityChangedListener].
     *
     * @param passthroughOpacityPreference The application's passthrough opacity preference between
     *   0.0f (disabled with no passthrough) and 1.0f (fully enabled passthrough hides the spatial
     *   environment). Values within 0.01f of 0.0 or 1.0 will be snapped to those values. Other
     *   values result in semi-transparent passthrough alpha blended with the spatial environment.
     *   Values outside [0.0f, 1.0f] are clamped. If null, the system will manage the passthrough
     *   opacity.
     * @return The result of the call to set the passthrough opacity preference. If the preference
     *   was successfully set and applied, the result will be
     *   [SetPassthroughOpacityPreferenceChangeApplied]. If the preference was set, but it cannot be
     *   currently applied, the result will be [SetPassthroughOpacityPreferenceChangePending].
     */
    @CanIgnoreReturnValue
    public fun setPassthroughOpacityPreference(
        @SuppressWarnings("AutoBoxing") passthroughOpacityPreference: Float?
    ): SetPassthroughOpacityPreferenceResult {
        return rtEnvironment
            .setPassthroughOpacityPreference(passthroughOpacityPreference)
            .toSetPassthroughOpacityPreferenceResult()
    }

    /**
     * Gets the current passthrough opacity value visible to the user.
     *
     * Unlike the application's opacity preference returned by [getPassthroughOpacityPreference],
     * this value can be overwritten by the system, and is not directly under the application's
     * control.
     *
     * @return The current passthrough opacity value between 0.0f and 1.0f. A value of 0.0f means no
     *   passthrough is shown, and a value of 1.0f means the passthrough completely obscures the
     *   spatial environment geometry and skybox.
     */
    public fun getCurrentPassthroughOpacity(): Float {
        return rtEnvironment.currentPassthroughOpacity
    }

    /**
     * Gets the current passthrough opacity preference set through
     * [setPassthroughOpacityPreference]. Defaults to null if [setPassthroughOpacityPreference] has
     * not been called.
     *
     * This value only reflects the application's preference and does not necessarily reflect what
     * the system is currently showing the user. See [getCurrentPassthroughOpacity] to get the
     * actual visible opacity value.
     *
     * @return The last passthrough opacity value between 0.0f and 1.0f requested through
     *   [setPassthroughOpacityPreference]. If null, no application preference is set and the
     *   passthrough opacity will be fully managed through the system.
     */
    @SuppressWarnings("AutoBoxing")
    public fun getPassthroughOpacityPreference(): Float? {
        return rtEnvironment.passthroughOpacityPreference
    }

    /**
     * Notifies an application when the user visible passthrough state changes, such as when the
     * application enters or exits passthrough or when the passthrough opacity changes.
     *
     * This [listener] will be called on the Application's main thread.
     *
     * @param listener The [Consumer<Float>] to be added to listen for passthrough opacity changes.
     */
    public fun addOnPassthroughOpacityChangedListener(listener: Consumer<Float>) {
        rtEnvironment.addOnPassthroughOpacityChangedListener(listener)
    }

    /**
     * Remove a listener previously added by [addOnPassthroughOpacityChangedListener].
     *
     * @param listener The previously-added [Consumer<Float>] listener to be removed.
     */
    public fun removeOnPassthroughOpacityChangedListener(listener: Consumer<Float>) {
        rtEnvironment.removeOnPassthroughOpacityChangedListener(listener)
    }

    // TODO: b/370015943 - Remove this once all clients migrate to the SpatialEnvironment APIs.
    /**
     * Sets the preferred environmental skybox based on a pre-loaded EXR Image.
     *
     * Note that this method does not necessarily cause an immediate change, it only sets a
     * preference. Once the device enters a state where the XR background can be changed, the
     * preference will be applied.
     *
     * Setting the skybox to null will disable the skybox.
     */
    @Deprecated(message = "Use setSpatialEnvironmentPreference() instead.")
    public fun setSkybox(exrImage: ExrImage?) {
        Log.w(TAG, "setGeometry() is deprecated. Use setSpatialEnvironmentPreference() instead.")
        deprecatedSkybox.updateAndGet {
            setSpatialEnvironmentPreference(
                SpatialEnvironmentPreference(exrImage, deprecatedGeometry.get())
            )
            exrImage
        }
    }

    // TODO: b/370015943 - Remove this once all clients migrate to the SpatialEnvironment APIs.
    /**
     * Sets the preferred environmental geometry based on a pre-loaded [GltfModel].
     *
     * Note that this method does not necessarily cause an immediate change, it only sets a
     * preference. Once the device enters a state where the XR background can be changed, the
     * preference will be applied.
     *
     * Setting the geometry to null will disable the geometry.
     */
    @Deprecated(message = "Use setSpatialEnvironmentPreference() instead.")
    public fun setGeometry(gltfModel: GltfModel?) {
        Log.w(TAG, "setGeometry() is deprecated. Use setSpatialEnvironmentPreference() instead.")
        deprecatedGeometry.updateAndGet {
            setSpatialEnvironmentPreference(
                SpatialEnvironmentPreference(deprecatedSkybox.get(), gltfModel)
            )
            gltfModel
        }
    }

    /**
     * Returns true if the environment set by [setSpatialEnvironmentPreference] is active.
     *
     * Spatial environment preference set through [setSpatialEnvironmentPreference] are shown when
     * this is true, but passthrough or other objects in the scene could partially or totally
     * occlude them. When this is false, the default system environment will be active instead.
     *
     * @return True if the environment set by [setSpatialEnvironmentPreference] is active.
     */
    public fun isSpatialEnvironmentPreferenceActive(): Boolean {
        return rtEnvironment.isSpatialEnvironmentPreferenceActive
    }

    /**
     * Gets the preferred spatial environment for the application.
     *
     * The returned value is always what was most recently supplied to
     * [setSpatialEnvironmentPreference], or null if no preference has been set.
     *
     * See [isSpatialEnvironmentPreferenceActive] or the [addOnSpatialEnvironmentChangedListener]
     * listeners to know when this preference becomes active.
     *
     * @return The most recent spatial environment preference supplied to
     *   [setSpatialEnvironmentPreference]. If null, the default system environment will be
     *   displayed instead.
     */
    public fun getSpatialEnvironmentPreference(): SpatialEnvironmentPreference? {
        return rtEnvironment.spatialEnvironmentPreference?.toSpatialEnvironmentPreference()
    }

    /**
     * Sets the preferred spatial environment for the application.
     *
     * Note that this method only sets a preference and does not cause an immediate change unless
     * [isSpatialEnvironmentPreferenceActive] is already true. Once the device enters a state where
     * the XR background can be changed and the
     * [SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENTS] capability is available, the
     * preferred spatial environment for the application will be automatically displayed.
     *
     * Setting the preference to null will disable the preferred spatial environment for the
     * application, meaning the default system environment will be displayed instead.
     *
     * If the given [SpatialEnvironmentPreference] is not null, but all of its properties are null,
     * then the spatial environment will consist of a black skybox and no geometry
     * [isSpatialEnvironmentPreferenceActive] is true.
     *
     * Changes to the Environment state will be notified via listeners added with
     * [addOnSpatialEnvironmentChangedListener].
     *
     * @param environmentPreference The preferred spatial environment for the application. If null,
     *   then there is no preference, and the default system environment will be displayed instead.
     * @return The result of the call to set the spatial environment preference. If the preference
     *   was successfully set and applied, the result will be
     *   [SetSpatialEnvironmentPreferenceChangeApplied]. If the preference was set, but it cannot be
     *   currently applied, the result will be [SetSpatialEnvironmentPreferenceChangePending].
     */
    @CanIgnoreReturnValue
    public fun setSpatialEnvironmentPreference(
        environmentPreference: SpatialEnvironmentPreference?
    ): SetSpatialEnvironmentPreferenceResult {
        return rtEnvironment
            .setSpatialEnvironmentPreference(
                environmentPreference?.toRtSpatialEnvironmentPreference()
            )
            .toSetSpatialEnvironmentPreferenceResult()
    }

    // TODO: b/370957362 - Add overloads for the add...Listener methods to take in an executor
    /**
     * Notifies an application whether or not the preferred spatial environment for the application
     * is active.
     *
     * The environment will try to transition to the application environment when a non-null
     * preference is set through [setSpatialEnvironmentPreference] and the application has the
     * [SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENTS] capability. The environment
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
        rtEnvironment.addOnSpatialEnvironmentChangedListener(listener)
    }

    /**
     * Remove a listener previously added by [addOnSpatialEnvironmentChangedListener].
     *
     * @param listener The previously-added [Consumer<Boolean>] listener to be removed.
     */
    public fun removeOnSpatialEnvironmentChangedListener(listener: Consumer<Boolean>) {
        rtEnvironment.removeOnSpatialEnvironmentChangedListener(listener)
    }

    /**
     * If the primary Activity in a [Session] spatial environment has focus, causes the [Session] to
     * be placed in FullSpace Mode. Otherwise, this call does nothing.
     */
    public fun requestFullSpaceMode(): Unit = runtime.requestFullSpaceMode()

    /**
     * If the primary Activity in a [Session] spatial environment has focus, causes the [Session] to
     * be placed in HomeSpace Mode. Otherwise, this call does nothing.
     */
    public fun requestHomeSpaceMode(): Unit = runtime.requestHomeSpaceMode()

    /** Result values for calls to [setPassthroughOpacityPreference] */
    public sealed class SetPassthroughOpacityPreferenceResult()

    /** The call to [setPassthroughOpacityPreference] succeeded and should now be visible. */
    public class SetPassthroughOpacityPreferenceChangeApplied :
        SetPassthroughOpacityPreferenceResult()

    /**
     * The call to [setPassthroughOpacityPreference] successfully applied the preference, but it is
     * not immediately visible due to requesting a state change while the activity does not have the
     * [SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL] capability to control the app
     * passthrough state. The preference was still set and will be applied when the capability is
     * gained.
     */
    public class SetPassthroughOpacityPreferenceChangePending :
        SetPassthroughOpacityPreferenceResult()

    /** Result values for calls to SpatialEnvironment.setSpatialEnvironmentPreference */
    public sealed class SetSpatialEnvironmentPreferenceResult()

    /** The call to [setSpatialEnvironmentPreference] succeeded and should now be visible. */
    public class SetSpatialEnvironmentPreferenceChangeApplied :
        SetSpatialEnvironmentPreferenceResult()

    /**
     * The call to [setSpatialEnvironmentPreference] successfully applied the preference, but it is
     * not immediately visible due to requesting a state change while the activity does not have the
     * [SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENTS] capability to control the app
     * environment state. The preference was still set and will be applied when the capability is
     * gained.
     */
    public class SetSpatialEnvironmentPreferenceChangePending :
        SetSpatialEnvironmentPreferenceResult()
}

internal fun SpatialEnvironment.SpatialEnvironmentPreference.toRtSpatialEnvironmentPreference():
    RtSpatialEnvironmentPreference {
    return RtSpatialEnvironmentPreference(skybox?.image, geometry?.model)
}

internal fun RtSpatialEnvironmentPreference.toSpatialEnvironmentPreference():
    SpatialEnvironment.SpatialEnvironmentPreference {
    return SpatialEnvironment.SpatialEnvironmentPreference(
        skybox?.let { ExrImage(it) },
        geometry?.let { GltfModel(it) },
    )
}

internal fun RtSetSpatialEnvironmentPreferenceResult.toSetSpatialEnvironmentPreferenceResult():
    SpatialEnvironment.SetSpatialEnvironmentPreferenceResult {
    return when (this) {
        RtSetSpatialEnvironmentPreferenceResult.CHANGE_APPLIED ->
            SpatialEnvironment.SetSpatialEnvironmentPreferenceChangeApplied()
        RtSetSpatialEnvironmentPreferenceResult.CHANGE_PENDING ->
            SpatialEnvironment.SetSpatialEnvironmentPreferenceChangePending()
    }
}

internal fun RtSetPassthroughOpacityPreferenceResult.toSetPassthroughOpacityPreferenceResult():
    SpatialEnvironment.SetPassthroughOpacityPreferenceResult {
    return when (this) {
        RtSetPassthroughOpacityPreferenceResult.CHANGE_APPLIED ->
            SpatialEnvironment.SetPassthroughOpacityPreferenceChangeApplied()
        RtSetPassthroughOpacityPreferenceResult.CHANGE_PENDING ->
            SpatialEnvironment.SetPassthroughOpacityPreferenceChangePending()
    }
}
