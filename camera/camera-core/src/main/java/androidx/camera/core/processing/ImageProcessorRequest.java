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

package androidx.camera.core.processing;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProcessor;
import androidx.camera.core.ImageProxy;

/**
 * Internal implementation of {@link ImageProcessor.Request} for sending {@link ImageProxy} to
 * effect implementations.
 */
public class ImageProcessorRequest implements ImageProcessor.Request {
    @NonNull
    private final ImageProxy mImageProxy;
    private final int mOutputFormat;

    public ImageProcessorRequest(@NonNull ImageProxy imageProxy, int outputFormat) {
        mImageProxy = imageProxy;
        mOutputFormat = outputFormat;
    }

    @NonNull
    @Override
    public ImageProxy getInputImage() {
        return mImageProxy;
    }

    @Override
    public int getOutputFormat() {
        return mOutputFormat;
    }
}
