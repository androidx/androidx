/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.media.Image;
import android.media.ImageReader;

import androidx.annotation.GuardedBy;

import java.util.Collections;
import java.util.List;

/**
 * An {@link ImageReader.OnImageAvailableListener} which forks and forwards newly available images
 * to multiple {@link ImageReaderProxy} instances.
 */
final class ForwardingImageReaderListener implements ImageReader.OnImageAvailableListener {
    @GuardedBy("this")
    private final List<QueuedImageReaderProxy> mImageReaders;

    /**
     * Creates a new forwarding listener.
     *
     * @param imageReaders list of image readers which will receive a copy of every new image
     * @return new {@link ForwardingImageReaderListener} instance
     */
    ForwardingImageReaderListener(List<QueuedImageReaderProxy> imageReaders) {
        mImageReaders = Collections.unmodifiableList(imageReaders);
    }

    @Override
    public synchronized void onImageAvailable(ImageReader imageReader) {
        Image image = imageReader.acquireNextImage();
        ImageProxy imageProxy = new AndroidImageProxy(image);
        ReferenceCountedImageProxy referenceCountedImageProxy =
                new ReferenceCountedImageProxy(imageProxy);
        for (QueuedImageReaderProxy imageReaderProxy : mImageReaders) {
            synchronized (imageReaderProxy) {
                if (!imageReaderProxy.isClosed()) {
                    ImageProxy forkedImage = referenceCountedImageProxy.fork();
                    ForwardingImageProxy imageToEnqueue =
                            ImageProxyDownsampler.downsample(
                                    forkedImage,
                                    imageReaderProxy.getWidth(),
                                    imageReaderProxy.getHeight(),
                                    ImageProxyDownsampler.DownsamplingMethod.AVERAGING);
                    imageReaderProxy.enqueueImage(imageToEnqueue);
                }
            }
        }
        referenceCountedImageProxy.close();
    }
}
