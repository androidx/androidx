/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.extensions.internal;

import android.util.Pair;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.impl.ConfigProvider;
import androidx.camera.core.impl.ImageCaptureConfig;

import java.util.List;

/**
 * Provides extensions related configs for image capture
 */
public class ImageCaptureConfigProvider implements ConfigProvider<ImageCaptureConfig> {

    private final VendorExtender mVendorExtender;

    public ImageCaptureConfigProvider(@NonNull VendorExtender vendorExtender) {
        mVendorExtender = vendorExtender;
    }

    @NonNull
    @Override
    public ImageCaptureConfig getConfig() {
        ImageCapture.Builder builder = new ImageCapture.Builder();
        updateBuilderConfig(builder, mVendorExtender);

        return builder.getUseCaseConfig();
    }

    /**
     * Update extension related configs to the builder.
     */
    void updateBuilderConfig(@NonNull ImageCapture.Builder builder,
            @NonNull VendorExtender vendorExtender) {
        List<Pair<Integer, Size[]>> supportedResolutions =
                vendorExtender.getSupportedCaptureOutputResolutions();
        builder.setSupportedResolutions(supportedResolutions);
        builder.setHighResolutionDisabled(true);
    }
}
