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

import static androidx.camera.core.impl.UseCaseConfig.OPTION_ZSL_DISABLED;

import android.graphics.ImageFormat;
import android.util.Pair;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCapture.CaptureMode;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ImageAnalysisConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.UseCaseConfigFactory;

import java.util.List;

/**
 * Implementation of UseCaseConfigFactory to provide the default extensions configurations for use
 * cases.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ExtensionsUseCaseConfigFactory implements UseCaseConfigFactory {
    private final ImageCaptureConfigProvider mImageCaptureConfigProvider;
    private final PreviewConfigProvider mPreviewConfigProvider;
    private final ImageAnalysisConfigProvider mImageAnalysisConfigProvider;

    public ExtensionsUseCaseConfigFactory(@NonNull VendorExtender vendorExtender) {
        mImageCaptureConfigProvider = new ImageCaptureConfigProvider(vendorExtender);
        mPreviewConfigProvider = new PreviewConfigProvider(vendorExtender);
        mImageAnalysisConfigProvider = new ImageAnalysisConfigProvider(vendorExtender);
    }

    private boolean isImageAnalysisSupported(
            @Nullable List<Pair<Integer, Size[]>> supportedResolutions) {
        if (supportedResolutions == null) {
            return false;
        }

        for (Pair<Integer, Size[]> pair : supportedResolutions) {
            int imageFormat = pair.first;
            Size[] sizes = pair.second;
            if (imageFormat == ImageFormat.YUV_420_888) {
                if (sizes != null && sizes.length > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns the configuration for the given capture type, or <code>null</code> if the
     * configuration cannot be produced.
     */
    @Nullable
    @Override
    public Config getConfig(
            @NonNull CaptureType captureType,
            @CaptureMode int captureMode
    ) {
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
            case IMAGE_ANALYSIS: // invoked when ImageAnalysis is bound.
                ImageAnalysisConfig config =  mImageAnalysisConfigProvider.getConfig();
                List<Pair<Integer, Size[]>> supportedResolutions =
                        config.getSupportedResolutions(/* valueIfMissing */ null);
                if (!isImageAnalysisSupported(supportedResolutions)) {
                    // This will be thrown when invoking bindToLifecycle.
                    throw new IllegalArgumentException(
                            "ImageAnalysis is not supported when Extension is enabled on "
                                    + "this device. Check "
                                    + "ExtensionsManager.isImageAnalysisSupported before binding "
                                    + "the ImageAnalysis use case.");
                }
                mutableOptionsBundle = MutableOptionsBundle.from(config);
                break;
            case VIDEO_CAPTURE:
                throw new IllegalArgumentException("CameraX Extensions doesn't support "
                        + "VideoCapture!");
            default:
                return null;
        }

        // Disable ZSL when Extension is ON.
        mutableOptionsBundle.insertOption(OPTION_ZSL_DISABLED, true);

        return OptionsBundle.from(mutableOptionsBundle);
    }
}
