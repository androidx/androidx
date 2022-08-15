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

import static androidx.core.util.Preconditions.checkArgument;

import android.graphics.Matrix;
import android.graphics.Rect;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.ImageOutputConfig;

import com.google.auto.value.AutoValue;

import java.util.concurrent.Executor;

/**
 * A {@link ImageCapture#takePicture} request.
 *
 * <p> It contains app provided data and a snapshot of {@link ImageCapture} properties.
 */
@AutoValue
public abstract class TakePictureRequest {

    /**
     * Gets the callback {@link Executor} provided by the app.
     */
    @NonNull
    abstract Executor getAppExecutor();

    /**
     * Gets the app provided callback for in-memory capture.
     */
    @Nullable
    abstract ImageCapture.OnImageCapturedCallback getInMemoryCallback();

    /**
     * Gets the app provided callback for on-disk capture.
     */
    @Nullable
    abstract ImageCapture.OnImageSavedCallback getOnDiskCallback();

    /**
     * Gets the app provided options for on-disk capture.
     */
    @Nullable
    abstract ImageCapture.OutputFileOptions getOutputFileOptions();

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
    abstract Matrix sensorToBufferTransform();

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
     * Creates a {@link TakePictureRequest} instance.
     */
    @NonNull
    public static TakePictureRequest of(@NonNull Executor appExecutor,
            @Nullable ImageCapture.OnImageCapturedCallback inMemoryCallback,
            @Nullable ImageCapture.OnImageSavedCallback onDiskCallback,
            @Nullable ImageCapture.OutputFileOptions outputFileOptions,
            @NonNull Rect cropRect,
            @NonNull Matrix sensorToBufferTransform,
            int rotationDegrees,
            int jpegQuality) {
        checkArgument((onDiskCallback == null) == (outputFileOptions == null),
                "onDiskCallback and outputFileOptions should be both null or both non-null.");
        checkArgument((onDiskCallback == null) ^ (inMemoryCallback == null),
                "One and only one on-disk or in-memory callback should be present.");
        return new AutoValue_TakePictureRequest(appExecutor, inMemoryCallback,
                onDiskCallback, outputFileOptions, cropRect, sensorToBufferTransform,
                rotationDegrees, jpegQuality);
    }
}
