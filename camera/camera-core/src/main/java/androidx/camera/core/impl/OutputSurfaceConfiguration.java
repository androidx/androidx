/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

/**
 * The configuration for all the output surfaces of the SessionProcessor.
 */
@AutoValue
public abstract class OutputSurfaceConfiguration {
    /**
     * Creates an OutputSurface instance.
     */
    @NonNull
    public static OutputSurfaceConfiguration create(
            @NonNull OutputSurface previewOutputSurface,
            @NonNull OutputSurface imageCaptureOutputSurface,
            @Nullable OutputSurface imageAnalysisOutputSurface,
            @Nullable OutputSurface postviewOutputSurface) {
        return new AutoValue_OutputSurfaceConfiguration(
                previewOutputSurface, imageCaptureOutputSurface,
                imageAnalysisOutputSurface, postviewOutputSurface);
    }
    /**
     * gets the preview {@link OutputSurface}.
     */
    @NonNull
    public abstract OutputSurface getPreviewOutputSurface();

    /**
     * gets the still capture {@link OutputSurface}.
     */
    @NonNull
    public abstract OutputSurface getImageCaptureOutputSurface();

    /**
     * gets the image analysis {@link OutputSurface}.
     */
    @Nullable
    public abstract OutputSurface getImageAnalysisOutputSurface();

    /**
     * gets the postview {@link OutputSurface}.
     */
    @Nullable
    public abstract OutputSurface getPostviewOutputSurface();
}
