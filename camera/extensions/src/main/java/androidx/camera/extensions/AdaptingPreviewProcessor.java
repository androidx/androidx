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

package androidx.camera.extensions;

import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.camera.camera2.impl.Camera2CameraCaptureResultConverter;
import androidx.camera.core.CameraCaptureResult;
import androidx.camera.core.CameraCaptureResults;
import androidx.camera.core.CaptureProcessor;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ImageProxyBundle;
import androidx.camera.extensions.impl.PreviewImageProcessorImpl;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;

/** A {@link CaptureProcessor} that calls a vendor provided preview processing implementation. */
final class AdaptingPreviewProcessor implements CaptureProcessor {
    private static final String TAG = "AdaptingPreviewProcesso";
    private final PreviewImageProcessorImpl mImpl;

    AdaptingPreviewProcessor(PreviewImageProcessorImpl impl) {
        mImpl = impl;
    }

    @Override
    public void onOutputSurface(Surface surface, int imageFormat) {
        mImpl.onOutputSurface(surface, imageFormat);
        mImpl.onImageFormatUpdate(imageFormat);
    }

    @Override
    public void process(ImageProxyBundle bundle) {
        List<Integer> ids = bundle.getCaptureIds();
        Preconditions.checkArgument(ids.size() == 1,
                "Processing preview bundle must be 1, but found " + ids.size());

        ListenableFuture<ImageProxy> imageProxyListenableFuture = bundle.getImageProxy(ids.get(0));
        Preconditions.checkArgument(imageProxyListenableFuture.isDone());

        try {
            ImageProxy imageProxy = imageProxyListenableFuture.get();
            Image image = imageProxy.getImage();
            if (image == null) {
                return;
            }

            ImageInfo imageInfo = imageProxy.getImageInfo();

            CameraCaptureResult result =
                    CameraCaptureResults.retrieveCameraCaptureResult(imageInfo);
            if (result == null) {
                mImpl.process(image, null);
                return;
            }

            CaptureResult captureResult =
                    Camera2CameraCaptureResultConverter.getCaptureResult(result);
            if (captureResult == null) {
                mImpl.process(image, null);
                return;
            }

            if (captureResult instanceof TotalCaptureResult) {
                mImpl.process(imageProxy.getImage(), (TotalCaptureResult) captureResult);
            } else {
                mImpl.process(image, null);
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Unable to retrieve ImageProxy from bundle");
        }
    }

    @Override
    public void onResolutionUpdate(Size size) {
        mImpl.onResolutionUpdate(size);
    }
}
