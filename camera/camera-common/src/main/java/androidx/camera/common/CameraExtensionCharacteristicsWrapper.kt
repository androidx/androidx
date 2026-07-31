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

package androidx.camera.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.annotation.RequiresApi

/**
 * Compatibility wrapper around [CameraExtensionCharacteristics].
 *
 * This wrapper provides type-safe, version-compatible access to the characteristics and
 * capabilities of camera extension modes on a device. It extends [CameraCharacteristicsMetadata] to
 * support querying characteristics keys and [UnsafeWrapper] to allow accessing the underlying
 * native characteristics object.
 */
public interface CameraExtensionCharacteristicsWrapper :
    CameraCharacteristicsMetadata, UnsafeWrapper {

    /** The ID of the camera device associated with these extension characteristics. */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getCameraId")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    public val cameraId: CameraId

    /**
     * The camera extension mode represented by this characteristics instance.
     *
     * Value is one of the native camera extension constants defined in
     * [CameraExtensionCharacteristics] (e.g., [CameraExtensionCharacteristics.EXTENSION_BOKEH],
     * [CameraExtensionCharacteristics.EXTENSION_HDR],
     * [CameraExtensionCharacteristics.EXTENSION_NIGHT],
     * [CameraExtensionCharacteristics.EXTENSION_FACE_RETOUCH], or
     * [CameraExtensionCharacteristics.EXTENSION_AUTOMATIC]).
     */
    @get:CameraExtension public val cameraExtension: Int

    /**
     * Whether access to some camera characteristics keys is restricted due to camera permissions.
     *
     * If true, querying restricted keys via [get] or [getOrDefault] may return null or default
     * values, matching the behavior of restricted extension modes when CAMERA permission is not
     * granted.
     */
    public val isRestricted: Boolean

    /**
     * Whether postview is supported for this extension mode.
     *
     * A postview is a quickly available, lower-resolution preview of the captured image that can be
     * displayed while the final high-quality capture is processing.
     *
     * @see android.hardware.camera2.CameraExtensionCharacteristics#isPostviewSupported
     */
    public val isPostviewSupported: Boolean

    /**
     * Whether capture progress callbacks are supported for this extension mode.
     *
     * If true, capture requests can report progress updates during the extension processing.
     *
     * @see CameraExtensionCharacteristics#isCaptureProcessProgressSupported
     */
    public val isCaptureProgressSupported: Boolean

    /**
     * The set of camera characteristics keys supported by this camera extension.
     *
     * @see android.hardware.camera2.CameraExtensionCharacteristics#getKeys
     */
    public val keys: Set<CameraCharacteristics.Key<*>>

    /**
     * The set of capture request keys supported by this camera extension.
     *
     * @see android.hardware.camera2.CameraExtensionCharacteristics#getAvailableCaptureRequestKeys
     */
    public val captureRequestKeys: Set<CaptureRequest.Key<*>>

    /**
     * The set of capture result keys supported by this camera extension.
     *
     * @see android.hardware.camera2.CameraExtensionCharacteristics#getAvailableCaptureResultKeys
     */
    public val captureResultKeys: Set<CaptureResult.Key<*>>

    /** The set of camera characteristics keys that require specific permissions to be accessed. */
    public val restrictedKeys: Set<CameraCharacteristics.Key<*>>

    /** The set of camera characteristics keys that are dynamically updated for this extension. */
    public val dynamicKeys: Set<CameraCharacteristics.Key<*>>

    /**
     * Returns the output sizes supported for the given image format.
     *
     * @param imageFormat the image format (e.g., [android.graphics.ImageFormat.JPEG],
     *   [android.graphics.ImageFormat.YUV_420_888]).
     * @return the set of supported sizes for the image format.
     * @see android.hardware.camera2.CameraExtensionCharacteristics#getExtensionSupportedSizes
     */
    public fun getOutputSizes(@ImageFormat imageFormat: Int): Set<Size>

    /**
     * Returns the output sizes supported for the given class (e.g.,
     * [android.graphics.SurfaceTexture]).
     *
     * @param klass the class of the output target.
     * @return the set of supported sizes for the output class.
     * @see android.hardware.camera2.CameraExtensionCharacteristics#getExtensionSupportedSizes
     */
    public fun getOutputSizes(klass: Class<*>): Set<Size>

    /**
     * Returns the supported postview sizes for a given capture size and format.
     *
     * @param captureSize the size of the final high-quality capture.
     * @param format the image format of the postview.
     * @return the set of supported postview sizes, or an empty set if postview is not supported.
     * @see android.hardware.camera2.CameraExtensionCharacteristics#getPostviewSupportedSizes
     */
    public fun getPostviewSizes(captureSize: Size, @ImageFormat format: Int): Set<Size>

    /**
     * Returns the estimated capture latency range in milliseconds for the given capture size and
     * format.
     *
     * @param captureSize the size of the capture.
     * @param imageFormat the format of the captured image.
     * @return the range of estimated capture latency, or null if the latency is not available.
     * @see CameraExtensionCharacteristics#getEstimatedCaptureLatencyRangeMillis
     */
    public fun getEstimatedCaptureLatencyRangeMillis(
        captureSize: Size,
        @ImageFormat imageFormat: Int,
    ): Range<Long>?
}

/** Helper for constructing and loading [CameraExtensionCharacteristicsWrapper] instances. */
public object CameraExtensionCharacteristicsWrappers {
    /**
     * Wraps an existing [CameraExtensionCharacteristics] instance.
     *
     * @param cameraId the ID of the camera device.
     * @param cameraExtension the camera extension mode.
     * @param extensionCharacteristics the native camera extension characteristics instance.
     * @param isRestricted whether access to some characteristics keys is restricted.
     * @return a [CameraExtensionCharacteristicsWrapper] instance wrapping the native object.
     */
    @JvmStatic
    @JvmName("wrap")
    @JvmOverloads
    @RequiresApi(31)
    public fun wrap(
        cameraId: CameraId,
        @CameraExtension cameraExtension: Int,
        extensionCharacteristics: CameraExtensionCharacteristics,
        isRestricted: Boolean = false,
    ): CameraExtensionCharacteristicsWrapper {
        return AndroidCameraExtensionCharacteristics(
            cameraId,
            cameraExtension,
            extensionCharacteristics,
            isRestricted = isRestricted,
            restrictedKeys = emptySet(),
            dynamicKeys = emptySet(),
        )
    }

    /**
     * Loads and wraps [CameraExtensionCharacteristicsWrapper] for the given camera ID and extension
     * mode using the [Context].
     *
     * Returns `null` if the API level is less than 31 or if the extension is not supported by the
     * camera device.
     *
     * @param context the application context.
     * @param cameraId the ID of the camera device.
     * @param cameraExtension the camera extension mode.
     * @return a [CameraExtensionCharacteristicsWrapper] instance, or null if unsupported.
     */
    @JvmStatic
    @JvmName("loadFrom")
    public fun loadFrom(
        context: Context,
        cameraId: CameraId,
        @CameraExtension cameraExtension: Int,
    ): CameraExtensionCharacteristicsWrapper? {
        if (Build.VERSION.SDK_INT >= 31) {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val extensionCharacteristics =
                cameraManager.getCameraExtensionCharacteristics(cameraId.value)
            if (!extensionCharacteristics.supportedExtensions.contains(cameraExtension)) {
                return null
            }
            val isRestricted =
                context.checkSelfPermission(Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_DENIED
            return wrap(
                cameraId,
                cameraExtension,
                extensionCharacteristics,
                isRestricted = isRestricted,
            )
        }
        return null
    }

    /**
     * Loads and wraps [CameraExtensionCharacteristicsWrapper] for the given camera ID and extension
     * mode using the [CameraManager].
     *
     * Returns `null` if the API level is less than 31 or if the extension is not supported by the
     * camera device.
     *
     * @param cameraManager the system camera manager.
     * @param cameraId the ID of the camera device.
     * @param cameraExtension the camera extension mode.
     * @return a [CameraExtensionCharacteristicsWrapper] instance, or null if unsupported.
     */
    @JvmStatic
    @JvmName("loadFrom")
    public fun loadFrom(
        cameraManager: CameraManager,
        cameraId: CameraId,
        @CameraExtension cameraExtension: Int,
    ): CameraExtensionCharacteristicsWrapper? {
        if (Build.VERSION.SDK_INT >= 31) {
            val extensionCharacteristics =
                cameraManager.getCameraExtensionCharacteristics(cameraId.value)
            if (!extensionCharacteristics.supportedExtensions.contains(cameraExtension)) {
                return null
            }
            return wrap(cameraId, cameraExtension, extensionCharacteristics)
        }
        return null
    }

    /**
     * Loads and wraps [CameraExtensionCharacteristicsWrapper] instances for all supported extension
     * modes on the given camera ID using the [Context].
     *
     * Returns an empty map if the API level is less than 31.
     *
     * @param context the application context.
     * @param cameraId the ID of the camera device.
     * @return a map of [CameraExtension] mode to [CameraExtensionCharacteristicsWrapper].
     */
    @JvmStatic
    @JvmName("loadAvailableExtensionsFrom")
    public fun loadAvailableExtensionsFrom(
        context: Context,
        cameraId: CameraId,
    ): Map<@CameraExtension Int, CameraExtensionCharacteristicsWrapper> {
        if (Build.VERSION.SDK_INT >= 31) {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val isRestricted =
                context.checkSelfPermission(Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_DENIED
            val extensionCharacteristics =
                cameraManager.getCameraExtensionCharacteristics(cameraId.value)
            val supported = extensionCharacteristics.supportedExtensions
            val map = mutableMapOf<Int, CameraExtensionCharacteristicsWrapper>()
            for (extension in supported) {
                @Suppress("WrongConstant")
                map[extension] =
                    wrap(cameraId, extension, extensionCharacteristics, isRestricted = isRestricted)
            }
            return map
        }
        return emptyMap()
    }

    /**
     * Loads and wraps [CameraExtensionCharacteristicsWrapper] instances for all supported extension
     * modes on the given camera ID using the [CameraManager].
     *
     * Returns an empty map if the API level is less than 31.
     *
     * @param cameraManager the system camera manager.
     * @param cameraId the ID of the camera device.
     * @return a map of [CameraExtension] mode to [CameraExtensionCharacteristicsWrapper].
     */
    @JvmStatic
    @JvmName("loadAvailableExtensionsFrom")
    public fun loadAvailableExtensionsFrom(
        cameraManager: CameraManager,
        cameraId: CameraId,
    ): Map<@CameraExtension Int, CameraExtensionCharacteristicsWrapper> {
        if (Build.VERSION.SDK_INT >= 31) {
            val extensionCharacteristics =
                cameraManager.getCameraExtensionCharacteristics(cameraId.value)
            val supported = extensionCharacteristics.supportedExtensions
            val map = mutableMapOf<Int, CameraExtensionCharacteristicsWrapper>()
            for (extension in supported) {
                @Suppress("WrongConstant")
                map[extension] = wrap(cameraId, extension, extensionCharacteristics)
            }
            return map
        }
        return emptyMap()
    }
}
