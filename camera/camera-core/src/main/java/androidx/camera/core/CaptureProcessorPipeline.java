/*
 * Copyright 2021 The Android Open Source Project
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

import android.graphics.ImageFormat;
import android.media.ImageReader;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.CaptureProcessor;
import androidx.camera.core.impl.ImageProxyBundle;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.TagBundle;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * A CaptureProcessor which can link two CaptureProcessors.
 */
class CaptureProcessorPipeline implements CaptureProcessor {
    private final CaptureProcessor mPreCaptureProcessor;
    private final CaptureProcessor mPostCaptureProcessor;
    private final Executor mExecutor;
    private final int mMaxImages;
    private ImageReaderProxy mIntermediateImageReader = null;
    private ImageInfo mSourceImageInfo = null;

    /**
     * Creates a {@link CaptureProcessorPipeline} to link two CaptureProcessors to process the
     * captured images.
     *
     * @param preCaptureProcessor  The pre-processing {@link CaptureProcessor} which must output
     *                             YUV_420_888 {@link ImageProxy} for the post-processing
     *                             {@link CaptureProcessor} to process.
     * @param maxImages            the maximum image buffer count used to create the intermediate
     *                             {@link ImageReaderProxy} to receive the processed
     *                             {@link ImageProxy} from the
     *                             pre-processing {@link CaptureProcessor}.
     * @param postCaptureProcessor The post-processing {@link CaptureProcessor} which can process
     *                             an {@link ImageProxy} of YUV_420_888 format . It must be able
     *                             to process the image without referencing to the
     *                             {@link TagBundle} and capture id.
     * @param executor             the {@link Executor} used by the post-processing
     * {@link CaptureProcessor}
     *                             to process the {@link ImageProxy}.
     */
    CaptureProcessorPipeline(@NonNull CaptureProcessor preCaptureProcessor, int maxImages,
            @NonNull CaptureProcessor postCaptureProcessor, @NonNull Executor executor) {
        mPreCaptureProcessor = preCaptureProcessor;
        mPostCaptureProcessor = postCaptureProcessor;
        mExecutor = executor;
        mMaxImages = maxImages;
    }

    @Override
    public void onOutputSurface(@NonNull Surface surface, int imageFormat) {
        // Updates the output surface to the post-processing CaptureProcessor
        mPostCaptureProcessor.onOutputSurface(surface, imageFormat);
    }

    @Override
    public void process(@NonNull ImageProxyBundle bundle) {
        List<Integer> ids = bundle.getCaptureIds();
        ListenableFuture<ImageProxy> imageProxyListenableFuture = bundle.getImageProxy(ids.get(0));
        Preconditions.checkArgument(imageProxyListenableFuture.isDone());

        ImageProxy imageProxy;
        try {
            imageProxy = imageProxyListenableFuture.get();
            ImageInfo imageInfo = imageProxy.getImageInfo();
            // Stores the imageInfo of source image that will be used when when the processed
            // ImageProxy is received from the pre-processing CaptureProcessor.
            mSourceImageInfo = imageInfo;
        } catch (ExecutionException | InterruptedException e) {
            throw new IllegalArgumentException("Can not successfully extract ImageProxy from the "
                    + "ImageProxyBundle.");
        }

        // Calls the pre-processing CaptureProcessor to process the ImageProxyBundle
        mPreCaptureProcessor.process(bundle);
    }

    @Override
    public void onResolutionUpdate(@NonNull Size size) {
        // Creates an intermediate ImageReader to receive the processed image from the
        // pre-processing CaptureProcessor.
        mIntermediateImageReader = new AndroidImageReaderProxy(
                ImageReader.newInstance(size.getWidth(), size.getHeight(),
                        ImageFormat.YUV_420_888, mMaxImages));
        mPreCaptureProcessor.onOutputSurface(mIntermediateImageReader.getSurface(),
                ImageFormat.YUV_420_888);
        mPreCaptureProcessor.onResolutionUpdate(size);

        // Updates the resolution information to the post-processing CaptureProcessor.
        mPostCaptureProcessor.onResolutionUpdate(size);

        // Register the ImageAvailableListener to receive the processed image from the
        // pre-processing CaptureProcessor.
        mIntermediateImageReader.setOnImageAvailableListener(
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(@NonNull ImageReaderProxy imageReader) {
                        postProcess(imageReader.acquireNextImage());
                    }
                }, mExecutor);
    }

    void postProcess(ImageProxy imageProxy) {
        Size resolution = new Size(imageProxy.getWidth(), imageProxy.getHeight());

        // Retrieves information from ImageInfo of source image to create a
        // SettableImageProxyBundle and calls the post-processing CaptureProcessor to process it.
        Preconditions.checkNotNull(mSourceImageInfo);
        String tagBundleKey = mSourceImageInfo.getTagBundle().listKeys().iterator().next();
        int captureId = (Integer) mSourceImageInfo.getTagBundle().getTag(tagBundleKey);
        SettableImageProxy settableImageProxy =
                new SettableImageProxy(imageProxy, resolution, mSourceImageInfo);
        mSourceImageInfo = null;

        SettableImageProxyBundle settableImageProxyBundle = new SettableImageProxyBundle(
                Collections.singletonList(captureId), tagBundleKey);
        settableImageProxyBundle.addImageProxy(settableImageProxy);
        mPostCaptureProcessor.process(settableImageProxyBundle);
    }

    /**
     * Closes the objects generated when creating the {@link CaptureProcessorPipeline}.
     */
    void close() {
        if (mIntermediateImageReader != null) {
            mIntermediateImageReader.clearOnImageAvailableListener();
            mIntermediateImageReader.close();
        }
    }
}
