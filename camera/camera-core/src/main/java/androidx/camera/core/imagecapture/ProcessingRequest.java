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

package androidx.camera.core.imagecapture;

import static java.util.Objects.requireNonNull;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.CaptureBundle;
import androidx.camera.core.impl.CaptureStage;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

/**
 * A post-processing request and its callback.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class ProcessingRequest {
    private final int mRequestId;
    @Nullable
    private final ImageCapture.OutputFileOptions mOutputFileOptions;
    @NonNull
    private final Rect mCropRect;
    private final int mRotationDegrees;
    private final int mJpegQuality;
    @NonNull
    private final Matrix mSensorToBufferTransform;
    @NonNull
    private final TakePictureCallback mCallback;
    @NonNull
    private final String mTagBundleKey;
    @NonNull
    private final List<Integer> mStageIds;

    @NonNull final ListenableFuture<Void> mCaptureFuture;

    ProcessingRequest(
            @NonNull CaptureBundle captureBundle,
            @Nullable ImageCapture.OutputFileOptions outputFileOptions,
            @NonNull Rect cropRect,
            int rotationDegrees,
            int jpegQuality,
            @NonNull Matrix sensorToBufferTransform,
            @NonNull TakePictureCallback callback,
            @NonNull ListenableFuture<Void> captureFuture) {
        this(captureBundle, outputFileOptions, cropRect, rotationDegrees, jpegQuality,
                sensorToBufferTransform, callback, captureFuture, 0);
    }
    ProcessingRequest(
            @NonNull CaptureBundle captureBundle,
            @Nullable ImageCapture.OutputFileOptions outputFileOptions,
            @NonNull Rect cropRect,
            int rotationDegrees,
            int jpegQuality,
            @NonNull Matrix sensorToBufferTransform,
            @NonNull TakePictureCallback callback,
            @NonNull ListenableFuture<Void> captureFuture,
            int requestId) {
        mRequestId = requestId;
        mOutputFileOptions = outputFileOptions;
        mJpegQuality = jpegQuality;
        mRotationDegrees = rotationDegrees;
        mCropRect = cropRect;
        mSensorToBufferTransform = sensorToBufferTransform;
        mCallback = callback;
        mTagBundleKey = String.valueOf(captureBundle.hashCode());
        mStageIds = new ArrayList<>();
        for (CaptureStage captureStage : requireNonNull(captureBundle.getCaptureStages())) {
            mStageIds.add(captureStage.getId());
        }
        mCaptureFuture = captureFuture;
    }

    @NonNull
    String getTagBundleKey() {
        return mTagBundleKey;
    }

    @NonNull
    List<Integer> getStageIds() {
        return mStageIds;
    }

    public int getRequestId() {
        return mRequestId;
    }

    @Nullable
    ImageCapture.OutputFileOptions getOutputFileOptions() {
        return mOutputFileOptions;
    }

    @NonNull
    Rect getCropRect() {
        return mCropRect;
    }

    int getRotationDegrees() {
        return mRotationDegrees;
    }

    int getJpegQuality() {
        return mJpegQuality;
    }

    @NonNull
    Matrix getSensorToBufferTransform() {
        return mSensorToBufferTransform;
    }

    boolean isInMemoryCapture() {
        return getOutputFileOptions() == null;
    }

    /**
     * @see TakePictureCallback#onCaptureStarted()
     */
    @MainThread
    void onCaptureStarted() {
        mCallback.onCaptureStarted();
    }

    @MainThread
    void onCaptureProcessProgressed(int progress) {
        mCallback.onCaptureProcessProgressed(progress);
    }

    /**
     * @see TakePictureCallback#onImageCaptured()
     */
    @MainThread
    void onImageCaptured() {
        mCallback.onImageCaptured();
    }

    /**
     * @see TakePictureCallback#onFinalResult
     */
    @MainThread
    void onFinalResult(@NonNull ImageCapture.OutputFileResults outputFileResults) {
        mCallback.onFinalResult(outputFileResults);
    }

    void onPostviewBitmapAvailable(@NonNull Bitmap bitmap) {
        mCallback.onPostviewBitmapAvailable(bitmap);
    }

    /**
     * @see TakePictureCallback#onFinalResult
     */
    @MainThread
    void onFinalResult(@NonNull ImageProxy imageProxy) {
        mCallback.onFinalResult(imageProxy);
    }

    /**
     * @see TakePictureCallback#onProcessFailure
     */
    @MainThread
    void onProcessFailure(@NonNull ImageCaptureException imageCaptureException) {
        mCallback.onProcessFailure(imageCaptureException);
    }

    /**
     * @see TakePictureCallback#onCaptureFailure
     */
    @MainThread
    void onCaptureFailure(@NonNull ImageCaptureException imageCaptureException) {
        mCallback.onCaptureFailure(imageCaptureException);
    }

    /**
     * Returns true if the request has been aborted by the app/lifecycle.
     */
    boolean isAborted() {
        return mCallback.isAborted();
    }

    @NonNull
    ListenableFuture<Void> getCaptureFuture() {
        return mCaptureFuture;
    }
}
