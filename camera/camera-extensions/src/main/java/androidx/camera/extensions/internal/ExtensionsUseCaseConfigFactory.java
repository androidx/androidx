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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.extensions.ExtensionMode;

/**
 * Implementation of UseCaseConfigFactory to provide the default extensions configurations for use
 * cases.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ExtensionsUseCaseConfigFactory implements UseCaseConfigFactory {
    private final ImageCaptureConfigProvider mImageCaptureConfigProvider;
    private final PreviewConfigProvider mPreviewConfigProvider;

    public ExtensionsUseCaseConfigFactory(
            @ExtensionMode.Mode int mode,
            @NonNull VendorExtender vendorExtender,
            @NonNull Context context) {
        mImageCaptureConfigProvider = new ImageCaptureConfigProvider(mode, vendorExtender, context);
        mPreviewConfigProvider = new PreviewConfigProvider(mode, vendorExtender, context);
    }

    /**
     * Returns the configuration for the given capture type, or <code>null</code> if the
     * configuration cannot be produced.
     */
    @Nullable
    @Override
    public Config getConfig(@NonNull CaptureType captureType) {
        MutableOptionsBundle mutableOptionsBundle;

        switch (captureType) {
            case IMAGE_CAPTURE:
                mutableOptionsBundle =
                        MutableOptionsBundle.from(mImageCaptureConfigProvider.getConfig());
                break;
            case PREVIEW:
                mutableOptionsBundle =
                        MutableOptionsBundle.from(mPreviewConfigProvider.getConfig());
                break;
            case VIDEO_CAPTURE:
                throw new IllegalArgumentException("CameraX Extensions doesn't support "
                        + "VideoCapture!");
            default:
                return null;
        }

        return OptionsBundle.from(mutableOptionsBundle);
    }
}
