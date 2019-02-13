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

package androidx.camera.core;

import android.util.Size;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.google.auto.value.AutoValue;

import java.util.Map;

/**
 * Camera device surface size definition
 *
 * <p>{@link android.hardware.camera2.CameraDevice#createCaptureSession} defines the default
 * guaranteed stream combinations for different hardware level devices.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
@SuppressWarnings("AutoValueImmutableFields")
@AutoValue
public abstract class SurfaceSizeDefinition {

    /** Prevent subclassing */
    SurfaceSizeDefinition() {
    }

    /**
     * Create a SurfaceSizeDefinition object with input analysis, preview, record and maximum sizes
     *
     * @param analysisSize   Default ANALYSIS size is * 640x480.
     * @param previewSize    PREVIEW refers to the best size match to the device's screen
     *                       resolution,
     *                       or to 1080p * (1920x1080), whichever is smaller.
     * @param recordSize     RECORD refers to the camera device's maximum supported * recording
     *                       resolution, as determined by CamcorderProfile.
     * @param maximumSizeMap MAXIMUM refers to the camera * device's maximum output resolution for
     *                       that format or target from * StreamConfigurationMap.getOutputSizes(int)
     * @return new {@link SurfaceSizeDefinition} object
     */
    public static SurfaceSizeDefinition create(
            Size analysisSize,
            Size previewSize,
            Size recordSize,
            Map<Integer, Size> maximumSizeMap) {
        return new AutoValue_SurfaceSizeDefinition(
                analysisSize, previewSize, recordSize, maximumSizeMap);
    }

    /** Returns the size of an ANALYSIS stream. */
    public abstract Size getAnalysisSize();

    /** Returns the size of a PREVIEW stream. */
    public abstract Size getPreviewSize();

    /** Returns the size of a RECORD stream*/
    public abstract Size getRecordSize();

    /**
     * Returns a map of image format to resolution.
     * @return a map with the image format as the key and resolution as the value.
     */
    public abstract Map<Integer, Size> getMaximumSizeMap();
}
