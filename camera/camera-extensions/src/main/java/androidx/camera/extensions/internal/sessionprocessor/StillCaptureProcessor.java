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

import android.graphics.ImageFormat;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.OutputSurface;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.extensions.impl.CaptureProcessorImpl;
import androidx.camera.extensions.impl.ProcessResultImpl;
import androidx.camera.extensions.internal.ClientVersion;
import androidx.camera.extensions.internal.ExtensionVersion;
import androidx.camera.extensions.internal.Version;
import androidx.camera.extensions.internal.compat.workaround.CaptureOutputSurfaceForCaptureProcessor;
import androidx.core.util.Preconditions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A processor that is responsible for invoking OEM's CaptureProcessorImpl and converting the
 * processed YUV images into JPEG images. The JPEG images are written to the given output surface.
 *
 * <p> It only outputs JPEG format images, meaning that the given output surface must be of JPEG
 * format. To use this processor, follow the steps below:
 * <pre>
 * 1. Invoke start() with the required capture stage id list. A listener is given to
 *     be invoked when the processing is done and the resulting image is ready to appear in the
 *     output surface.
 * 2. When input images from camera and TotalCaptureResult arrives, notify the processor by
 *   {@link #notifyImage(ImageReference)} and {@link #notifyCaptureResult(TotalCaptureResult, int)}.
 * 3. When camera session is finishing, invoke {@link #close()} to reclaim the resources.
 *    Please note that the output YUV surface should be closed AFTER this processor is closed().
 * </pre>
 */
class StillCaptureProcessor {
    private static final String TAG = "StillCaptureProcessor";
    private static final long UNSPECIFIED_TIMESTAMP = -1;
    @NonNull
    final CaptureProcessorImpl mCaptureProcessorImpl;
    @NonNull
    final CaptureResultImageMatcher mCaptureResultImageMatcher = new CaptureResultImageMatcher();
    private boolean mIsPostviewConfigured;
    final Object mLock = new Object();
    @GuardedBy("mLock")
    @NonNull
    HashMap<Integer, Pair<ImageReference, TotalCaptureResult>> mCaptureResults =
            new HashMap<>();

    @GuardedBy("mLock")
    OnCaptureResultCallback mOnCaptureResultCallback = null;
    // Stores the first capture result for injecting into the output JPEG ImageProxy.
    @GuardedBy("mLock")
    TotalCaptureResult mSourceCaptureResult = null;
    CaptureOutputSurfaceForCaptureProcessor mCaptureOutputSurface;
    @GuardedBy("mLock")
    boolean mIsClosed = false;
    long mTimeStampForOutputImage = UNSPECIFIED_TIMESTAMP;

    StillCaptureProcessor(@NonNull CaptureProcessorImpl captureProcessorImpl,
            @NonNull Surface outputSurface,
            @NonNull Size surfaceSize,
            @Nullable OutputSurface postviewOutputSurface,
            boolean needOverrideTimestamp) {
        mCaptureProcessorImpl = captureProcessorImpl;

        mCaptureOutputSurface = new CaptureOutputSurfaceForCaptureProcessor(
                outputSurface, surfaceSize, needOverrideTimestamp);

        mCaptureProcessorImpl.onOutputSurface(
                mCaptureOutputSurface.getSurface(), ImageFormat.YUV_420_888);
        mCaptureProcessorImpl.onImageFormatUpdate(ImageFormat.YUV_420_888);

        mIsPostviewConfigured = (postviewOutputSurface != null);
        if (postviewOutputSurface != null
                && ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)) {
            Preconditions.checkArgument(
                    postviewOutputSurface.getImageFormat() == ImageFormat.YUV_420_888);
            mCaptureProcessorImpl.onResolutionUpdate(surfaceSize, postviewOutputSurface.getSize());
            mCaptureProcessorImpl.onPostviewOutputSurface(postviewOutputSurface.getSurface());

        } else {
            mCaptureProcessorImpl.onResolutionUpdate(surfaceSize);
        }
    }

    interface OnCaptureResultCallback {
        void onProcessCompleted();

        void onCaptureCompleted(long shutterTimestamp,
                @NonNull List<Pair<CaptureResult.Key, Object>> result);

        void onError(@NonNull Exception e);

        void onCaptureProcessProgressed(int progress);
    }

    void clearCaptureResults() {
        synchronized (mLock) {
            for (Pair<ImageReference, TotalCaptureResult> value :
                    mCaptureResults.values()) {
                value.first.decrement();
            }
            mCaptureResults.clear();
        }
    }

    void startCapture(boolean enablePostview, @NonNull List<Integer> captureIdList,
            @NonNull OnCaptureResultCallback onCaptureResultCallback) {
        Logger.d(TAG, "Start the capture: enablePostview=" + enablePostview);
        mTimeStampForOutputImage = UNSPECIFIED_TIMESTAMP;
        synchronized (mLock) {
            Preconditions.checkState(!mIsClosed, "StillCaptureProcessor is closed. Can't invoke "
                    + "startCapture()");
            mOnCaptureResultCallback = onCaptureResultCallback;
            clearCaptureResults();
        }

        mCaptureResultImageMatcher.clear();
        mCaptureResultImageMatcher.setImageReferenceListener(
                (imageReference, totalCaptureResult, captureStageId) -> {
                    synchronized (mLock) {
                        if (mIsClosed) {
                            imageReference.decrement();
                            Logger.d(TAG, "Ignore image in closed state");
                            return;
                        }
                        Logger.d(TAG,
                                "onImageReferenceIncoming  captureStageId=" + captureStageId);

                        mCaptureResults.put(captureStageId, new Pair<>(imageReference,
                                totalCaptureResult));

                        Logger.d(TAG, "mCaptureResult has capture stage Id: "
                                + mCaptureResults.keySet());
                        if (mCaptureResults.keySet().containsAll(captureIdList)) {
                            process(mCaptureResults, onCaptureResultCallback, enablePostview);
                        }
                    }
                });
    }

    void process(@NonNull Map<Integer, Pair<ImageReference, TotalCaptureResult>> results,
            @NonNull OnCaptureResultCallback onCaptureResultCallback,
            boolean enablePostview) {
        HashMap<Integer, Pair<Image, TotalCaptureResult>> convertedResult =
                new HashMap<>();
        synchronized (mLock) {
            for (Integer id : results.keySet()) {
                Pair<ImageReference, TotalCaptureResult> pair =
                        results.get(id);
                convertedResult.put(id,
                        new Pair<>(pair.first.get(), pair.second));
            }
        }

        // Run OEM's process in another thread to avoid blocking the camera thread.
        CameraXExecutors.ioExecutor().execute(() -> {
            synchronized (mLock) {
                try {
                    if (mIsClosed) {
                        Logger.d(TAG, "Ignore process() in closed state.");
                        return;
                    }
                    Logger.d(TAG, "CaptureProcessorImpl.process() begin");
                    if (ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                            && ClientVersion.isMinimumCompatibleVersion(
                            Version.VERSION_1_4)
                            && enablePostview && mIsPostviewConfigured) {
                        mCaptureProcessorImpl.processWithPostview(convertedResult,
                                new ProcessResultImpl() {
                                    @Override
                                    public void onCaptureCompleted(
                                            long shutterTimestamp,
                                            @NonNull List<Pair<CaptureResult.Key,
                                                    Object>> result) {
                                        onCaptureResultCallback.onCaptureCompleted(
                                                shutterTimestamp, result);
                                    }

                                    @Override
                                    public void onCaptureProcessProgressed(
                                            int progress) {
                                        onCaptureResultCallback
                                                .onCaptureProcessProgressed(
                                                        progress);
                                    }

                                }, CameraXExecutors.directExecutor());
                    } else if (ExtensionVersion.isMinimumCompatibleVersion(
                            Version.VERSION_1_3)
                            && ClientVersion.isMinimumCompatibleVersion(
                            Version.VERSION_1_3)) {
                        mCaptureProcessorImpl.process(convertedResult,
                                new ProcessResultImpl() {
                                    @Override
                                    public void onCaptureCompleted(
                                            long shutterTimestamp,
                                            @NonNull List<Pair<CaptureResult.Key,
                                                    Object>> result) {
                                        onCaptureResultCallback.onCaptureCompleted(
                                                shutterTimestamp, result);
                                    }

                                    @Override
                                    public void onCaptureProcessProgressed(
                                            int progress) {
                                        onCaptureResultCallback
                                                .onCaptureProcessProgressed(progress);
                                    }
                                }, CameraXExecutors.directExecutor());
                    } else {
                        mCaptureProcessorImpl.process(convertedResult);
                    }
                } catch (Exception e) {
                    Logger.e(TAG, "mCaptureProcessorImpl.process exception ", e);
                    mOnCaptureResultCallback = null;
                    if (onCaptureResultCallback != null) {
                        onCaptureResultCallback.onError(e);
                    }
                } finally {
                    Logger.d(TAG, "CaptureProcessorImpl.process() finish");
                    if (mOnCaptureResultCallback != null) {
                        mOnCaptureResultCallback.onProcessCompleted();
                        mOnCaptureResultCallback = null;
                    }
                    clearCaptureResults();
                }
            }
        });
    }

    void notifyCaptureResult(@NonNull TotalCaptureResult captureResult,
            int captureStageId) {
        mCaptureResultImageMatcher.captureResultIncoming(captureResult,
                captureStageId);
        // Fetch the timestamp for the 1st captureResult received.
        if (mTimeStampForOutputImage == UNSPECIFIED_TIMESTAMP) {
            Long timestamp = captureResult.get(CaptureResult.SENSOR_TIMESTAMP);
            if (timestamp != null) {
                mTimeStampForOutputImage = timestamp;
                mCaptureOutputSurface.setOutputImageTimestamp(mTimeStampForOutputImage);
            }
        }

        synchronized (mLock) {
            if (mSourceCaptureResult == null) {
                mSourceCaptureResult = captureResult;
            }
        }
    }

    void notifyImage(@NonNull ImageReference imageReference) {
        mCaptureResultImageMatcher.imageIncoming(imageReference);
    }

    /**
     * Close the processor. Please note that captureOutputSurface passed in must be closed AFTER
     * invoking this function.
     */
    void close() {
        synchronized (mLock) {
            Logger.d(TAG, "Close the StillCaptureProcessor");
            mIsClosed = true;
            clearCaptureResults();
            mCaptureResultImageMatcher.clearImageReferenceListener();
            mCaptureResultImageMatcher.clear();
            mCaptureOutputSurface.close();
        }
    }
}
