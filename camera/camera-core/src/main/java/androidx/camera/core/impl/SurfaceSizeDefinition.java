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

import android.util.Size;

import androidx.annotation.RequiresApi;

import com.google.auto.value.AutoValue;

/**
 * Camera device surface size definition
 *
 * <p>{@link android.hardware.camera2.CameraDevice#createCaptureSession} defines the default
 * guaranteed stream combinations for different hardware level devices.
 */
@SuppressWarnings("AutoValueImmutableFields")
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
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
     * @return new {@link SurfaceSizeDefinition} object
     */
    public static SurfaceSizeDefinition create(
            Size analysisSize,
            Size previewSize,
            Size recordSize) {
        return new AutoValue_SurfaceSizeDefinition(analysisSize, previewSize, recordSize);
    }

    /** Returns the size of an ANALYSIS stream. */
    public abstract Size getAnalysisSize();

    /** Returns the size of a PREVIEW stream. */
    public abstract Size getPreviewSize();

    /** Returns the size of a RECORD stream*/
    public abstract Size getRecordSize();
}
