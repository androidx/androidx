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
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;

/**
 * An {@link ImageProxyBundle} that contains a single {@link ImageProxy}.
 */
final class SingleImageProxyBundle implements ImageProxyBundle {
    private final int mCaptureId;
    private final ImageProxy mImageProxy;

    /**
     * Create an {@link ImageProxyBundle} from a single {@link ImageProxy} using the tag from the
     * ImageProxy as the captureId.
     *
     * @throws IllegalArgumentException if the ImageProxy doesn't contain a tag
     */
    SingleImageProxyBundle(@NonNull ImageProxy imageProxy) {
        ImageInfo imageInfo = imageProxy.getImageInfo();

        if (imageInfo == null) {
            throw new IllegalArgumentException("ImageProxy has no associated ImageInfo");
        }

        Object tag = imageInfo.getTag();

        if (tag == null) {
            throw new IllegalArgumentException("ImageProxy has no associated tag");
        }

        if (!(tag instanceof Integer)) {
            throw new IllegalArgumentException("ImageProxy has tag that isn't an integer");
        }

        mCaptureId = (Integer) tag;
        mImageProxy = imageProxy;
    }

    /**
     * Create an {@link ImageProxyBundle} from a single {@link ImageProxy} using a specified
     * captureId.
     */
    SingleImageProxyBundle(@NonNull ImageProxy imageProxy, int captureId) {
        mCaptureId = captureId;
        mImageProxy = imageProxy;
    }

    /** Close the {@link ImageProxy} that has been wrapped by the bundle. */
    void close() {
        mImageProxy.close();
    }

    @Override
    public ListenableFuture<ImageProxy> getImageProxy(int captureId) {
        if (captureId != mCaptureId) {
            return Futures.immediateFailedFuture(
                    new IllegalArgumentException("Capture id does not exist in the bundle"));
        }
        return Futures.immediateFuture(mImageProxy);
    }

    @Override
    public List<Integer> getCaptureIds() {
        return Collections.singletonList(mCaptureId);
    }
}
