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

package androidx.xr.runtime

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.interfaces.DisplayBlendMode as InternalDisplayBlendMode
import androidx.xr.runtime.interfaces.Feature
import androidx.xr.runtime.interfaces.XrDeviceCapabilityProvider
import androidx.xr.runtime.interfaces.XrDeviceCapabilityProviderFactory
import androidx.xr.runtime.internal.XrInstanceManager
import java.util.WeakHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

/** Device hardware capabilities. */
public class XrDevice
private constructor(
    /**
     * The [Context] associated with this device.
     *
     * Can be passed to [Session.create] or used to access device-specific system services (such as
     * [android.hardware.camera2.CameraManager] or [android.media.AudioManager]).
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val context: Context,
    private val xrDeviceCapabilityProvider: XrDeviceCapabilityProvider?,
    /**
     * The [FormFactor] of this device (such as [FormFactor.HEAD_MOUNTED] or [FormFactor.HANDHELD]).
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val formFactor: FormFactor,
) {

    /**
     * Returns this XrDevice's [Lifecycle].
     *
     * The value will be the Projected device's lifecycle if its [Context] was used when calling
     * [getCurrentDevice].
     *
     * @throws [IllegalStateException] if there is no lifecycle associated with this XrDevice
     */
    public fun getLifecycle(): Lifecycle =
        xrDeviceCapabilityProvider?.lifecycle
            ?: throw IllegalStateException("No lifecycle associated with this XrDevice.")

    public companion object {

        private val CAPABILITY_FACTORY_PROVIDERS =
            listOf(
                "androidx.xr.runtime.openxr.OpenXrDeviceCapabilityProviderFactory",
                "androidx.xr.projected.ProjectedDeviceCapabilityProviderFactory",
                "androidx.xr.runtime.testing.internal.FakeXrDeviceCapabilityProviderFactory",
            )

        @GuardedBy("deviceCache") private val deviceCache = WeakHashMap<Context, XrDevice>()

        /**
         * Get the current [XrDevice] for the provided [Context].
         *
         * @param context the [Context] associated with the device
         * @param coroutineContext the [CoroutineContext] to use for the XrDevice operations
         * @throws [IllegalArgumentException] if the provided [Context] is not supported
         */
        @JvmStatic
        @JvmOverloads
        public fun getCurrentDevice(
            context: Context,
            coroutineContext: CoroutineContext = EmptyCoroutineContext,
        ): XrDevice {
            if (context is LifecycleOwner) {
                check(context.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                    "Cannot get XrDevice for a destroyed context."
                }
            }
            synchronized(deviceCache) {
                deviceCache[context]?.let {
                    return it
                }
            }
            val features = getDeviceContextFeatures(context)
            val xrDeviceCapabilityProviderFactory: XrDeviceCapabilityProviderFactory? =
                selectProvider(
                    loadProviders(
                        XrDeviceCapabilityProviderFactory::class.java,
                        CAPABILITY_FACTORY_PROVIDERS,
                    ),
                    features,
                )
            // TODO(b/525421830): Make xrDeviceCapabilityProvider non-nullable.
            val device =
                XrDevice(
                    context,
                    xrDeviceCapabilityProviderFactory?.create(
                        context,
                        coroutineContext,
                        XrInstanceManager.getProvider(context),
                    ),
                    features.toFormFactor(),
                )
            synchronized(deviceCache) { deviceCache[context] = device }
            if (context is LifecycleOwner) {
                val mainDispatcher = MainExecutorDispatcher(ContextCompat.getMainExecutor(context))
                CoroutineScope(mainDispatcher.immediate + NonCancellable).launch {
                    if (context.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                        synchronized(deviceCache) { deviceCache.remove(context) }
                        return@launch
                    }
                    val observer =
                        object : DefaultLifecycleObserver {
                            override fun onDestroy(owner: LifecycleOwner) {
                                synchronized(deviceCache) { deviceCache.remove(context) }
                                owner.lifecycle.removeObserver(this)
                            }
                        }
                    context.lifecycle.addObserver(observer)
                }
            }
            return device
        }

        /**
         * Get the current [XrDevice] for the provided [Context] and inject extra OpenXR extensions.
         * This method must be called before a [Session] or any other [XrDevice] is created in the
         * current process. This API is only valid if the [XrDevice] is backed by an OpenXR runtime.
         *
         * @param context the [Context] associated with the device
         * @param extensions the list of extra OpenXR extension names to inject
         * @param coroutineContext the [CoroutineContext] to use for the XrDevice operations
         * @throws [IllegalArgumentException] if the provided [Context] is not supported
         * @throws [IllegalStateException] if the OpenXR instance has already been created or the
         *   XrDevice is not backed by an OpenXR instance
         * @throws [UnsupportedOperationException] if any of the requested extensions are not
         *   supported by the device
         */
        @JvmStatic
        @JvmOverloads
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun getCurrentDevice(
            context: Context,
            extensions: List<String>,
            coroutineContext: CoroutineContext = EmptyCoroutineContext,
        ): XrDevice {
            XrInstanceManager.addExtraExtensions(context, extensions)
            return getCurrentDevice(context, coroutineContext)
        }

        /**
         * Returns true if the Android XR Projected service is available on this device. This means
         * the device supports the necessary system features and has the required system service
         * components for Projected experiences. Returns false otherwise.
         */
        @JvmStatic
        public fun isProjectedServiceAvailable(context: Context): Boolean {
            if (!PackageManagerUtils.hasXrProjectedSystemFeature(context)) {
                return false
            }

            return PackageManagerUtils.hasXrProjectedSystemService(context)
        }
    }

    /**
     * Returns the preferred display blend mode for this session.
     *
     * @return the [DisplayBlendMode] that is preferred by the [Session] for rendering
     *   [DisplayBlendMode.NO_DISPLAY] will be returned if there are no supported blend modes
     *   available
     */
    public fun getPreferredDisplayBlendMode(): androidx.xr.runtime.DisplayBlendMode {
        return androidx.xr.runtime.DisplayBlendMode.fromInternalDisplayBlendMode(
            xrDeviceCapabilityProvider?.getPreferredDisplayBlendMode()
                ?: throw IllegalStateException(
                    "XrDeviceCapabilityProvider was not initialized. Did you use XrDevice.getCurrentDevice(context)?"
                )
        )
    }

    /**
     * Returns the [XrDevice]'s support for the given [HandTrackingMode].
     *
     * @return whether the device supports the supplied [HandTrackingMode]
     */
    public fun isHandTrackingModeSupported(mode: HandTrackingMode): Boolean {
        return xrDeviceCapabilityProvider?.isHandTrackingModeSupported(
            mode.toInternalHandTrackingMode()
        )
            ?: throw IllegalStateException(
                "XrDeviceCapabilityProvider was not initialized. Did you use XrDevice.getCurrentDevice(context)?"
            )
    }

    /**
     * Returns the [XrDevice]'s support for the given [EyeTrackingMode].
     *
     * @return whether the device supports the supplied [EyeTrackingMode]
     */
    public fun isEyeTrackingModeSupported(mode: EyeTrackingMode): Boolean {
        return xrDeviceCapabilityProvider?.isEyeTrackingModeSupported(
            mode.toInternalEyeTrackingMode()
        )
            ?: throw IllegalStateException(
                "XrDeviceCapabilityProvider was not initialized. Did you use XrDevice.getCurrentDevice(context)?"
            )
    }

    /**
     * Returns the [XrDevice]'s support for the given [GeospatialMode].
     *
     * @return whether the device supports the supplied [GeospatialMode]
     */
    public fun isGeospatialModeSupported(mode: GeospatialMode): Boolean {
        return xrDeviceCapabilityProvider?.isGeospatialModeSupported(
            mode.toInternalGeospatialMode()
        )
            ?: throw IllegalStateException(
                "XrDeviceCapabilityProvider was not initialized. Did you use XrDevice.getCurrentDevice(context)?"
            )
    }

    /**
     * Returns whether the [XrDevice] supports the supplied [RenderingMode].
     *
     * @return whether the device supports the supplied [RenderingMode]. For devices that support
     *   [RenderingMode.MONO], RenderViewpoint.mono(session) is expected to be non-null. For devices
     *   that support [RenderingMode.STEREO], RenderViewpoint.left(session),
     *   RenderViewpoint.right(session), and RenderViewpoint.mono(session) are all expected to be
     *   non-null
     */
    public fun isRenderingModeSupported(mode: RenderingMode): Boolean {
        return xrDeviceCapabilityProvider?.isRenderingModeSupported(mode.toInternalRenderingMode())
            ?: throw IllegalStateException(
                "XrDeviceCapabilityProvider was not initialized. Did you use XrDevice.getCurrentDevice(context)?"
            )
    }

    /**
     * Returns the [XrDevice]'s support for the given [DepthEstimationMode].
     *
     * @return whether the device supports the supplied [DepthEstimationMode]
     */
    public fun isDepthEstimationModeSupported(mode: DepthEstimationMode): Boolean {
        return xrDeviceCapabilityProvider?.isDepthEstimationModeSupported(
            mode.toInternalDepthEstimationMode()
        )
            ?: throw IllegalStateException(
                "XrDeviceCapabilityProvider was not initialized. Did you use XrDevice.getCurrentDevice(context)?"
            )
    }
}

/**
 * Returns the [FormFactor] of a device with these [Feature]s. Projected devices and devices with
 * the Android XR spatial APIs, such as headsets, are head-mounted. All other devices are handheld.
 *
 * [Feature.OPEN_XR] isn't used because it doesn't identify a head-mounted device: handheld devices
 * can support OpenXR too, and it applies to every context in the process, including the host's.
 */
internal fun Set<Feature>.toFormFactor(): FormFactor =
    // TODO(b/589837935): Get the device type from a reliable source instead of device features.
    if (contains(Feature.PROJECTED) || contains(Feature.SPATIAL)) {
        FormFactor.HEAD_MOUNTED
    } else {
        FormFactor.HANDHELD
    }

private fun DisplayBlendMode.Companion.fromInternalDisplayBlendMode(
    value: InternalDisplayBlendMode
): DisplayBlendMode =
    when (value) {
        InternalDisplayBlendMode.NO_DISPLAY -> DisplayBlendMode.NO_DISPLAY
        InternalDisplayBlendMode.ADDITIVE -> DisplayBlendMode.ADDITIVE
        InternalDisplayBlendMode.ALPHA_BLEND -> DisplayBlendMode.ALPHA_BLEND
        else -> throw IllegalArgumentException("Unknown DisplayBlendMode: $value")
    }
