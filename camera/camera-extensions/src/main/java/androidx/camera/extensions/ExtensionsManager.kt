/*
 * Copyright (C) 2019 The Android Open Source Project
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
package androidx.camera.extensions

import android.content.Context
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Range
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureCapabilities
import androidx.camera.core.Preview
import androidx.camera.core.impl.ExtendedCameraConfigProviderStore
import androidx.camera.core.impl.utils.ContextUtil
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.extensions.ExtensionsManager.Companion.getInstanceAsync
import androidx.camera.extensions.internal.Camera2ExtensionsInfo
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.await
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException

/**
 * Provides interfaces for third party app developers to get capabilities info of extension
 * functions.
 *
 * Many Android devices contain powerful cameras, with manufacturers devoting a lot of effort to
 * build many cutting-edge features, or special effects, into these camera devices. `CameraX
 * Extensions` allows third party apps to enable the available extension modes on the supported
 * devices. The extension modes which might be supported via `CameraX Extensions` are
 * [ExtensionMode.BOKEH], [ExtensionMode.HDR], [ExtensionMode.NIGHT], [ExtensionMode.FACE_RETOUCH]
 * and [ExtensionMode.AUTO]. The known supported devices are listed in the
 * [Supported devices](https://developer.android.com/training/camera/supported-devices) page.
 *
 * `CameraX Extensions` are built on the top of `CameraX Core` libraries. To enable an extension
 * mode, an [ExtensionsManager] instance needs to be retrieved first. For kotlin users, it is
 * recommended to use [ExtensionsManager.getInstance] which is a suspend function. For Java users,
 * please use [getInstanceAsync].
 *
 * After retrieving the [ExtensionsManager] instance, the availability of a specific extension mode
 * can be checked by [isExtensionAvailable]. For an available extension mode, an extension enabled
 * [CameraSelector] can be obtained by calling [getExtensionEnabledCameraSelector]. After binding
 * use cases by the extension enabled [CameraSelector], the extension mode will be applied to the
 * bound [Preview] and [ImageCapture]. The following sample code describes how to enable an
 * extension mode for use cases.
 *
 * @sample androidx.camera.extensions.samples.bindUseCasesWithBokehMode
 *
 * Without enabling `CameraX Extensions`, any device should be able to support the use cases
 * combination of [ImageCapture], [Preview] and [ImageAnalysis]. To support the `CameraX Extensions`
 * functionality, the [ImageCapture] or [Preview] might need to occupy a different format of stream.
 * This might restrict the app to not be able to bind [ImageCapture], [Preview] and [ImageAnalysis]
 * at the same time if the device's hardware level is not
 * [CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL] or above. If enabling an extension mode is
 * more important and the [ImageAnalysis] could be optional to the app design, the extension mode
 * can be enabled successfully when only binding [ImageCapture], [Preview] even if the device's
 * hardware level is [CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED].
 *
 * While `CameraX Extensions` dose not directly support [androidx.camera.video.VideoCapture],
 * [androidx.camera.video.VideoCapture] can still be used when any extension mode is enabled. When
 * the app binds [androidx.camera.video.VideoCapture] and enables any extension mode,
 * [androidx.camera.video.VideoCapture] can obtain the shared stream of [Preview] and record it as a
 * video.
 *
 * For some devices, the vendor library implementation might only support a subset of the all
 * supported sizes retrieved by [StreamConfigurationMap.getOutputSizes]. `CameraX` will select the
 * supported sizes for the use cases according to the use cases' configuration and combination.
 */
public final class ExtensionsManager
@VisibleForTesting
internal constructor(
    internal val extensionsAvailability: ExtensionsAvailability,
    cameraProvider: CameraProvider,
    applicationContext: Context,
) {
    internal enum class ExtensionsAvailability {
        /** The device extensions library exists and has been correctly loaded. */
        LIBRARY_AVAILABLE,
        /**
         * The device extensions library exists. However, there was some error loading the library.
         */
        LIBRARY_UNAVAILABLE_ERROR_LOADING,
        /**
         * The device extensions library exists. However, the library is missing implementations.
         */
        LIBRARY_UNAVAILABLE_MISSING_IMPLEMENTATION,
        /** There are no extensions available on this device. */
        NONE,
    }

    private val extensionsInfo: ExtensionsInfo = ExtensionsInfo(cameraProvider, applicationContext)

    /**
     * Shutdown the extensions.
     *
     * For the moment only used for testing to shutdown the extensions. Calling this function can
     * deinitialize the extensions vendor library and release the created [ExtensionsManager]
     * instance. Tests should wait until the returned future is complete. Then, tests can call the
     * [ExtensionsManager.getInstanceAsync] function again to initialize a new [ExtensionsManager]
     * instance.
     */
    // TODO: Will need to be rewritten to be threadsafe with use in conjunction with
    //  ExtensionsManager.init(...) if this is to be released for use outside of testing.
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun shutdown(): ListenableFuture<Void> {
        synchronized(EXTENSIONS_LOCK) {
            // If initialization not yet attempted then deinit should succeed immediately.
            if (sInitializeFuture == null) {
                return Futures.immediateFuture<Void>(null)
            }

            // If already in progress of deinit then return the future
            if (sDeinitializeFuture != null) {
                return sDeinitializeFuture!!
            }

            val availability: ExtensionsAvailability?

            // Wait for the extension to be initialized before deinitializing. Block since
            // this is only used for testing.
            try {
                sInitializeFuture!!.get()
                sInitializeFuture = null
                availability = sExtensionsManager!!.extensionsAvailability
                sExtensionsManager = null
            } catch (e: ExecutionException) {
                sDeinitializeFuture = Futures.immediateFailedFuture<Void>(e)
                return sDeinitializeFuture!!
            } catch (e: InterruptedException) {
                sDeinitializeFuture = Futures.immediateFailedFuture<Void>(e)
                return sDeinitializeFuture!!
            }

            ExtendedCameraConfigProviderStore.clear()
            sDeinitializeFuture = Futures.immediateFuture<Void>(null)
            return sDeinitializeFuture!!
        }
    }

    /**
     * Returns a modified [CameraSelector] that will enable the specified extension mode.
     *
     * The returned extension [CameraSelector] can be used to bind use cases to a desired
     * [LifecycleOwner] and then the specified extension mode will be enabled on the camera.
     *
     * @param cameraSelector The base [CameraSelector] on top of which the extension config is
     *   applied. [isExtensionAvailable] can be used to check whether any camera can support the
     *   specified extension mode for the base camera selector.
     * @param mode The target extension mode.
     * @return a [CameraSelector] for the specified Extensions mode.
     * @throws IllegalArgumentException If this device doesn't support extensions function, no
     *   camera can be found to support the specified extension mode, or the base [CameraSelector]
     *   has contained extension related configuration in it.
     */
    public fun getExtensionEnabledCameraSelector(
        cameraSelector: CameraSelector,
        @ExtensionMode.Mode mode: Int,
    ): CameraSelector {
        // Directly return the input cameraSelector if the target extension mode is NONE.
        if (mode == ExtensionMode.NONE) {
            return cameraSelector
        }

        require(extensionsAvailability == ExtensionsAvailability.LIBRARY_AVAILABLE) {
            ("This device doesn't support extensions function! " +
                "isExtensionAvailable should be checked first before calling " +
                "getExtensionEnabledCameraSelector.")
        }

        return extensionsInfo.getExtensionCameraSelectorAndInjectCameraConfig(cameraSelector, mode)
    }

    /** Obtains the [CameraFilter] to filter out the cameras for the specified extension mode. */
    internal fun getExtensionCameraFilterAndInjectCameraConfig(
        @ExtensionMode.Mode mode: Int
    ): CameraFilter? {
        // Directly return null if the target extension mode is NONE.
        if (mode == ExtensionMode.NONE) {
            return null
        }

        require(extensionsAvailability == ExtensionsAvailability.LIBRARY_AVAILABLE) {
            "This device doesn't support extensions function! " +
                "isExtensionAvailable should be checked first before calling " +
                "getExtensionEnabledCameraSelector."
        }

        // Injects CameraConfigProvider for the extension mode to the
        // ExtendedCameraConfigProviderStore.
        extensionsInfo.injectExtensionCameraConfig(mode)

        return extensionsInfo.getCameraFilter(mode)
    }

    /**
     * Checks if a specific extension mode is available for a given [CameraSelector].
     *
     * To use Ultra HDR, you must first check for support and then enable the format. This feature
     * is available on capable devices starting from API level 34.
     * 1. Obtain a [CameraInfo] instance by calling [CameraProvider.getCameraInfo] with the
     *    extension-enabled `CameraSelector` from [getExtensionEnabledCameraSelector].
     * 2. Use this `CameraInfo` to get the [ImageCaptureCapabilities] via
     *    [ImageCapture.getImageCaptureCapabilities].
     * 3. Check the supported formats by calling
     *    [ImageCaptureCapabilities.getSupportedOutputFormats]. The presence of
     *    [ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR] indicates Ultra HDR support.
     * 4. When Ultra HDR is supported, configure the [ImageCapture] to use it by calling
     *    [ImageCapture.Builder.setOutputFormat] with [ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR].
     *
     * **Note:** Camera extensions do not support 10-bit preview or video capture. When using
     * extensions, the dynamic range must be [DynamicRange.SDR] (the default).
     *
     * @param cameraSelector The base [CameraSelector] used to select the camera.
     * @param mode The extension mode to verify.
     * @return `true` if the extension mode is available for the camera selector, `false` otherwise.
     */
    public fun isExtensionAvailable(
        cameraSelector: CameraSelector,
        @ExtensionMode.Mode mode: Int,
    ): Boolean {
        if (mode == ExtensionMode.NONE) {
            return true
        }

        if (extensionsAvailability != ExtensionsAvailability.LIBRARY_AVAILABLE) {
            // Returns false if extensions are not available.
            return false
        }

        return extensionsInfo.isExtensionAvailable(cameraSelector, mode)
    }

    /**
     * Returns the estimated capture latency range in milliseconds for the target camera and
     * extension mode.
     *
     * This includes the time spent processing the multi-frame capture request along with any
     * additional time for encoding of the processed buffer in the framework if necessary.
     *
     * @param cameraSelector The [CameraSelector] to find a camera which supports the specified
     *   extension mode.
     * @param mode The extension mode to check.
     * @return the range of estimated minimal and maximal capture latency in milliseconds. Returns
     *   null if no capture latency info can be provided or if the device doesn't support the
     *   extension mode on this camera.
     */
    public fun getEstimatedCaptureLatencyRange(
        cameraSelector: CameraSelector,
        @ExtensionMode.Mode mode: Int,
    ): Range<Long>? {
        if (
            mode == ExtensionMode.NONE ||
                extensionsAvailability != ExtensionsAvailability.LIBRARY_AVAILABLE
        ) {
            // Returns null for non-Extensions mode or if Extensions are not supported on this
            // device.
            return null
        }

        return extensionsInfo.getEstimatedCaptureLatencyRange(cameraSelector, mode, null)
    }

    /**
     * Returns whether the given extension mode supports the [ImageAnalysis] use case on the camera
     * specified by the given [CameraSelector]. If it returns false, invoking
     * `ProcessCameraProvider.bindToLifecycle` with an [ImageAnalysis] use case will throw an
     * [IllegalArgumentException].
     *
     * @param cameraSelector The [CameraSelector] to find a camera which supports the specified
     *   extension mode.
     * @param mode The extension mode to check.
     * @return true if [ImageAnalysis] can be bound when the specified extension mode is enabled on
     *   the camera specified by the given [CameraSelector]. Returns false otherwise. If the device
     *   doesn't support this extension mode on this camera, it will also return false.
     */
    public fun isImageAnalysisSupported(
        cameraSelector: CameraSelector,
        @ExtensionMode.Mode mode: Int,
    ): Boolean {
        if (mode == ExtensionMode.NONE) {
            return true
        }

        // Returns false if Extensions are not supported on this device.
        if (extensionsAvailability != ExtensionsAvailability.LIBRARY_AVAILABLE) {
            return false
        }

        return extensionsInfo.isImageAnalysisSupported(cameraSelector, mode)
    }

    /**
     * Retrieves a [CameraExtensionsControl] object that allows customization of capture request
     * settings for supported camera extensions.
     *
     * @param cameraControl the camera control for a camera with a specific extension mode turned
     *   on.
     * @return a [CameraExtensionsControl] object to manage extension-related settings. Or returns
     *   `null` if the provided [CameraControl] doesn't represent a camera with enabled extensions.
     */
    public fun getCameraExtensionsControl(cameraControl: CameraControl): CameraExtensionsControl? =
        CameraExtensionsControls.from(cameraControl)

    /**
     * Retrieves a [CameraExtensionsInfo] object that allows to observe or monitor capture request
     * settings and results for supported camera extensions.
     *
     * If the provided [CameraInfo] doesn't represent a camera with enabled extensions, a
     * placeholder [CameraExtensionsInfo] object will be returned, indicating no extension type and
     * strength support.
     *
     * @param cameraInfo the camera info for a camera with a specific extension mode turned on.
     * @return a [CameraExtensionsInfo] object for observing extension-specific capture request
     *   settings and results.
     */
    public fun getCameraExtensionsInfo(cameraInfo: CameraInfo): CameraExtensionsInfo =
        CameraExtensionsInfos.from(cameraInfo)

    @VisibleForTesting
    internal fun setVendorExtenderFactory(vendorExtenderFactory: VendorExtenderFactory) {
        extensionsInfo.setVendorExtenderFactory(vendorExtenderFactory)
    }

    public companion object {
        private const val TAG = "ExtensionsManager"
        private const val MINIMUM_SUPPORTED_API_LEVEL = Build.VERSION_CODES.TIRAMISU

        // Singleton instance of the Extensions object
        private val EXTENSIONS_LOCK = Any()

        @GuardedBy("EXTENSIONS_LOCK")
        private var sInitializeFuture: ListenableFuture<ExtensionsManager>? = null

        @GuardedBy("EXTENSIONS_LOCK")
        private var sDeinitializeFuture: ListenableFuture<Void>? = null

        @GuardedBy("EXTENSIONS_LOCK") private var sExtensionsManager: ExtensionsManager? = null

        /**
         * Retrieves the [ExtensionsManager] associated with the current process.
         *
         * An application must wait until the [ListenableFuture] completes to get an
         * [ExtensionsManager] instance. The [ExtensionsManager] instance can be used to access the
         * extensions related functions.
         *
         * @param context The context to initialize the extensions library.
         * @param cameraProvider A [CameraProvider] will be used to query the information of cameras
         *   on the device. The [CameraProvider] can be the
         *   [androidx.camera.lifecycle.ProcessCameraProvider] which is obtained by
         *   [androidx.camera.lifecycle.ProcessCameraProvider.getInstance].
         */
        @JvmStatic
        public fun getInstanceAsync(
            context: Context,
            cameraProvider: CameraProvider,
        ): ListenableFuture<ExtensionsManager> {
            synchronized(EXTENSIONS_LOCK) {
                val deinitInProgress = sDeinitializeFuture?.isDone == false
                check(!deinitInProgress) { "Not yet done deinitializing extensions" }
                sDeinitializeFuture = null
                val applicationContext = ContextUtil.getPersistentApplicationContext(context)

                // CameraX Extensions run on CameraPipe with Camera2 Extensions API. Only devices
                // with API level 33+ can be supported. For devices with API level < 33, we will
                // return an empty implementation which will report all extensions as unavailable
                if (Build.VERSION.SDK_INT < MINIMUM_SUPPORTED_API_LEVEL) {
                    return Futures.immediateFuture<ExtensionsManager>(
                        getOrCreateExtensionsManager(
                            ExtensionsAvailability.NONE,
                            cameraProvider,
                            applicationContext,
                        )
                    )
                }

                if (sInitializeFuture == null) {
                    sInitializeFuture =
                        CallbackToFutureAdapter.getFuture {
                            completer: CallbackToFutureAdapter.Completer<ExtensionsManager> ->
                            val cameraManager =
                                applicationContext.getSystemService(CameraManager::class.java)

                            val camera2ExtensionsInfo = Camera2ExtensionsInfo(cameraManager)

                            val isCamera2ExtensionsSupported =
                                cameraManager.cameraIdList.find { cameraId ->
                                    camera2ExtensionsInfo
                                        .getExtensionCharacteristics(cameraId)
                                        .supportedExtensions
                                        .isNotEmpty()
                                } != null

                            completer.set(
                                getOrCreateExtensionsManager(
                                    if (!isCamera2ExtensionsSupported) {
                                        ExtensionsAvailability.NONE
                                    } else {
                                        ExtensionsAvailability.LIBRARY_AVAILABLE
                                    },
                                    cameraProvider,
                                    applicationContext,
                                )
                            )
                            "Initialize extensions"
                        }
                }
                return sInitializeFuture!!
            }
        }

        private fun getOrCreateExtensionsManager(
            extensionsAvailability: ExtensionsAvailability,
            cameraProvider: CameraProvider,
            applicationContext: Context,
        ): ExtensionsManager =
            synchronized(EXTENSIONS_LOCK) {
                sExtensionsManager
                    ?: ExtensionsManager(extensionsAvailability, cameraProvider, applicationContext)
                        .also { sExtensionsManager = it }
            }

        /**
         * Retrieves the [ExtensionsManager].
         *
         * @param context The application context.
         * @param cameraProvider A [CameraProvider] will be used to query the information of cameras
         *   on the device.
         * @return A fully initialized [ExtensionsManager] for the current process.
         * @see ExtensionsManager.getInstanceAsync
         */
        @JvmStatic
        public suspend fun getInstance(
            context: Context,
            cameraProvider: CameraProvider,
        ): ExtensionsManager = getInstanceAsync(context, cameraProvider).await()
    }
}
