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

import static android.graphics.ImageFormat.JPEG;
import static android.graphics.ImageFormat.RAW_SENSOR;

import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.core.util.Preconditions.checkArgument;

import static java.util.Objects.requireNonNull;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;

import androidx.annotation.IntRange;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.internal.compat.workaround.CaptureFailedRetryEnabler;

import com.google.auto.value.AutoValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * A {@link ImageCapture#takePicture} request.
 *
 * <p> It contains app provided data and a snapshot of {@link ImageCapture} properties.
 */
@SuppressWarnings("AutoValueImmutableFields")
@AutoValue
public abstract class TakePictureRequest {

    private static final String TAG = "TakePictureRequest";

    /**
     * By default, ImageCapture does not retry requests. For some problematic devices, the
     * capture request can become success after retrying. The allowed retry count will be
     * provided by the {@link CaptureFailedRetryEnabler}.
     */
    private int mRemainingRetires = new CaptureFailedRetryEnabler().getRetryCount();

    /**
     * Map to track image capture status for simultaneous capture.
     *
     * <p>For RAW + JPEG, we will only have two formats
     * {@link android.graphics.ImageFormat#RAW_SENSOR} and {@link android.graphics.ImageFormat#JPEG}
     * but it could extend to other formats in the future.
     *
     * It is not thread-safe.
     */
    private final Map<Integer, Boolean> mFormatCaptureStatus = new HashMap<>();

    /**
     * Gets the callback {@link Executor} provided by the app.
     */
    @NonNull
    abstract Executor getAppExecutor();

    /**
     * Gets the app provided callback for in-memory capture.
     */
    @Nullable
    public abstract ImageCapture.OnImageCapturedCallback getInMemoryCallback();

    /**
     * Gets the app provided callback for on-disk capture.
     */
    @Nullable
    public abstract ImageCapture.OnImageSavedCallback getOnDiskCallback();

    /**
     * Gets the app provided options for on-disk capture.
     */
    @Nullable
    abstract List<ImageCapture.OutputFileOptions> getOutputFileOptions();

    /**
     * A snapshot of {@link ImageCapture#getViewPortCropRect()} when
     * {@link ImageCapture#takePicture} is called.
     */
    @NonNull
    abstract Rect getCropRect();

    /**
     * A snapshot of {@link ImageCapture#getSensorToBufferTransformMatrix()} when
     * {@link ImageCapture#takePicture} is called.
     */
    @NonNull
    abstract Matrix getSensorToBufferTransform();

    /**
     * A snapshot of rotation degrees when {@link ImageCapture#takePicture} is called.
     */
    @ImageOutputConfig.RotationValue
    abstract int getRotationDegrees();

    /**
     * A snapshot of {@link ImageCaptureConfig#getJpegQuality()} when
     * {@link ImageCapture#takePicture} is called.
     */
    @IntRange(from = 1, to = 100)
    abstract int getJpegQuality();

    /**
     * Gets the capture mode of the request.
     *
     * <p>When there are software JPEG encoding/decoding, the value of {@link #getJpegQuality()}
     * is used for the software encoding. The capture mode value is for calculating the JPEG
     * quality for camera hardware encoding.
     */
    @ImageCapture.CaptureMode
    abstract int getCaptureMode();

    /**
     * Checks if the request is for simultaneous capture. Currently only RAW + JPEG are supported.
     */
    abstract boolean isSimultaneousCapture();

    /**
     * Gets the {@link CameraCaptureCallback}s set on the {@link SessionConfig}.
     *
     * <p>This is for calling back to Camera2InterOp. See: aosp/947197.
     */
    @NonNull
    abstract List<CameraCaptureCallback> getSessionConfigCameraCaptureCallbacks();

    /**
     * Decrements retry counter.
     *
     * @return true if there is still remaining retries at the time of calling. In that case, the
     * request should be retried. False when there is no retry left. The caller needs to fail the
     * request.
     */
    @MainThread
    boolean decrementRetryCounter() {
        checkMainThread();
        if (mRemainingRetires > 0) {
            mRemainingRetires--;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Increments retry counter.
     */
    @MainThread
    void incrementRetryCounter() {
        checkMainThread();
        mRemainingRetires++;
    }

    /**
     * Gets the current retry count for testing.
     */
    @MainThread
    @VisibleForTesting
    int getRemainingRetries() {
        checkMainThread();
        return mRemainingRetires;
    }

    void initFormatProcessStatusInSimultaneousCapture() {
        mFormatCaptureStatus.put(RAW_SENSOR, false);
        mFormatCaptureStatus.put(JPEG, false);
    }

    /**
     * Marks the format as processed in simultaneous capture.
     */
    void markFormatProcessStatusInSimultaneousCapture(int format, boolean isProcessed) {
        if (!mFormatCaptureStatus.containsKey(format)) {
            Logger.e(TAG, "The format is not supported in simultaneous capture");
            return;
        }
        mFormatCaptureStatus.put(format, isProcessed);
    }

    /**
     * Checks if all the formats are processed in simultaneous capture.
     */
    boolean isFormatProcessedInSimultaneousCapture() {
        boolean isProcessed = true;
        for (Map.Entry<Integer, Boolean> entry : mFormatCaptureStatus.entrySet()) {
            if (!entry.getValue()) {
                isProcessed = false;
                break;
            }
        }
        return isProcessed;
    }

    /**
     * Delivers {@link ImageCaptureException} to the app.
     */
    void onError(@NonNull ImageCaptureException imageCaptureException) {
        getAppExecutor().execute(() -> {
            boolean hasInMemory = getInMemoryCallback() != null;
            boolean hasOnDisk = getOnDiskCallback() != null;
            if (hasInMemory && !hasOnDisk) {
                requireNonNull(getInMemoryCallback()).onError(imageCaptureException);
            } else if (hasOnDisk && !hasInMemory) {
                requireNonNull(getOnDiskCallback()).onError(imageCaptureException);
            } else {
                throw new IllegalStateException("One and only one callback is allowed.");
            }
        });
    }

    /**
     * Delivers on-disk capture result to the app.
     */
    void onResult(@Nullable ImageCapture.OutputFileResults outputFileResults) {
        getAppExecutor().execute(() -> requireNonNull(getOnDiskCallback()).onImageSaved(
                requireNonNull(outputFileResults)));
    }

    /**
     * Delivers in-memory capture result to the app.
     */
    void onResult(@Nullable ImageProxy imageProxy) {
        getAppExecutor().execute(() -> requireNonNull(getInMemoryCallback()).onCaptureSuccess(
                requireNonNull(imageProxy)));
    }

    void onCaptureProcessProgressed(int progress) {
        getAppExecutor().execute(() -> {
            if (getOnDiskCallback() != null) {
                getOnDiskCallback().onCaptureProcessProgressed(progress);
            } else if (getInMemoryCallback() != null) {
                getInMemoryCallback().onCaptureProcessProgressed(progress);
            }
        });
    }

    /**
     * Delivers postview bitmap result to the app.
     */
    void onPostviewBitmapAvailable(@NonNull Bitmap bitmap) {
        getAppExecutor().execute(() -> {
            if (getOnDiskCallback() != null) {
                getOnDiskCallback().onPostviewBitmapAvailable(bitmap);
            } else if (getInMemoryCallback() != null) {
                getInMemoryCallback().onPostviewBitmapAvailable(bitmap);
            }
        });
    }

    /**
     * Creates a {@link TakePictureRequest} instance.
     */
    @NonNull
    public static TakePictureRequest of(@NonNull Executor appExecutor,
            @Nullable ImageCapture.OnImageCapturedCallback inMemoryCallback,
            @Nullable ImageCapture.OnImageSavedCallback onDiskCallback,
            @Nullable List<ImageCapture.OutputFileOptions> outputFileOptions,
            @NonNull Rect cropRect,
            @NonNull Matrix sensorToBufferTransform,
            int rotationDegrees,
            int jpegQuality,
            @ImageCapture.CaptureMode int captureMode,
            boolean isSimultaneousCapture,
            @NonNull List<CameraCaptureCallback> sessionConfigCameraCaptureCallbacks) {
        checkArgument((onDiskCallback == null) == (outputFileOptions == null),
                "onDiskCallback and outputFileOptions should be both null or both non-null.");
        checkArgument((onDiskCallback == null) ^ (inMemoryCallback == null),
                "One and only one on-disk or in-memory callback should be present.");
        TakePictureRequest request = new AutoValue_TakePictureRequest(appExecutor, inMemoryCallback,
                onDiskCallback, outputFileOptions, cropRect,
                sensorToBufferTransform, rotationDegrees, jpegQuality, captureMode,
                isSimultaneousCapture,
                sessionConfigCameraCaptureCallbacks);
        if (isSimultaneousCapture) {
            request.initFormatProcessStatusInSimultaneousCapture();
        }
        return request;
    }

    /**
     * Interface for retrying a {@link TakePictureRequest}.
     */
    interface RetryControl {

        /**
         * Retries the given {@link TakePictureRequest}.
         *
         * <p>The request should be injected to the front of the queue.
         */
        void retryRequest(@NonNull TakePictureRequest takePictureRequest);
    }
}
