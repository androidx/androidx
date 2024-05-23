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

import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.util.Map;

/**
 * Camera device surface size definition
 *
 * <p>{@link android.hardware.camera2.CameraDevice#createCaptureSession} defines the default
 * guaranteed stream combinations for different hardware level devices.
 */
@SuppressWarnings("AutoValueImmutableFields")
@AutoValue
public abstract class SurfaceSizeDefinition {

    /** Prevent subclassing */
    SurfaceSizeDefinition() {
    }

    /**
     * Create a SurfaceSizeDefinition object with input analysis, preview, record and maximum sizes
     *
     * @param analysisSize        Default ANALYSIS size is * 640x480.
     * @param s720pSizeMap        The format to size map of an s720p size stream. s720p refers to
     *                            the 720p (1280 x 720) or the maximum supported resolution for the
     *                            particular format returned by
     *                            {@link StreamConfigurationMap#getOutputSizes(int)}, whichever is
     *                            smaller.
     * @param previewSize         PREVIEW refers to the best size match to the device's screen
     *                            resolution, or to 1080p * (1920x1080), whichever is smaller.
     * @param s1440pSizeMap       The format to size map of an s1440p size stream. s1440p refers
     *                            to the 1440p (1920 x 1440) or the maximum supported resolution
     *                            for the particular format returned by
     *                            {@link StreamConfigurationMap#getOutputSizes(int)}, whichever is
     *                            smaller.
     * @param recordSize          RECORD refers to the camera device's maximum supported * recording
     *                            resolution, as determined by CamcorderProfile.
     * @param maximumSizeMap      The format to size map of an MAXIMUM size stream. MAXIMUM
     *                            refers to the camera device's maximum output resolution in the
     *                            default sensor pixel mode.
     * @param ultraMaximumSizeMap The format to size map of an ULTRA_MAXIMUM size stream.
     *                            ULTRA_MAXIMUM refers to the camera device's maximum output
     *                            resolution in the maximum resolution sensor pixel mode.
     * @return new {@link SurfaceSizeDefinition} object
     */
    @NonNull
    public static SurfaceSizeDefinition create(
            @NonNull Size analysisSize,
            @NonNull Map<Integer, Size> s720pSizeMap,
            @NonNull Size previewSize,
            @NonNull Map<Integer, Size> s1440pSizeMap,
            @NonNull Size recordSize,
            @NonNull Map<Integer, Size> maximumSizeMap,
            @NonNull Map<Integer, Size> ultraMaximumSizeMap) {
        return new AutoValue_SurfaceSizeDefinition(
                analysisSize,
                s720pSizeMap,
                previewSize,
                s1440pSizeMap,
                recordSize,
                maximumSizeMap,
                ultraMaximumSizeMap);
    }

    /** Returns the size of an ANALYSIS stream. */
    @NonNull
    public abstract Size getAnalysisSize();

    /** Returns the format to size map of an s720p stream. */
    @NonNull
    public abstract Map<Integer, Size> getS720pSizeMap();

    /** Returns the size of a PREVIEW stream. */
    @NonNull
    public abstract Size getPreviewSize();

    /** Returns the format to size map of an s1440p stream. */
    @NonNull
    public abstract Map<Integer, Size> getS1440pSizeMap();

    /** Returns the size of a RECORD stream*/
    @NonNull
    public abstract Size getRecordSize();

    /** Returns the format to size map of an MAXIMUM stream. */
    @NonNull
    public abstract Map<Integer, Size> getMaximumSizeMap();

    /** Returns the format to size map of an ULTRA_MAXIMUM stream. */
    @NonNull
    public abstract Map<Integer, Size> getUltraMaximumSizeMap();

    /**
     * Returns the s720p size for the specified format, or {@code null} null if there is no data
     * for the format.
     */
    @NonNull
    public Size getS720pSize(int format) {
        return getS720pSizeMap().get(format);
    }

    /**
     * Returns the s1440p size for the specified format, or {@code null} null if there is no data
     * for the format.
     */
    @NonNull
    public Size getS1440pSize(int format) {
        return getS1440pSizeMap().get(format);
    }

    /**
     * Returns the MAXIMUM size for the specified format, or {@code null} null if there is no
     * data for the format.
     */
    @NonNull
    public Size getMaximumSize(int format) {
        return getMaximumSizeMap().get(format);
    }

    /**
     * Returns the ULTRA_MAXIMUM size for the specified format, or {@code null} if the device
     * doesn't support maximum resolution sensor pixel mode.
     */
    @Nullable
    public Size getUltraMaximumSize(int format) {
        return getUltraMaximumSizeMap().get(format);
    }
}
