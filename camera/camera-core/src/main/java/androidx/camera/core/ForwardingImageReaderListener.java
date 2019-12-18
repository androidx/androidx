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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.camera.core.impl.ImageReaderProxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An {@link ImageReaderProxy.OnImageAvailableListener} which forks and forwards newly available
 * images to multiple {@link ImageReaderProxy} instances.
 */
final class ForwardingImageReaderListener implements ImageReaderProxy.OnImageAvailableListener {
    @GuardedBy("this")
    private final List<QueuedImageReaderProxy> mImageReaders;

    /**
     * Creates a new forwarding listener.
     *
     * @param imageReaders list of image readers which will receive a copy of every new image
     * @return new {@link ForwardingImageReaderListener} instance
     */
    ForwardingImageReaderListener(List<QueuedImageReaderProxy> imageReaders) {
        // Make a copy of the incoming List to avoid ConcurrentAccessException.
        mImageReaders = Collections.unmodifiableList(new ArrayList<>(imageReaders));
    }

    @Override
    public synchronized void onImageAvailable(@NonNull ImageReaderProxy imageReaderProxy) {
        ImageProxy imageProxy = imageReaderProxy.acquireNextImage();
        if (imageProxy == null) {
            return;
        }
        ReferenceCountedImageProxy referenceCountedImageProxy =
                new ReferenceCountedImageProxy(imageProxy);
        for (QueuedImageReaderProxy queuedImageReaderProxy : mImageReaders) {
            synchronized (queuedImageReaderProxy) {
                if (!queuedImageReaderProxy.isClosed()) {
                    ImageProxy forkedImage = referenceCountedImageProxy.fork();
                    ForwardingImageProxy imageToEnqueue =
                            ImageProxyDownsampler.downsample(
                                    forkedImage,
                                    queuedImageReaderProxy.getWidth(),
                                    queuedImageReaderProxy.getHeight(),
                                    ImageProxyDownsampler.DownsamplingMethod.AVERAGING);
                    queuedImageReaderProxy.enqueueImage(imageToEnqueue);
                }
            }
        }
        referenceCountedImageProxy.close();
    }
}
