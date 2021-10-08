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

package androidx.camera.camera2.internal;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.compat.workaround.ImageCapturePixelHDRPlus;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.UseCaseConfig;

/**
 * A {@link Camera2CaptureOptionUnpacker} extender for unpacking ImageCapture options into
 * {@link CaptureConfig.Builder}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class ImageCaptureOptionUnpacker extends Camera2CaptureOptionUnpacker {

    static final ImageCaptureOptionUnpacker INSTANCE = new ImageCaptureOptionUnpacker(
            new ImageCapturePixelHDRPlus());

    @NonNull
    private final ImageCapturePixelHDRPlus mImageCapturePixelHDRPlus;

    private ImageCaptureOptionUnpacker(@NonNull ImageCapturePixelHDRPlus imageCapturePixelHDRPlus) {
        mImageCapturePixelHDRPlus = imageCapturePixelHDRPlus;
    }

    @Override
    public void unpack(@NonNull UseCaseConfig<?> config,
            @NonNull final CaptureConfig.Builder builder) {
        super.unpack(config, builder);

        if (!(config instanceof ImageCaptureConfig)) {
            throw new IllegalArgumentException("config is not ImageCaptureConfig");
        }
        ImageCaptureConfig imageCaptureConfig = (ImageCaptureConfig) config;

        Camera2ImplConfig.Builder camera2ConfigBuilder = new Camera2ImplConfig.Builder();

        if (imageCaptureConfig.hasCaptureMode()) {
            mImageCapturePixelHDRPlus.toggleHDRPlus(imageCaptureConfig.getCaptureMode(),
                    camera2ConfigBuilder);
        }

        builder.addImplementationOptions(camera2ConfigBuilder.build());
    }
}
