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

import android.graphics.ImageFormat;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.camera2.impl.Camera2CameraCaptureResultConverter;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraCaptureResults;
import androidx.camera.core.impl.CaptureProcessor;
import androidx.camera.core.impl.ImageProxyBundle;
import androidx.camera.extensions.impl.PreviewImageProcessorImpl;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * A {@link CaptureProcessor} that calls a vendor provided preview processing implementation.
 */
public final class AdaptingPreviewProcessor implements CaptureProcessor, CloseableProcessor {
    private static final String TAG = "AdaptingPreviewProcesso";
    private final PreviewImageProcessorImpl mImpl;
    private BlockingCloseAccessCounter mAccessCounter = new BlockingCloseAccessCounter();

    public AdaptingPreviewProcessor(@NonNull PreviewImageProcessorImpl impl) {
        mImpl = impl;
    }

    @Override
    public void onOutputSurface(@NonNull Surface surface, int imageFormat) {
        if (!mAccessCounter.tryIncrement()) {
            return;
        }

        try {
            mImpl.onOutputSurface(surface, imageFormat);
            // No input formats other than YUV_420_888 are allowed.
            mImpl.onImageFormatUpdate(ImageFormat.YUV_420_888);
        } finally {
            mAccessCounter.decrement();
        }
    }

    @Override
    @ExperimentalGetImage
    public void process(@NonNull ImageProxyBundle bundle) {
        List<Integer> ids = bundle.getCaptureIds();
        Preconditions.checkArgument(ids.size() == 1,
                "Processing preview bundle must be 1, but found " + ids.size());

        ListenableFuture<ImageProxy> imageProxyListenableFuture = bundle.getImageProxy(
                ids.get(0));
        Preconditions.checkArgument(imageProxyListenableFuture.isDone());

        ImageProxy imageProxy;
        try {

            imageProxy = imageProxyListenableFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            Logger.e(TAG, "Unable to retrieve ImageProxy from bundle");
            return;
        }

        Image image = imageProxy.getImage();

        ImageInfo imageInfo = imageProxy.getImageInfo();
        CameraCaptureResult result =
                CameraCaptureResults.retrieveCameraCaptureResult(imageInfo);
        CaptureResult captureResult =
                Camera2CameraCaptureResultConverter.getCaptureResult(result);

        TotalCaptureResult totalCaptureResult = null;
        if (captureResult instanceof TotalCaptureResult) {
            totalCaptureResult = (TotalCaptureResult) captureResult;
        }

        if (image == null) {
            return;
        }

        if (!mAccessCounter.tryIncrement()) {
            return;
        }

        try {
            mImpl.process(image, totalCaptureResult);
        } finally {
            mAccessCounter.decrement();
        }
    }

    @Override
    public void onResolutionUpdate(@NonNull Size size) {
        if (!mAccessCounter.tryIncrement()) {
            return;
        }

        try {
            mImpl.onResolutionUpdate(size);
        } finally {
            mAccessCounter.decrement();
        }
    }

    @Override
    public void close() {
        mAccessCounter.destroyAndWaitForZeroAccess();
    }
}
