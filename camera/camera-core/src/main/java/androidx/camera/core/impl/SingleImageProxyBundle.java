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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;

/**
 * An {@link ImageProxyBundle} that contains a single {@link ImageProxy}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class SingleImageProxyBundle implements ImageProxyBundle {
    private final int mCaptureId;
    private final ImageProxy mImageProxy;

    /**
     * Create an {@link ImageProxyBundle} from a single {@link ImageProxy} using a tag's value from
     * the ImageProxy as the captureId.
     *
     * The tagBundleKey is used to query from a TagBundle. It needs to be one of the keys that are
     * in the ImageInfo's TagBundle.
     *
     * @throws IllegalArgumentException if the ImageProxy doesn't contain a tag
     */
    public SingleImageProxyBundle(@NonNull ImageProxy imageProxy,
            @NonNull String tagBundleKey) {
        ImageInfo imageInfo = imageProxy.getImageInfo();

        if (imageInfo == null) {
            throw new IllegalArgumentException("ImageProxy has no associated ImageInfo");
        }

        Integer tagValue = (Integer) imageInfo.getTagBundle().getTag(tagBundleKey);

        if (tagValue == null) {
            throw new IllegalArgumentException("ImageProxy has no associated tag");
        }

        mCaptureId = tagValue;
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
    public void close() {
        mImageProxy.close();
    }

    @Override
    @NonNull
    public ListenableFuture<ImageProxy> getImageProxy(int captureId) {
        if (captureId != mCaptureId) {
            return Futures.immediateFailedFuture(
                    new IllegalArgumentException("Capture id does not exist in the bundle"));
        }
        return Futures.immediateFuture(mImageProxy);
    }

    @Override
    @NonNull
    public List<Integer> getCaptureIds() {
        return Collections.singletonList(mCaptureId);
    }
}
