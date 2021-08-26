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

package androidx.camera.core.impl;

import android.graphics.ImageFormat;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.CaptureMode;
import androidx.camera.core.ImageReaderProxyProvider;
import androidx.camera.core.internal.IoConfig;

import java.util.concurrent.Executor;

/**
 * Configuration for an image capture use case.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ImageCaptureConfig implements UseCaseConfig<ImageCapture>, ImageOutputConfig,
        IoConfig {

    // Option Declarations:
    // *********************************************************************************************

    public static final Option<Integer> OPTION_IMAGE_CAPTURE_MODE =
            Option.create(
                    "camerax.core.imageCapture.captureMode", int.class);
    public static final Option<Integer> OPTION_FLASH_MODE =
            Option.create("camerax.core.imageCapture.flashMode", int.class);
    public static final Option<CaptureBundle> OPTION_CAPTURE_BUNDLE =
            Option.create("camerax.core.imageCapture.captureBundle", CaptureBundle.class);
    public static final Option<CaptureProcessor> OPTION_CAPTURE_PROCESSOR =
            Option.create("camerax.core.imageCapture.captureProcessor", CaptureProcessor.class);
    public static final Option<Integer> OPTION_BUFFER_FORMAT =
            Option.create("camerax.core.imageCapture.bufferFormat", Integer.class);
    public static final Option<Integer> OPTION_MAX_CAPTURE_STAGES =
            Option.create("camerax.core.imageCapture.maxCaptureStages", Integer.class);
    public static final Option<ImageReaderProxyProvider> OPTION_IMAGE_READER_PROXY_PROVIDER =
            Option.create("camerax.core.imageCapture.imageReaderProxyProvider",
                    ImageReaderProxyProvider.class);
    public static final Option<Boolean> OPTION_USE_SOFTWARE_JPEG_ENCODER =
            Option.create("camerax.core.imageCapture.useSoftwareJpegEncoder", boolean.class);
    public static final Option<Integer> OPTION_FLASH_TYPE =
            Option.create("camerax.core.imageCapture.flashType", int.class);
    public static final Option<Integer> OPTION_JPEG_COMPRESSION_QUALITY =
            Option.create("camerax.core.imageCapture.jpegCompressionQuality", int.class);

    // *********************************************************************************************

    private final OptionsBundle mConfig;

    /** Creates a new configuration instance. */
    public ImageCaptureConfig(@NonNull OptionsBundle config) {
        mConfig = config;
    }

    @NonNull
    @Override
    public Config getConfig() {
        return mConfig;
    }

    /**
     * Returns whether a {@link CaptureMode} option has been set in this configuration.
     *
     * @return true if a {@link CaptureMode} option has been set in this configuration, false
     * otherwise.
     */
    public boolean hasCaptureMode() {
        return containsOption(OPTION_IMAGE_CAPTURE_MODE);
    }

    /**
     * Returns the {@link CaptureMode}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @CaptureMode
    public int getCaptureMode() {
        return retrieveOption(OPTION_IMAGE_CAPTURE_MODE);
    }

    /**
     * Returns the {@link ImageCapture.FlashMode}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value, if it exists in this configuration.
     */
    @ImageCapture.FlashMode
    public int getFlashMode(@ImageCapture.FlashMode int valueIfMissing) {
        return retrieveOption(OPTION_FLASH_MODE, valueIfMissing);
    }

    /**
     * Returns the {@link ImageCapture.FlashMode}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @ImageCapture.FlashMode
    public int getFlashMode() {
        return retrieveOption(OPTION_FLASH_MODE);
    }

    /**
     * Returns the {@link CaptureBundle}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    public CaptureBundle getCaptureBundle(@Nullable CaptureBundle valueIfMissing) {
        return retrieveOption(OPTION_CAPTURE_BUNDLE, valueIfMissing);
    }

    /**
     * Returns the {@link CaptureBundle}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    public CaptureBundle getCaptureBundle() {
        return retrieveOption(OPTION_CAPTURE_BUNDLE);
    }

    /**
     * Returns the {@link CaptureProcessor}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    public CaptureProcessor getCaptureProcessor(@Nullable CaptureProcessor valueIfMissing) {
        return retrieveOption(OPTION_CAPTURE_PROCESSOR, valueIfMissing);
    }

    /**
     * Returns the {@link CaptureProcessor}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    public CaptureProcessor getCaptureProcessor() {
        return retrieveOption(OPTION_CAPTURE_PROCESSOR);
    }

    /**
     * Returns the {@link ImageFormat} of the capture in memory.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>ValueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    public Integer getBufferFormat(@Nullable Integer valueIfMissing) {
        return retrieveOption(OPTION_BUFFER_FORMAT, valueIfMissing);
    }

    /**
     * Returns the {@link ImageFormat} of the capture in memory.
     *
     * @return The stored value, if it exists in the configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    public Integer getBufferFormat() {
        return retrieveOption(OPTION_BUFFER_FORMAT);
    }

    /**
     * Retrieves the format of the image that is fed as input.
     *
     * <p>This should be YUV_420_888, when processing is run on the image. Otherwise it is JPEG.
     */
    @Override
    public int getInputFormat() {
        return retrieveOption(OPTION_INPUT_FORMAT);
    }

    /**
     * Returns the max number of {@link CaptureStage}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in
     * this configuration.
     */
    public int getMaxCaptureStages(int valueIfMissing) {
        return retrieveOption(OPTION_MAX_CAPTURE_STAGES, valueIfMissing);
    }

    /**
     * Returns the max number of {@link CaptureStage}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public int getMaxCaptureStages() {
        return retrieveOption(OPTION_MAX_CAPTURE_STAGES);
    }

    /**
     * Gets the caller provided {@link ImageReaderProxy}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    public ImageReaderProxyProvider getImageReaderProxyProvider() {
        return retrieveOption(OPTION_IMAGE_READER_PROXY_PROVIDER, null);
    }

    /**
     * Returns whether ImageCapture should use a software JPEG encoder, if available.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean isSoftwareJpegEncoderRequested() {
        return retrieveOption(OPTION_USE_SOFTWARE_JPEG_ENCODER, false);
    }

    /**
     * Returns the {@link ImageCapture.FlashType}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value, if it exists in this configuration.
     */
    @ImageCapture.FlashType
    public int getFlashType(@ImageCapture.FlashType int valueIfMissing) {
        return retrieveOption(OPTION_FLASH_TYPE, valueIfMissing);
    }

    /**
     * Returns the {@link ImageCapture.FlashType}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @ImageCapture.FlashType
    public int getFlashType() {
        return retrieveOption(OPTION_FLASH_TYPE);
    }

    /**
     * Returns the JPEG compression quality setting.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @IntRange(from = 1, to = 100)
    public int getJpegQuality(@IntRange(from = 1, to = 100) int valueIfMissing) {
        return retrieveOption(OPTION_JPEG_COMPRESSION_QUALITY, valueIfMissing);
    }


    /**
     * Returns the JPEG compression quality setting.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @IntRange(from = 1, to = 100)
    public int getJpegQuality() {
        return retrieveOption(OPTION_JPEG_COMPRESSION_QUALITY);
    }

    // Implementations of IO default methods

    /**
     * Returns the executor that will be used for IO tasks.
     *
     * <p> This executor will be used for any IO tasks specifically for ImageCapture, such as
     * {@link ImageCapture#takePicture(ImageCapture.OutputFileOptions, Executor,
     * ImageCapture.OnImageSavedCallback)}. If no executor is set, then a default Executor
     * specifically for IO will be used instead.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    @Override
    public Executor getIoExecutor(@Nullable Executor valueIfMissing) {
        return retrieveOption(OPTION_IO_EXECUTOR, valueIfMissing);
    }

    /**
     * Returns the executor that will be used for IO tasks.
     *
     * <p> This executor will be used for any IO tasks specifically for ImageCapture, such as
     * {@link ImageCapture#takePicture(ImageCapture.OutputFileOptions, Executor,
     * ImageCapture.OnImageSavedCallback)}. If no executor is set, then a default Executor
     * specifically for IO will be used instead.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    @Override
    public Executor getIoExecutor() {
        return retrieveOption(OPTION_IO_EXECUTOR);
    }
}
