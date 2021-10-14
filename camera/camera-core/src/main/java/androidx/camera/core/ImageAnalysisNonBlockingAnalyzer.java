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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;

/**
 * OnImageAvailableListener with non-blocking behavior. Analyzes images in a non-blocking way by
 * dropping images when analyzer is busy.
 *
 * <p> Used with {@link ImageAnalysis}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class ImageAnalysisNonBlockingAnalyzer extends ImageAnalysisAbstractAnalyzer {

    // The executor for managing cached image.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Executor mBackgroundExecutor;

    private final Object mLock = new Object();

    // The cached image when analyzer is busy. Image removed from cache must be closed by 1) closing
    // it directly or 2) re-posting it to close it eventually.
    @GuardedBy("mLock")
    @Nullable
    @VisibleForTesting
    ImageProxy mCachedImage;

    // The latest unclosed image sent to the app.
    @GuardedBy("mLock")
    @Nullable
    private CacheAnalyzingImageProxy mPostedImage;

    ImageAnalysisNonBlockingAnalyzer(Executor executor) {
        mBackgroundExecutor = executor;
    }

    @Nullable
    @Override
    ImageProxy acquireImage(@NonNull ImageReaderProxy imageReaderProxy) {
        // Use acquireLatestImage() so older images should be released.
        return imageReaderProxy.acquireLatestImage();
    }

    /**
     * This method guarantees closing the image by either 1) closing the image in the current
     * thread, 2) caching it for later or 3) posting it to user Thread to close it.
     *
     * @param imageProxy the incoming image frame.
     */
    @Override
    void onValidImageAvailable(@NonNull ImageProxy imageProxy) {
        synchronized (mLock) {
            if (!mIsAttached) {
                imageProxy.close();
                return;
            }
            if (mPostedImage != null) {
                // There is unclosed image held by the app. The incoming image has to wait.

                if (imageProxy.getImageInfo().getTimestamp()
                        <= mPostedImage.getImageInfo().getTimestamp()) {
                    // Discard the incoming image that is in the wrong order. Cached image can be
                    // in this state.
                    imageProxy.close();
                } else {
                    // Otherwise cache the incoming image and repost it later.
                    if (mCachedImage != null) {
                        mCachedImage.close();
                    }
                    mCachedImage = imageProxy;
                }
                return;
            }

            // Post the incoming image to app.
            final CacheAnalyzingImageProxy newPostedImage = new CacheAnalyzingImageProxy(imageProxy,
                    this);
            mPostedImage = newPostedImage;
            Futures.addCallback(analyzeImage(newPostedImage), new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // No-op. If the post is successful, app should close it.
                }

                @Override
                public void onFailure(Throwable t) {
                    // Close the image if we didn't post it to user.
                    newPostedImage.close();
                }
            }, CameraXExecutors.directExecutor());
        }
    }

    @Override
    void clearCache() {
        synchronized (mLock) {
            if (mCachedImage != null) {
                mCachedImage.close();
                mCachedImage = null;
            }
        }
    }

    /**
     * Removes cached image from cache and analyze it.
     */
    void analyzeCachedImage() {
        synchronized (mLock) {
            mPostedImage = null;
            if (mCachedImage != null) {
                ImageProxy cachedImage = mCachedImage;
                mCachedImage = null;
                onValidImageAvailable(cachedImage);
            }
        }
    }

    /**
     * An {@link ImageProxy} that analyze cached image on close.
     */
    static class CacheAnalyzingImageProxy extends ForwardingImageProxy {

        // WeakReference so that if the app holds onto the ImageProxy instance the analyzer can
        // still be GCed.
        final WeakReference<ImageAnalysisNonBlockingAnalyzer> mNonBlockingAnalyzerWeakReference;

        /**
         * Creates a new instance which wraps the given image.
         *
         * @param image  image proxy to wrap.
         * @param nonBlockingAnalyzer instance of the nonblocking analyzer.
         */
        CacheAnalyzingImageProxy(@NonNull ImageProxy image,
                @NonNull ImageAnalysisNonBlockingAnalyzer nonBlockingAnalyzer) {
            super(image);
            mNonBlockingAnalyzerWeakReference = new WeakReference<>(nonBlockingAnalyzer);

            addOnImageCloseListener((imageProxy) -> {
                ImageAnalysisNonBlockingAnalyzer analyzer = mNonBlockingAnalyzerWeakReference.get();
                if (analyzer != null) {
                    analyzer.mBackgroundExecutor.execute(
                            () -> analyzer.analyzeCachedImage());
                }
            });
        }
    }
}
