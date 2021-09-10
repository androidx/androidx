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

import android.util.SparseArray;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.camera.core.impl.ImageProxyBundle;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link ImageProxyBundle} with a predefined set of captured ids. The {@link ListenableFuture}
 * for the capture id becomes valid when the corresponding {@link ImageProxy} has been set.
 */
final class SettableImageProxyBundle implements ImageProxyBundle {
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Object mLock = new Object();
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    final SparseArray<CallbackToFutureAdapter.Completer<ImageProxy>> mCompleters =
            new SparseArray<>();
    /** Map of id to {@link ImageProxy} Future. */
    @GuardedBy("mLock")
    private final SparseArray<ListenableFuture<ImageProxy>> mFutureResults = new SparseArray<>();

    @GuardedBy("mLock")
    private final List<ImageProxy> mOwnedImageProxies = new ArrayList<>();

    private final List<Integer> mCaptureIdList;
    private String mTagBundleKey = null;

    // Whether or not the bundle has been closed or not
    @GuardedBy("mLock")
    private boolean mClosed = false;

    /**
     * Create a {@link ImageProxyBundle} for captures with the given ids.
     *
     * @param captureIds    The set of captureIds contained by the ImageProxyBundle
     * @param tagBundleKey `The key for checking desired image from TagBundle
     */
    SettableImageProxyBundle(List<Integer> captureIds, String tagBundleKey) {
        mCaptureIdList = captureIds;
        mTagBundleKey = tagBundleKey;
        setup();
    }

    @Override
    @NonNull
    public ListenableFuture<ImageProxy> getImageProxy(int captureId) {
        synchronized (mLock) {
            if (mClosed) {
                throw new IllegalStateException("ImageProxyBundle already closed.");
            }

            // Returns the future that has been set if it exists
            ListenableFuture<ImageProxy> result = mFutureResults.get(captureId);
            if (result == null) {
                throw new IllegalArgumentException(
                        "ImageProxyBundle does not contain this id: " + captureId);
            }

            return result;
        }
    }

    @Override
    @NonNull
    public List<Integer> getCaptureIds() {
        return Collections.unmodifiableList(mCaptureIdList);
    }

    /**
     * Add an {@link ImageProxy} to synchronize.
     */
    void addImageProxy(ImageProxy imageProxy) {
        synchronized (mLock) {
            if (mClosed) {
                return;
            }

            Integer captureId =
                    (Integer) imageProxy.getImageInfo().getTagBundle().getTag(mTagBundleKey);
            if (captureId == null) {
                throw new IllegalArgumentException("CaptureId is null.");
            }

            // If the CaptureId is associated with this SettableImageProxyBundle, set the
            // corresponding Future. Otherwise, throws exception.
            CallbackToFutureAdapter.Completer<ImageProxy> completer = mCompleters.get(captureId);
            if (completer != null) {
                mOwnedImageProxies.add(imageProxy);
                completer.set(imageProxy);
            } else {
                throw new IllegalArgumentException(
                        "ImageProxyBundle does not contain this id: " + captureId);
            }
        }
    }

    /**
     * Flush all {@link ImageProxy} that have been added.
     */
    void close() {
        synchronized (mLock) {
            if (mClosed) {
                return;
            }
            for (ImageProxy imageProxy : mOwnedImageProxies) {
                imageProxy.close();
            }
            mOwnedImageProxies.clear();
            mFutureResults.clear();
            mCompleters.clear();
            mClosed = true;
        }
    }

    /**
     * Clear all {@link ImageProxy} that have been added and recreate the entries from the bundle.
     */
    void reset() {
        synchronized (mLock) {
            if (mClosed) {
                return;
            }
            for (ImageProxy imageProxy : mOwnedImageProxies) {
                imageProxy.close();
            }
            mOwnedImageProxies.clear();
            mFutureResults.clear();
            mCompleters.clear();
            setup();
        }
    }

    private void setup() {
        synchronized (mLock) {
            for (final int captureId : mCaptureIdList) {
                ListenableFuture<ImageProxy> futureResult = CallbackToFutureAdapter.getFuture(
                        new CallbackToFutureAdapter.Resolver<ImageProxy>() {
                            @Override
                            public Object attachCompleter(
                                    @NonNull CallbackToFutureAdapter.Completer<ImageProxy>
                                            completer) {
                                synchronized (mLock) { // Not technically needed since
                                    // attachCompleter is called inline, but mLock is re-entrant
                                    // so there's no harm.
                                    mCompleters.put(captureId, completer);
                                }
                                return "getImageProxy(id: " + captureId + ")";
                            }
                        });
                mFutureResults.put(captureId, futureResult);
            }
        }
    }
}
