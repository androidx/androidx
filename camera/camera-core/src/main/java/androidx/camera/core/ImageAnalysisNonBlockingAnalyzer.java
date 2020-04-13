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
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OnImageAvailableListener with non-blocking behavior. Analyzes images in a non-blocking way by
 * dropping images when analyzer is busy.
 *
 * <p> Used with {@link ImageAnalysis}.
 */
final class ImageAnalysisNonBlockingAnalyzer extends ImageAnalysisAbstractAnalyzer {

    private static final String TAG = "NonBlockingCallback";

    // The executor for managing cached image.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Executor mBackgroundExecutor;

    // The cached image when analyzer is busy. Image removed from cache must be closed by 1) closing
    // it directly or 2) re-posting it to close it eventually.
    @GuardedBy("this")
    private ImageProxy mCachedImage;

    // Timestamp of the last image posted to user callback thread.
    private final AtomicLong mPostedImageTimestamp;

    private final AtomicReference<CacheAnalyzingImageProxy> mPostedImage;

    ImageAnalysisNonBlockingAnalyzer(Executor executor) {
        mBackgroundExecutor = executor;
        mPostedImage = new AtomicReference<>();
        mPostedImageTimestamp = new AtomicLong();
        open();
    }

    @Override
    public void onImageAvailable(@NonNull ImageReaderProxy imageReaderProxy) {
        ImageProxy imageProxy = imageReaderProxy.acquireLatestImage();
        if (imageProxy == null) {
            return;
        }
        analyze(imageProxy);
    }

    @Override
    synchronized void open() {
        super.open();
        mCachedImage = null;
        mPostedImageTimestamp.set(-1);
        mPostedImage.set(null);
    }

    @Override
    synchronized void close() {
        super.close();
        if (mCachedImage != null) {
            mCachedImage.close();
            mCachedImage = null;
        }
    }

    /**
     * Removes cached image from cache and analyze it.
     */
    synchronized void analyzeCachedImage() {
        if (mCachedImage != null) {
            ImageProxy cachedImage = mCachedImage;
            mCachedImage = null;
            analyze(cachedImage);
        }
    }

    /**
     * This method guarantees closing the image by either 1) closing the image in the current
     * thread, 2) caching it for later or 3) posting it to user Thread to close it.
     *
     * @param imageProxy the incoming image frame.
     */
    private synchronized void analyze(@NonNull ImageProxy imageProxy) {
        if (isClosed()) {
            imageProxy.close();
            return;
        }

        CacheAnalyzingImageProxy postedImage = mPostedImage.get();
        if (postedImage != null
                && imageProxy.getImageInfo().getTimestamp() <= mPostedImageTimestamp.get()) {
            // Discard image that is in wrong order. Reposted cached image can be in this state.
            imageProxy.close();
            return;
        }

        if (postedImage != null && !postedImage.isClosed()) {
            // If the posted image hasn't been closed, cache the new image.
            if (mCachedImage != null) {
                mCachedImage.close();
            }
            mCachedImage = imageProxy;
            return;
        }

        final CacheAnalyzingImageProxy newPostedImage = new CacheAnalyzingImageProxy(imageProxy,
                this);
        mPostedImage.set(newPostedImage);
        mPostedImageTimestamp.set(newPostedImage.getImageInfo().getTimestamp());

        ListenableFuture<Void> analyzeFuture = analyzeImage(newPostedImage);

        // Callback to close the image only after analysis complete regardless of success
        Futures.addCallback(analyzeFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // No-op. Keep dropping the images until user closes the current one.
            }

            @Override
            public void onFailure(Throwable t) {
                // Close the image if we didn't post it to user.
                newPostedImage.close();
            }
        }, CameraXExecutors.directExecutor());
    }

    /**
     * An {@link ImageProxy} which will trigger analysis of the cached ImageProxy if it exists.
     */
    static class CacheAnalyzingImageProxy extends ForwardingImageProxy {

        // So that if the user holds onto the ImageProxy instance the analyzer can still be GC'ed
        WeakReference<ImageAnalysisNonBlockingAnalyzer> mNonBlockingAnalyzerWeakReference;

        private boolean mClosed = false;

        /**
         * Creates a new instance which wraps the given image.
         *
         * @param image               to wrap
         * @param nonBlockingAnalyzer instance of the nonblocking analyzer
         */
        CacheAnalyzingImageProxy(ImageProxy image,
                ImageAnalysisNonBlockingAnalyzer nonBlockingAnalyzer) {
            super(image);
            mNonBlockingAnalyzerWeakReference = new WeakReference<>(nonBlockingAnalyzer);

            addOnImageCloseListener((imageProxy) -> {
                mClosed = true;
                ImageAnalysisNonBlockingAnalyzer analyzer = mNonBlockingAnalyzerWeakReference.get();
                if (analyzer != null) {
                    analyzer.mBackgroundExecutor.execute(analyzer::analyzeCachedImage);
                }
            });
        }

        boolean isClosed() {
            return mClosed;
        }
    }
}
