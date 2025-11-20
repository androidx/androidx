/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.camera.core.impl

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import androidx.camera.core.internal.utils.SizeUtil

/**
 * Surface configuration type and size pair
 *
 * [CameraDevice.createCaptureSession] defines the default guaranteed stream combinations for
 * different hardware level devices. It defines what combination of surface configuration type and
 * size pairs can be supported for different hardware level camera devices.
 */
public data class SurfaceConfig(
    public val configType: ConfigType,
    public val configSize: ConfigSize,
    public val streamUseCase: StreamUseCase = DEFAULT_STREAM_USE_CASE,
) {

    public companion object {
        @JvmField public val DEFAULT_STREAM_USE_CASE: StreamUseCase = StreamUseCase.DEFAULT

        private val FEATURE_COMBO_QUERY_SUPPORTED_SIZES: Array<ConfigSize> =
            arrayOf(
                ConfigSize.S720P_16_9,
                ConfigSize.S1080P_4_3,
                ConfigSize.S1080P_16_9,
                ConfigSize.S1440P_16_9,
                ConfigSize.UHD,
                ConfigSize.X_VGA,
            )

        private val IMAGE_FORMATS_BY_CONFIG_TYPE: Map<ConfigType, Int> =
            mapOf(
                ConfigType.YUV to ImageFormat.YUV_420_888,
                ConfigType.JPEG to ImageFormat.JPEG,
                ConfigType.JPEG_R to ImageFormat.JPEG_R,
                ConfigType.RAW to ImageFormat.RAW_SENSOR,
                ConfigType.PRIV to ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
            )

        private val CONFIG_TYPES_BY_IMAGE_FORMAT: Map<Int, ConfigType> =
            IMAGE_FORMATS_BY_CONFIG_TYPE.entries.associateBy({ it.value }, { it.key })

        /** Creates a new instance of SurfaceConfig with the given parameters. */
        @JvmStatic
        @JvmOverloads
        public fun create(
            type: ConfigType,
            size: ConfigSize,
            streamUseCase: StreamUseCase = DEFAULT_STREAM_USE_CASE,
        ): SurfaceConfig {
            return SurfaceConfig(type, size, streamUseCase)
        }

        /**
         * Gets [ConfigType] from image format.
         *
         * PRIV refers to any target whose available sizes are found using
         * StreamConfigurationMap.getOutputSizes(Class) with no direct application-visible format,
         * YUV refers to a target Surface using the ImageFormat.YUV_420_888 format, JPEG refers to
         * the ImageFormat.JPEG or ImageFormat.JPEG_R format, and RAW refers to the
         * ImageFormat.RAW_SENSOR format.
         */
        @JvmStatic
        public fun getConfigType(imageFormat: Int): ConfigType {
            return CONFIG_TYPES_BY_IMAGE_FORMAT[imageFormat] ?: ConfigType.PRIV
        }

        /**
         * Transform to a SurfaceConfig object with image format and size info
         *
         * @param cameraMode the working camera mode.
         * @param imageFormat the image format info for the surface configuration object
         * @param size the size info for the surface configuration object
         * @param surfaceSizeDefinition the surface definition for the surface configuration object
         * @return new [SurfaceConfig] object
         */
        @JvmStatic
        @JvmOverloads
        public fun transformSurfaceConfig(
            imageFormat: Int,
            size: Size,
            surfaceSizeDefinition: SurfaceSizeDefinition,
            @CameraMode.Mode cameraMode: Int = CameraMode.DEFAULT,
            configSource: ConfigSource = ConfigSource.CAPTURE_SESSION_TABLES,
            streamUseCase: StreamUseCase = DEFAULT_STREAM_USE_CASE,
        ): SurfaceConfig {
            val configType = getConfigType(imageFormat)
            var configSize = ConfigSize.NOT_SUPPORT

            // Compare with surface size definition to determine the surface configuration size
            val sizeArea = SizeUtil.getArea(size)

            if (cameraMode == CameraMode.CONCURRENT_CAMERA) {
                if (sizeArea <= SizeUtil.getArea(surfaceSizeDefinition.getS720pSize(imageFormat))) {
                    configSize = ConfigSize.S720P_16_9
                } else if (
                    sizeArea <= SizeUtil.getArea(surfaceSizeDefinition.getS1440pSize(imageFormat))
                ) {
                    configSize = ConfigSize.S1440P_4_3
                }
            } else if (configSource == ConfigSource.FEATURE_COMBINATION_TABLE) {
                val maximumSize = surfaceSizeDefinition.getMaximumSize(imageFormat)

                // Try all fixed sizes first for exact match
                for (supportedSize in FEATURE_COMBO_QUERY_SUPPORTED_SIZES) {
                    if (size == supportedSize.relatedFixedSize) {
                        configSize = supportedSize
                        break
                    }
                }

                // There was no fixed size match, so try the max supported size next
                if (configSize == ConfigSize.NOT_SUPPORT) {
                    if (size == maximumSize) {
                        configSize = ConfigSize.MAXIMUM
                    }
                }
            } else {
                if (sizeArea <= SizeUtil.getArea(surfaceSizeDefinition.getAnalysisSize())) {
                    configSize = ConfigSize.VGA
                } else if (sizeArea <= SizeUtil.getArea(surfaceSizeDefinition.getPreviewSize())) {
                    configSize = ConfigSize.PREVIEW
                } else if (sizeArea <= SizeUtil.getArea(surfaceSizeDefinition.getRecordSize())) {
                    configSize = ConfigSize.RECORD
                } else {
                    val maximumSize = surfaceSizeDefinition.getMaximumSize(imageFormat)
                    val ultraMaximumSize = surfaceSizeDefinition.getUltraMaximumSize(imageFormat)
                    // On some devices, when extensions is on, some extra formats might be supported
                    // for extensions. But those formats are not supported in the normal mode. In
                    // that case, MaximumSize could be null. Directly make configSize as MAXIMUM for
                    // the case.
                    if (
                        (maximumSize == null || sizeArea <= SizeUtil.getArea(maximumSize)) &&
                            cameraMode != CameraMode.ULTRA_HIGH_RESOLUTION_CAMERA
                    ) {
                        configSize = ConfigSize.MAXIMUM
                    } else if (
                        ultraMaximumSize != null && sizeArea <= SizeUtil.getArea(ultraMaximumSize)
                    ) {
                        configSize = ConfigSize.ULTRA_MAXIMUM
                    }
                }
            }

            return create(type = configType, size = configSize, streamUseCase = streamUseCase)
        }
    }

    /**
     * Check whether the input surface configuration can be supported by this object.
     *
     * A surface configuration is considered "supported" if its properties (size, type, and stream
     * use case) are compatible with this `SurfaceConfig`. Specifically, for `other` to be
     * supported:
     * * The `other` surface's config size must be smaller than this SurfaceConfig's configSize.
     * * The `other` surface's configType must match this SurfaceConfig.
     * * If both SurfaceConfig have a [StreamUseCase] other than [StreamUseCase.DEFAULT], then the
     *   [StreamUseCase] must match.
     *
     * @param other the surface configuration to be compared
     * @return the check result that whether it could be supported
     */
    public fun isSupported(other: SurfaceConfig): Boolean {
        if (other.configSize.id > configSize.id) {
            return false
        } else if (other.configType != configType) {
            return false
        } else if (
            streamUseCase != StreamUseCase.DEFAULT &&
                other.streamUseCase != StreamUseCase.DEFAULT &&
                other.streamUseCase != streamUseCase
        ) {
            return false
        }
        return true
    }

    /** Returns the [ImageFormat] constant of the underlying [ConfigType]. */
    public val imageFormat: Int = IMAGE_FORMATS_BY_CONFIG_TYPE[configType] ?: ImageFormat.UNKNOWN

    /** Returns the resolution based on the underlying [ConfigSize]. */
    public fun getResolution(definition: SurfaceSizeDefinition): Size {
        return when (configSize) {
            ConfigSize.PREVIEW -> definition.getPreviewSize()
            ConfigSize.RECORD -> definition.getRecordSize()
            ConfigSize.MAXIMUM -> definition.getMaximumSize(imageFormat)
            ConfigSize.MAXIMUM_4_3 -> definition.getMaximum4x3Size(imageFormat)
            ConfigSize.MAXIMUM_16_9 -> definition.getMaximum16x9Size(imageFormat)
            ConfigSize.ULTRA_MAXIMUM -> definition.getUltraMaximumSize(imageFormat)
            ConfigSize.NOT_SUPPORT -> throw IllegalStateException("Not supported config size")
            else -> configSize.relatedFixedSize
        }!!
    }

    /**
     * The Camera2 configuration type for the surface.
     *
     * These are the enumerations defined in [CameraDevice.createCaptureSession] or
     * [CameraCharacteristics.INFO_SESSION_CONFIGURATION_QUERY_VERSION].
     */
    public enum class ConfigType {
        PRIV,
        YUV,
        JPEG,
        JPEG_R,
        RAW,
    }

    /**
     * Represents the source of config sizes, usually some stream config table defined in
     * [CameraDevice.createCaptureSession] or
     * [CameraCharacteristics.INFO_SESSION_CONFIGURATION_QUERY_VERSION].
     */
    public enum class ConfigSource {
        /**
         * Represents the stream config table defined in
         * [CameraCharacteristics.INFO_SESSION_CONFIGURATION_QUERY_VERSION].
         */
        FEATURE_COMBINATION_TABLE,

        /**
         * Represents the guaranteed stream config tables defined in
         * [CameraDevice.createCaptureSession].
         */
        CAPTURE_SESSION_TABLES,
    }

    /**
     * The Camera2 stream sizes for the surface.
     *
     * These are the enumerations defined in [ CameraDevice.createCaptureSession] for most cases, or
     * [CameraCharacteristics.INFO_SESSION_CONFIGURATION_QUERY_VERSION] for feature combination
     * cases.
     *
     * @param id an integer identifier of the config size.
     * @param relatedFixedSize the fixed size that is related to the provided [ConfigSize], null if
     *   none. Depending on the source table, it may act as the upper bound for the config size
     *   (e.g. concurrent camera guaranteed stream combo table) or it may be the exact size (e.g.
     *   feature combination table). For config sizes without any pre-defined fixed related to it
     *   (e.g. [ConfigSize.RECORD]), a null value will be returned.
     */
    public enum class ConfigSize(public val id: Int, public val relatedFixedSize: Size? = null) {
        /** Default VGA size is 640x480, which is the default size of Image Analysis. */
        VGA(0, Size(640, 480)),

        /** X_VGA size refers to 1024x768 which is of 4:3 aspect ratio. */
        X_VGA(1, Size(1024, 768)),

        /**
         * Represents 720P (1280x720) resolution of 16:9 resolution.
         *
         * For cases like concurrent camera which supports lower resolutions as well for a specified
         * stream size, it refers to the camera device's maximum resolution for that format from
         * [StreamConfigurationMap.getOutputSizes] or to 720p (1280x720), whichever is smaller.
         *
         * For cases like feature combination which supports only the exact resolutions for a
         * specified stream size, not lower resolutions, it refers to exactly 720P (1280x720).
         */
        S720P_16_9(2, Size(1280, 720)),

        /**
         * PREVIEW refers to the best size match to the device's screen resolution, or to 1080p
         * (1920x1080), whichever is smaller.
         */
        PREVIEW(3),

        /**
         * Represents 1080P (1440x1080) resolution of 4:3 aspect ratio.
         *
         * For cases like concurrent camera which supports lower resolutions as well for a specified
         * stream size, it refers to the camera device's maximum resolution for that format from
         * [StreamConfigurationMap.getOutputSizes] or to 1080P (1440x1080), whichever is smaller.
         *
         * For cases like feature combination which supports only the exact resolutions for a
         * specified stream size, not lower resolutions, it refers to exactly 1080P (1440x1080).
         */
        S1080P_4_3(4, Size(1440, 1080)),

        /**
         * Represents 1080P (1920x1080) resolution of 16:9 aspect ratio.
         *
         * For cases like concurrent camera which supports lower resolutions as well for a specified
         * stream size, it refers to the camera device's maximum resolution for that format from
         * [StreamConfigurationMap.getOutputSizes] or to 1080P (1920x1080), whichever is smaller.
         *
         * For cases like feature combination which supports only the exact resolutions for a
         * specified stream size, not lower resolutions, it refers to exactly 1080P (1920x1080).
         */
        S1080P_16_9(5, Size(1920, 1080)),

        /**
         * Represents 1440P (1920x1440) resolution of 4:3 aspect ratio.
         *
         * For cases like concurrent camera which supports lower resolutions as well for a specified
         * stream size, it refers to the camera device's maximum resolution for that format from
         * [StreamConfigurationMap.getOutputSizes] or to 1440P (1920x1440), whichever is smaller.
         *
         * For cases like feature combination which supports only the exact resolutions for a
         * specified stream size, not lower resolutions, it refers to exactly 1440P (1920x1440).
         */
        S1440P_4_3(6, Size(1920, 1440)),

        /**
         * Represents 1440P (2560x1440) resolution of 16:9 aspect ratio.
         *
         * For cases like concurrent camera which supports lower resolutions as well for a specified
         * stream size, it refers to the camera device's maximum resolution for that format from
         * [StreamConfigurationMap.getOutputSizes] or to 1440P (2560x1440), whichever is smaller.
         *
         * For cases like feature combination which supports only the exact resolutions for a
         * specified stream size, not lower resolutions, it refers to exactly 1440P (2560x1440).
         */
        S1440P_16_9(7, Size(2560, 1440)),

        /**
         * Represents UHD (3840x2160) resolution, which is of 16:9 aspect ratio.
         *
         * For cases like concurrent camera which supports lower resolutions as well for a specified
         * stream size, it refers to the camera device's maximum resolution for that format from
         * [StreamConfigurationMap.getOutputSizes] or to UHD (3840x2160), whichever is smaller.
         *
         * For cases like feature combination which supports only the exact resolutions for a
         * specified stream size, not lower resolutions, it refers to exactly UHD (3840x2160).
         */
        UHD(8, Size(3840, 2160)),

        /**
         * RECORD refers to the camera device's maximum supported recording resolution, as
         * determined by CamcorderProfile.
         */
        RECORD(9),

        /**
         * MAXIMUM refers to the camera device's maximum output resolution for that format or target
         * from StreamConfigurationMap.getOutputSizes() or getHighResolutionOutputSizes() in the
         * default sensor pixel mode.
         */
        MAXIMUM(10),

        /**
         * Refers to the camera device's maximum 4:3 output resolution for that format or target
         * from [StreamConfigurationMap.getOutputSizes] or
         * [StreamConfigurationMap.getHighResolutionOutputSizes] in the default sensor pixel mode.
         */
        MAXIMUM_4_3(11),

        /**
         * Refers to the camera device's maximum 16:9 output resolution for that format or target
         * from [StreamConfigurationMap.getOutputSizes] or
         * [StreamConfigurationMap.getHighResolutionOutputSizes] in the default sensor pixel mode.
         */
        MAXIMUM_16_9(12),

        /**
         * ULTRA_MAXIMUM refers to the camera device's maximum output resolution for that format or
         * target from StreamConfigurationMap.getOutputSizes() or getHighResolutionOutputSizes() in
         * the maximum resolution sensor pixel mode.
         */
        ULTRA_MAXIMUM(13),

        /** NOT_SUPPORT is for the size larger than MAXIMUM */
        NOT_SUPPORT(14),
    }
}
