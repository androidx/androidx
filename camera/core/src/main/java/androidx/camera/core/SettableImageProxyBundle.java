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

import androidx.annotation.GuardedBy;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link ImageProxyBundle} where the {@link ImageProxy} and {@link ImageInfo} can be
 * added after creation.
 */
final class SettableImageProxyBundle implements ImageProxyBundle {
    private final Object mLock = new Object();

    // Whether or not the bundle has been closed or not
    @GuardedBy("mLock")
    private boolean mClosed = false;

    /**
     * Map of id to {@link ImageInfo} waiting to synchronize with a {@link ImageProxy}.
     */
    @GuardedBy("mLock")
    private final Map<Integer, ImageInfo> mImageInfoToMatch = new HashMap<>();

    /** Map of timestamp to {@link ImageProxy} waiting to synchronize with a {@link ImageInfo}. */
    @GuardedBy("mLock")
    private final Map<Long, ImageProxy> mImageProxiesToMatch = new HashMap<>();

    /**
     * All the {@link ImageProxy} that have been added since they need to be closed once the
     * ImageProxyBundle has been closed.
     */
    @GuardedBy("mLock")
    private final List<ImageProxy> mImageProxiesToClear = new ArrayList<>();

    /** Map of id to {@link ImageProxy} Future. */
    @GuardedBy("mLock")
    private final Map<Integer, SettableFuture<ImageProxy>> mFutureResults = new HashMap<>();

    @Override
    public ListenableFuture<ImageProxy> getImageProxy(int captureId) {
        synchronized (mLock) {
            if (mClosed) {
                throw new IllegalStateException("ImageProxyBundle already closed.");
            }
            // Returns the future that has been set if it exists
            if (!mFutureResults.containsKey(captureId)) {
                throw new IllegalArgumentException(
                        "ImageProxyBundle does not contain this id: " + captureId);
            }
            return mFutureResults.get(captureId);
        }
    }

    @Override
    public List<Integer> getCaptureIds() {
        return Collections.unmodifiableList(new ArrayList<>(mFutureResults.keySet()));
    }

    /**
     * Create a {@link ImageProxyBundle} for captures with the given ids.
     *
     * @param captureIds The set of captureIds contained by the ImageProxyBundle
     */
    SettableImageProxyBundle(List<Integer> captureIds) {
        for (Integer captureId : captureIds) {
            SettableFuture<ImageProxy> future = SettableFuture.create();
            mFutureResults.put(captureId, future);
        }
    }

    /**
     * Add an {@link ImageInfo} to synchronize.
     *
     * @param captureId The identifier for the capture.
     * @param imageInfo The metadata results of the capture.
     * @throws IllegalArgumentException throws if the captureId is not one of the captureIds that
     *                                  the {@link SettableImageProxyBundle} was initialized with.
     */
    void addImageInfo(int captureId, ImageInfo imageInfo) {
        synchronized (mLock) {
            if (mClosed) {
                return;
            }
            if (!mFutureResults.containsKey(captureId)) {
                throw new IllegalArgumentException(
                        "Adding result with invalid id to bundle: " + captureId);
            }
            mImageInfoToMatch.put(captureId, imageInfo);
            matchImages();
        }
    }

    /**
     * Add an {@link ImageProxy} to synchronize.
     */
    void addImage(ImageProxy imageProxy) {
        synchronized (mLock) {
            if (mClosed) {
                return;
            }
            mImageProxiesToMatch.put(imageProxy.getTimestamp(), imageProxy);
            mImageProxiesToClear.add(imageProxy);
            matchImages();
        }
    }

    /**
     * Flush all the images and results that have been added from the bundle.
     */
    void close() {
        synchronized (mLock) {
            if (mClosed) {
                return;
            }
            mImageInfoToMatch.clear();
            for (ImageProxy imageProxy : mImageProxiesToClear) {
                imageProxy.close();
            }
            mImageProxiesToClear.clear();
            mImageProxiesToMatch.clear();
            mFutureResults.clear();
            mClosed = true;
        }
    }

    /**
     * Match the {@link ImageProxy} and {@link ImageInfo} based on the timestamp.
     *
     * <p>At the end of this method the following will hold true:
     *
     * <p>
     *
     * <ul>
     * <li>Any {@link ImageProxy} that has been matched will be removed from mImageProxiesToMatch.
     * <li>Any {@link ImageInfo} that has been matched will be removed from mImageInfoToMatch.
     * <li>Any {@link ImageProxy} that has been match will be moved into mFutureResults as a
     * {@link SettableImageProxy} which has the matched ImageProxy and ImageInfo
     * </ul>
     *
     * <p>
     */
    private void matchImages() {
        synchronized (mLock) {
            List<Integer> toRemove = new ArrayList<>();
            for (Map.Entry<Integer, ImageInfo> entry : mImageInfoToMatch.entrySet()) {
                ImageInfo imageInfo = entry.getValue();

                long timestamp = imageInfo.getTimestamp();

                // ImageProxy and TotalCaptureResult have been matched together
                if (mImageProxiesToMatch.containsKey(timestamp)) {
                    ImageProxy image = mImageProxiesToMatch.get(timestamp);
                    mImageProxiesToMatch.remove(timestamp);
                    Integer stageId = entry.getKey();
                    toRemove.add(stageId);

                    // futureResult should be non-null because only ImageInfo with valid captureId
                    // can be added to the SettableImageProxyBundle
                    SettableFuture<ImageProxy> futureResult = mFutureResults.get(stageId);
                    futureResult.set(new SettableImageProxy(image, imageInfo));
                }
            }

            for (Integer stageId : toRemove) {
                mImageInfoToMatch.remove(stageId);
            }
        }
    }
}
