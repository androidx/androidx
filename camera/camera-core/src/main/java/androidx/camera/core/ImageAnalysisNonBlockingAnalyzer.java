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

import android.os.Handler;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
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
    // Timestamp of the last image finished being processed by user callback thread.
    private final AtomicLong mFinishedImageTimestamp;

    ImageAnalysisNonBlockingAnalyzer(AtomicReference<ImageAnalysis.Analyzer> subscribedAnalyzer,
            AtomicInteger relativeRotation, Handler userHandler, Executor executor) {
        super(subscribedAnalyzer, relativeRotation, userHandler);
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
            return;
        }
        long postedImageTimestamp = mPostedImageTimestamp.get();
        long finishedImageTimestamp = mFinishedImageTimestamp.get();

        if (imageProxy.getTimestamp() <= postedImageTimestamp) {
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

        mPostedImageTimestamp.set(imageProxy.getTimestamp());
        try {
            mUserHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        analyzeImage(imageProxy);
                    } finally {
                        finishImage(imageProxy);
                        mBackgroundExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                analyzeCachedImage();
                            }
                        });
                    }
                }
            });
        } catch (RuntimeException e) {
            // Unblock if fails to post to user thread.
            Log.e(TAG, "Error calling user callback", e);
            finishImage(imageProxy);
        }
    }

    synchronized void finishImage(ImageProxy imageProxy) {
        if (isClosed()) {
            return;
        }
        mFinishedImageTimestamp.set(imageProxy.getTimestamp());
        imageProxy.close();
    }
}
