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

package androidx.camera.core.impl;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCaptureSession.StateCallback;
import android.os.Handler;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.internal.utils.SizeUtil;

import com.google.auto.value.AutoValue;

import java.util.List;

/**
 * Surface configuration type and size pair
 *
 * <p>{@link android.hardware.camera2.CameraDevice#createCaptureSession} defines the default
 * guaranteed stream combinations for different hardware level devices. It defines what combination
 * of surface configuration type and size pairs can be supported for different hardware level camera
 * devices.
 */
@AutoValue
public abstract class SurfaceConfig {
    public static final long DEFAULT_STREAM_USE_CASE_VALUE = 0;
    /** Prevent subclassing */
    SurfaceConfig() {
    }

    /**
     * Creates a new instance of SurfaceConfig with the given parameters.
     */
    @NonNull
    public static SurfaceConfig create(@NonNull ConfigType type, @NonNull ConfigSize size) {
        return new AutoValue_SurfaceConfig(type, size, DEFAULT_STREAM_USE_CASE_VALUE);
    }

    /**
     * Creates a new instance of SurfaceConfig with the given parameters.
     */
    @NonNull
    public static SurfaceConfig create(@NonNull ConfigType type, @NonNull ConfigSize size,
            long streamUseCase) {
        return new AutoValue_SurfaceConfig(type, size, streamUseCase);
    }

    /** Returns the configuration type. */
    @NonNull
    public abstract ConfigType getConfigType();

    /** Returns the configuration size. */
    @NonNull
    public abstract ConfigSize getConfigSize();

    /**
     * Returns the stream use case.
     * <p>Stream use case constants are implementation-specific constants that allow the
     * implementation to optimize power and quality characteristics of a stream depending on how
     * it will be used.
     * <p> Stream use case is an int flag used to specify the purpose of the stream associated
     * with this surface. Use cases for the camera2 implementation that are available on devices can
     * be found in
     * {@link android.hardware.camera2.CameraCharacteristics#SCALER_AVAILABLE_STREAM_USE_CASES}
     *
     * <p>See {@link android.hardware.camera2.params.OutputConfiguration#setStreamUseCase}
     * to see how Camera2 framework uses this.
     */
    public abstract long getStreamUseCase();

    /**
     * Check whether the input surface configuration has a smaller size than this object and can be
     * supported
     *
     * @param surfaceConfig the surface configuration to be compared
     * @return the check result that whether it could be supported
     */
    public final boolean isSupported(@NonNull SurfaceConfig surfaceConfig) {
        boolean isSupported = false;
        ConfigType configType = surfaceConfig.getConfigType();
        ConfigSize configSize = surfaceConfig.getConfigSize();

        // Check size and type to make sure it could be supported
        if (configSize.getId() <= getConfigSize().getId() && configType == getConfigType()) {
            isSupported = true;
        }
        return isSupported;
    }

    /**
     * Gets {@link ConfigType} from image format.
     *
     * <p> PRIV refers to any target whose available sizes are found using
     * StreamConfigurationMap.getOutputSizes(Class) with no direct application-visible format,
     * YUV refers to a target Surface using the ImageFormat.YUV_420_888 format, JPEG refers to
     * the ImageFormat.JPEG or ImageFormat.JPEG_R format, and RAW refers to the
     * ImageFormat.RAW_SENSOR format.
     */
    @NonNull
    public static SurfaceConfig.ConfigType getConfigType(int imageFormat) {
        if (imageFormat == ImageFormat.YUV_420_888) {
            return SurfaceConfig.ConfigType.YUV;
        } else if (imageFormat == ImageFormat.JPEG) {
            return SurfaceConfig.ConfigType.JPEG;
        } else if (imageFormat == ImageFormat.JPEG_R) {
            return SurfaceConfig.ConfigType.JPEG_R;
        } else if (imageFormat == ImageFormat.RAW_SENSOR) {
            return SurfaceConfig.ConfigType.RAW;
        } else {
            return SurfaceConfig.ConfigType.PRIV;
        }
    }

    /**
     * Transform to a SurfaceConfig object with image format and size info
     *
     * @param cameraMode            the working camera mode.
     * @param imageFormat           the image format info for the surface configuration object
     * @param size                  the size info for the surface configuration object
     * @param surfaceSizeDefinition the surface definition for the surface configuration object
     * @return new {@link SurfaceConfig} object
     */
    @NonNull
    public static SurfaceConfig transformSurfaceConfig(
            @CameraMode.Mode int cameraMode,
            int imageFormat,
            @NonNull Size size,
            @NonNull SurfaceSizeDefinition surfaceSizeDefinition) {
        ConfigType configType =
                SurfaceConfig.getConfigType(imageFormat);
        ConfigSize configSize = ConfigSize.NOT_SUPPORT;

        // Compare with surface size definition to determine the surface configuration size
        int sizeArea = SizeUtil.getArea(size);

        if (cameraMode == CameraMode.CONCURRENT_CAMERA) {
            if (sizeArea <= SizeUtil.getArea(surfaceSizeDefinition.getS720pSize(imageFormat))) {
                configSize = ConfigSize.s720p;
            } else if (sizeArea <= SizeUtil.getArea(surfaceSizeDefinition.getS1440pSize(
                    imageFormat))) {
                configSize = ConfigSize.s1440p;
            }
        } else {
            if (sizeArea <= SizeUtil.getArea(surfaceSizeDefinition.getAnalysisSize())) {
                configSize = ConfigSize.VGA;
            } else if (sizeArea <= SizeUtil.getArea(surfaceSizeDefinition.getPreviewSize())) {
                configSize = ConfigSize.PREVIEW;
            } else if (sizeArea <= SizeUtil.getArea(surfaceSizeDefinition.getRecordSize())) {
                configSize = ConfigSize.RECORD;
            } else if (sizeArea <= SizeUtil.getArea(
                    surfaceSizeDefinition.getMaximumSize(imageFormat))) {
                configSize = ConfigSize.MAXIMUM;
            } else {
                Size ultraMaximumSize = surfaceSizeDefinition.getUltraMaximumSize(imageFormat);
                if (ultraMaximumSize != null && sizeArea <= SizeUtil.getArea(ultraMaximumSize)) {
                    configSize = ConfigSize.ULTRA_MAXIMUM;
                }
            }
        }

        return SurfaceConfig.create(configType, configSize);
    }

    /**
     * The Camera2 configuration type for the surface.
     *
     * <p>These are the enumerations defined in {@link
     * android.hardware.camera2.CameraDevice#createCaptureSession(List, StateCallback, Handler)}.
     */
    public enum ConfigType {
        PRIV,
        YUV,
        JPEG,
        JPEG_R,
        RAW
    }

    /**
     * The Camera2 stream sizes for the surface.
     *
     * <p>These are the enumerations defined in {@link
     * android.hardware.camera2.CameraDevice#createCaptureSession(List, StateCallback, Handler)}.
     */
    public enum ConfigSize {
        /** Default VGA size is 640x480, which is the default size of Image Analysis. */
        VGA(0),
        /**
         * s720p refers to the best size match to the device's screen resolution, or to 720p
         * (1280x720), whichever is smaller.
         */
        s720p(1),
        /**
         * PREVIEW refers to the best size match to the device's screen resolution, or to 1080p
         * (1920x1080), whichever is smaller.
         */
        PREVIEW(2),
        /**
         * s1440p refers to the best size match to the device's screen resolution, or to 1440p
         * (1920x1440), whichever is smaller.
         */
        s1440p(3),
        /**
         * RECORD refers to the camera device's maximum supported recording resolution, as
         * determined by CamcorderProfile.
         */
        RECORD(4),
        /**
         * MAXIMUM refers to the camera device's maximum output resolution for that format or
         * target from StreamConfigurationMap.getOutputSizes() or getHighResolutionOutputSizes()
         * in the default sensor pixel mode.
         */
        MAXIMUM(5),
        /**
         * ULTRA_MAXIMUM refers to the camera device's maximum output resolution for that format or
         * target from StreamConfigurationMap.getOutputSizes() or getHighResolutionOutputSizes()
         * in the maximum resolution sensor pixel mode.
         */
        ULTRA_MAXIMUM(6),
        /** NOT_SUPPORT is for the size larger than MAXIMUM */
        NOT_SUPPORT(7);

        final int mId;

        ConfigSize(int id) {
            mId = id;
        }

        int getId() {
            return mId;
        }
    }
}
