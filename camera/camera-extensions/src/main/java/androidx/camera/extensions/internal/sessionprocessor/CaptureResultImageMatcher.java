/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.extensions.internal.sessionprocessor;

import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.util.LongSparseArray;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * To match {@link ImageReference} with {@link TotalCaptureResult} by timestamp.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class CaptureResultImageMatcher {
    private final Object mLock = new Object();
    private static final int INVALID_TIMESTAMP = -1;
    /** TotalCaptureResults that haven't been matched with Image. */
    @GuardedBy("mLock")
    private final LongSparseArray<TotalCaptureResult> mPendingCaptureResults =
            new LongSparseArray<>();

    /** To store the capture stage ids for each TotalCaptureResult */
    @GuardedBy("mLock")
    Map<TotalCaptureResult, Integer> mCaptureStageIdMap = new HashMap<>();

    /** Images that haven't been matched with timestamp. */
    @GuardedBy("mLock")
    private final LongSparseArray<ImageReference> mPendingImages = new LongSparseArray<>();

    @GuardedBy("mLock")
    ImageReferenceListener mImageReferenceListener;

    CaptureResultImageMatcher() {
    }

    void clear() {
        synchronized (mLock) {
            mPendingCaptureResults.clear();
            for (int i = 0; i < mPendingImages.size(); i++) {
                long key = mPendingImages.keyAt(i);
                mPendingImages.get(key).decrement();
            }
            mPendingImages.clear();
            mCaptureStageIdMap.clear();
        }
    }

    void setImageReferenceListener(
            @NonNull ImageReferenceListener imageReferenceListener) {
        synchronized (mLock) {
            mImageReferenceListener = imageReferenceListener;
        }
    }

    void clearImageReferenceListener() {
        synchronized (mLock) {
            mImageReferenceListener = null;
        }
    }

    void imageIncoming(@NonNull ImageReference imageReference) {
        synchronized (mLock) {
            Image image = imageReference.get();
            mPendingImages.put(image.getTimestamp(), imageReference);
        }
        matchImages();
    }

    void captureResultIncoming(@NonNull TotalCaptureResult captureResult) {
        captureResultIncoming(captureResult, 0);
    }

    void captureResultIncoming(@NonNull TotalCaptureResult captureResult,
            int captureStageId) {
        synchronized (mLock) {
            long timestamp = getTimeStampFromCaptureResult(captureResult);
            if (timestamp == INVALID_TIMESTAMP) {
                return;
            }
            // Add the incoming CameraCaptureResult to pending list and do the matching logic.
            mPendingCaptureResults.put(timestamp, captureResult);
            mCaptureStageIdMap.put(captureResult, captureStageId);
        }
        matchImages();
    }

    private long getTimeStampFromCaptureResult(TotalCaptureResult captureResult) {
        Long timestamp = captureResult.get(CaptureResult.SENSOR_TIMESTAMP);
        long timestampValue = INVALID_TIMESTAMP;
        if (timestamp != null) {
            timestampValue = timestamp;
        }

        return timestampValue;
    }


    private void notifyImage(ImageReference imageReference,
            TotalCaptureResult totalCaptureResult) {
        ImageReferenceListener listenerToInvoke = null;
        Integer captureStageId = null;
        synchronized (mLock) {
            if (mImageReferenceListener != null) {
                listenerToInvoke = mImageReferenceListener;
                captureStageId = mCaptureStageIdMap.get(totalCaptureResult);
            } else {
                imageReference.decrement();
            }
        }

        if (listenerToInvoke != null) {
            listenerToInvoke.onImageReferenceIncoming(imageReference,
                    totalCaptureResult, captureStageId);
        }
    }

    // Remove the stale {@link ImageReference} from the pending queue if there
    // are any missing which can happen if the camera is momentarily shut off.
    // The ImageReference timestamps are assumed to be monotonically increasing. This
    // means any ImageReference which has a timestamp older (smaller in value) than the
    // oldest timestamp in the other queue will never get matched, so they should be removed.
    //
    // This should only be called at the end of matchImages(). The assumption is that there are no
    // matching timestamps.
    private void removeStaleData() {
        synchronized (mLock) {
            // No stale data to remove
            if (mPendingImages.size() == 0 || mPendingCaptureResults.size() == 0) {
                return;
            }

            Long minImageRefTimestamp = mPendingImages.keyAt(0);
            Long minCaptureResultTimestamp = mPendingCaptureResults.keyAt(0);

            // If timestamps are equal then matchImages did not correctly match up the capture
            // result and Image
            Preconditions.checkArgument(!minCaptureResultTimestamp.equals(minImageRefTimestamp));

            if (minCaptureResultTimestamp > minImageRefTimestamp) {
                for (int i = mPendingImages.size() - 1; i >= 0; i--) {
                    if (mPendingImages.keyAt(i) < minCaptureResultTimestamp) {
                        ImageReference imageReference = mPendingImages.valueAt(i);
                        imageReference.decrement();
                        mPendingImages.removeAt(i);
                    }
                }
            } else {
                for (int i = mPendingCaptureResults.size() - 1; i >= 0; i--) {
                    if (mPendingCaptureResults.keyAt(i) < minImageRefTimestamp) {
                        mPendingCaptureResults.removeAt(i);
                    }
                }
            }
        }
    }

    private void matchImages() {
        ImageReference imageToNotify = null;
        TotalCaptureResult resultToNotify = null;
        synchronized (mLock) {
            // Iterate in reverse order so that capture result can be removed in place
            for (int i = mPendingCaptureResults.size() - 1; i >= 0; i--) {
                TotalCaptureResult captureResult = mPendingCaptureResults.valueAt(i);
                long timestamp = getTimeStampFromCaptureResult(captureResult);

                ImageReference imageReference = mPendingImages.get(timestamp);

                if (imageReference != null) {
                    mPendingImages.remove(timestamp);
                    mPendingCaptureResults.removeAt(i);
                    imageToNotify = imageReference;
                    resultToNotify = captureResult;
                }
            }
            removeStaleData();
        }

        if (imageToNotify != null && resultToNotify != null) {
            notifyImage(imageToNotify, resultToNotify);
        }
    }

    interface ImageReferenceListener {
        void onImageReferenceIncoming(@NonNull ImageReference imageReference,
                @NonNull TotalCaptureResult totalCaptureResult, int captureStageId);
    }
}
