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
import androidx.lifecycle.Lifecycle
import androidx.xr.runtime.XrDevice.Companion.getCurrentDevice
import androidx.xr.runtime.interfaces.DisplayBlendMode as InternalDisplayBlendMode
import androidx.xr.runtime.interfaces.XrDeviceCapabilityProvider
import androidx.xr.runtime.interfaces.XrDeviceCapabilityProviderFactory
import androidx.xr.runtime.internal.XrInstanceManager
import java.util.WeakHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/** Provides hardware capabilities of the device. */
public class XrDevice
private constructor(
    private val session: Session?,
    private val xrDeviceCapabilityProvider: XrDeviceCapabilityProvider?,
) {

    /**
     * Returns this XrDevice's [Lifecycle].
     *
     * The value will be the Projected device's lifecycle if its [Context] was used when calling
     * [getCurrentDevice]. Otherwise, the [Session's][Session] lifecycle will be returned.
     *
     * @throws IllegalStateException if there is no lifecycle associated with this XrDevice.
     */
    @ExperimentalXrDeviceLifecycleApi
    public fun getLifecycle(): Lifecycle =
        // TODO(b/461561664) : Use XrDeviceCapabilityProvider.getLifecycle() once session
        // constructor is removed.
        xrDeviceCapabilityProvider?.lifecycle
            ?: session?.lifecycleOwner?.lifecycle
            ?: throw IllegalStateException("No lifecycle associated with this XrDevice.")

    /** A device capability that determines how virtual content is added to the real world. */
    @Deprecated(
        "Use androidx.xr.runtime.DisplayBlendMode instead.",
        replaceWith = ReplaceWith("androidx.xr.runtime.DisplayBlendMode"),
    )
    public class DisplayBlendMode private constructor(private val value: Int) {

        @Suppress("DEPRECATION")
        public companion object {
            /** Blending is not supported. */
            @JvmField public val NO_DISPLAY: DisplayBlendMode = DisplayBlendMode(0)
            /**
             * Virtual content is added to the real world by adding the pixel values for each of
             * Red, Green, and Blue components. Alpha is ignored. Black pixels will appear
             * transparent.
             */
            @JvmField public val ADDITIVE: DisplayBlendMode = DisplayBlendMode(1)
            /**
             * Virtual content is added to the real world by alpha blending the pixel values based
             * on the Alpha component.
             */
            @JvmField public val ALPHA_BLEND: DisplayBlendMode = DisplayBlendMode(2)
        }
    }

    public companion object {

        private val CAPABILITY_FACTORY_PROVIDERS =
            listOf(
                "androidx.xr.runtime.openxr.OpenXrDeviceCapabilityProviderFactory",
                "androidx.xr.projected.ProjectedDeviceCapabilityProviderFactory",
                "androidx.xr.runtime.testing.internal.FakeXrDeviceCapabilityProviderFactory",
            )

        @GuardedBy("deviceCache") private val deviceCache = WeakHashMap<Context, XrDevice>()

        // TODO(b/461561664): Remove this API once session is no longer needed for XrDevice.
        /**
         * Get the current [XrDevice] for the provided [Session].
         *
         * @param session the [Session] connected to the device.
         */
        @JvmStatic
        @Deprecated("Use getCurrentDevice(Context) instead.")
        public fun getCurrentDevice(session: Session): XrDevice =
            XrDevice(session, xrDeviceCapabilityProvider = null)

        /**
         * Get the current [XrDevice] for the provided [Context].
         *
         * @param context the [Context] associated with the device
         * @param coroutineContext the [CoroutineContext] to use for the XrDevice operations
         * @throws IllegalArgumentException if the provided [Context] is not supported
         */
        @JvmStatic
        @JvmOverloads
        @ExperimentalXrDeviceLifecycleApi
        public fun getCurrentDevice(
            context: Context,
            coroutineContext: CoroutineContext = EmptyCoroutineContext,
        ): XrDevice {
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
            val device =
                XrDevice(
                    session = null,
                    xrDeviceCapabilityProviderFactory?.create(
                        context,
                        coroutineContext,
                        XrInstanceManager.getProvider(context),
                    ),
                )
            synchronized(deviceCache) { deviceCache[context] = device }
            return device
        }

        /** Returns true if Projected service is available on this device. False otherwise. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
     * @return The [DisplayBlendMode] that is preferred by the [Session] for rendering.
     *   [DisplayBlendMode.NO_DISPLAY] will be returned if there are no supported blend modes
     *   available.
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
     * @return whether the device supports the supplied [HandTrackingMode].
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
     * @return whether the device supports the supplied [EyeTrackingMode].
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
     * @return whether the device supports the supplied [GeospatialMode].
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
     *   non-null.
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
     * @return whether the device supports the supplied [DepthEstimationMode].
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

private fun DisplayBlendMode.Companion.fromInternalDisplayBlendMode(
    value: InternalDisplayBlendMode
): DisplayBlendMode =
    when (value) {
        InternalDisplayBlendMode.NO_DISPLAY -> DisplayBlendMode.NO_DISPLAY
        InternalDisplayBlendMode.ADDITIVE -> DisplayBlendMode.ADDITIVE
        InternalDisplayBlendMode.ALPHA_BLEND -> DisplayBlendMode.ALPHA_BLEND
        else -> throw IllegalArgumentException("Unknown DisplayBlendMode: $value")
    }
