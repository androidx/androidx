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

package androidx.camera.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * OnImageAvailableListener with blocking behavior. It never drops image without analyzing it.
 *
 * <p> Used with {@link ImageAnalysis}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class ImageAnalysisBlockingAnalyzer extends ImageAnalysisAbstractAnalyzer {

    @Nullable
    @Override
    ImageProxy acquireImage(@NonNull ImageReaderProxy imageReaderProxy) {
        // Use acquireNextImage() so it never drops older images.
        return imageReaderProxy.acquireNextImage();
    }

    @Override
    void onValidImageAvailable(@NonNull ImageProxy imageProxy) {
        ListenableFuture<Void> analyzeFuture = analyzeImage(imageProxy);

        // Callback to close the image only after analysis complete regardless of success
        Futures.addCallback(analyzeFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // No-op. Keep blocking the image reader until user closes the current one.
            }

            @Override
            public void onFailure(Throwable t) {
                imageProxy.close();
            }
        }, CameraXExecutors.directExecutor());
    }

    @Override
    void clearCache() {
        // no-op. The blocking analyzer does not cache images.
    }
}
