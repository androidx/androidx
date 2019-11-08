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

import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

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
    // Timestamp of the last image finished being processed by user callback thread.
    private final AtomicLong mFinishedImageTimestamp;

    ImageAnalysisNonBlockingAnalyzer(Executor executor) {
        mBackgroundExecutor = executor;
        mPostedImageTimestamp = new AtomicLong();
        mFinishedImageTimestamp = new AtomicLong();
        open();
    }

    @Override
    public void onImageAvailable(ImageReaderProxy imageReaderProxy) {
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
        mFinishedImageTimestamp.set(mPostedImageTimestamp.get());
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
        long postedImageTimestamp = mPostedImageTimestamp.get();
        long finishedImageTimestamp = mFinishedImageTimestamp.get();

        if (imageProxy.getImageInfo().getTimestamp() <= postedImageTimestamp) {
            // Discard image that is in wrong order. Reposted cached image can be in this state.
            imageProxy.close();
            return;
        }

        if (postedImageTimestamp > finishedImageTimestamp) {
            // If analyzer is busy, cache the new image.
            if (mCachedImage != null) {
                mCachedImage.close();
            }
            mCachedImage = imageProxy;
            return;
        }

        mPostedImageTimestamp.set(imageProxy.getImageInfo().getTimestamp());

        ListenableFuture<Void> analyzeFuture = analyzeImage(imageProxy);

        // Callback to close the image only after analysis complete regardless of success
        Futures.addCallback(analyzeFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                finishImage(imageProxy);

                mBackgroundExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        analyzeCachedImage();
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                finishImage(imageProxy);
            }
        }, CameraXExecutors.directExecutor());
    }

    // Finish processing image for handling dropping behavior
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    synchronized void finishImage(ImageProxy imageProxy) {
        try {
            mFinishedImageTimestamp.set(imageProxy.getImageInfo().getTimestamp());
            imageProxy.close();
        } catch (IllegalStateException e) {
            Log.d(TAG, "Image already closed");
        }
    }
}
