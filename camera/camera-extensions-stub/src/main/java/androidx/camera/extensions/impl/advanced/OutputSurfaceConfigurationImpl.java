/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.extensions.impl.advanced;

import android.hardware.camera2.params.ColorSpaceProfiles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * For specifying the output surface configurations for the extension.
 *
 * @since 1.4
 */
public interface OutputSurfaceConfigurationImpl {
    /**
     * gets the preview {@link OutputSurfaceImpl}, which may contain a <code>null</code> surface
     * if the app doesn't specify the preview output surface.
     */
    @NonNull
    OutputSurfaceImpl getPreviewOutputSurface();

    /**
     * gets the still capture {@link OutputSurfaceImpl} which may contain a <code>null</code>
     * surface if the app doesn't specify the still capture output surface.
     */
    @NonNull
    OutputSurfaceImpl getImageCaptureOutputSurface();

    /**
     * gets the image analysis {@link OutputSurfaceImpl}.
     */
    @Nullable
    OutputSurfaceImpl getImageAnalysisOutputSurface();

    /**
     * gets the postview {@link OutputSurfaceImpl} which may contain a <code>null</code> surface
     * if the app doesn't specify the postview output surface.
     */
    @Nullable
    OutputSurfaceImpl getPostviewOutputSurface();

    /**
     * Gets the color space.
     *
     * @since 1.5
     */
    default int getColorSpace() {
        return ColorSpaceProfiles.UNSPECIFIED;
    }
}
