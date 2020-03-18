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
import android.util.Pair;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.CaptureMode;
import androidx.camera.core.UseCase;
import androidx.camera.core.internal.IoConfig;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Configuration for an image capture use case.
 */
public final class ImageCaptureConfig
        implements UseCaseConfig<ImageCapture>,
        ImageOutputConfig,
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

    // *********************************************************************************************

    private final OptionsBundle mConfig;

    /** Creates a new configuration instance. */
    public ImageCaptureConfig(@NonNull OptionsBundle config) {
        mConfig = config;
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

    // Start of the default implementation of Config
    // *********************************************************************************************

    // Implementations of Config default methods

    @Override
    public boolean containsOption(@NonNull Option<?> id) {
        return mConfig.containsOption(id);
    }

    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(@NonNull Option<ValueT> id) {
        return mConfig.retrieveOption(id);
    }

    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(@NonNull Option<ValueT> id,
            @Nullable ValueT valueIfMissing) {
        return mConfig.retrieveOption(id, valueIfMissing);
    }

    @Override
    public void findOptions(@NonNull String idStem, @NonNull OptionMatcher matcher) {
        mConfig.findOptions(idStem, matcher);
    }

    @Override
    @NonNull
    public Set<Option<?>> listOptions() {
        return mConfig.listOptions();
    }

    // Implementations of TargetConfig default methods

    @Override
    @Nullable
    public Class<ImageCapture> getTargetClass(
            @Nullable Class<ImageCapture> valueIfMissing) {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<ImageCapture> storedClass =
                (Class<ImageCapture>) retrieveOption(
                        OPTION_TARGET_CLASS,
                        valueIfMissing);
        return storedClass;
    }

    @Override
    @NonNull
    public Class<ImageCapture> getTargetClass() {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<ImageCapture> storedClass =
                (Class<ImageCapture>) retrieveOption(
                        OPTION_TARGET_CLASS);
        return storedClass;
    }

    /**
     * Retrieves the name of the target object being configured.
     *
     * <p>The name should be a value that can uniquely identify an instance of the object being
     * configured.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Override
    @Nullable
    public String getTargetName(@Nullable String valueIfMissing) {
        return retrieveOption(OPTION_TARGET_NAME, valueIfMissing);
    }

    /**
     * Retrieves the name of the target object being configured.
     *
     * <p>The name should be a value that can uniquely identify an instance of the object being
     * configured.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @Override
    @NonNull
    public String getTargetName() {
        return retrieveOption(OPTION_TARGET_NAME);
    }

    // Implementations of ImageOutputConfig default methods

    /**
     * Retrieves the aspect ratio of the target intending to use images from this configuration.
     *
     * <p>This is the ratio of the target's width to the image's height, where the numerator of the
     * provided {@link Rational} corresponds to the width, and the denominator corresponds to the
     * height.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Override
    @Nullable
    public Rational getTargetAspectRatioCustom(@Nullable Rational valueIfMissing) {
        return retrieveOption(OPTION_TARGET_ASPECT_RATIO_CUSTOM, valueIfMissing);
    }

    /**
     * Retrieves the aspect ratio of the target intending to use images from this configuration.
     *
     * <p>This is the ratio of the target's width to the image's height, where the numerator of the
     * provided {@link Rational} corresponds to the width, and the denominator corresponds to the
     * height.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    @Override
    public Rational getTargetAspectRatioCustom() {
        return retrieveOption(OPTION_TARGET_ASPECT_RATIO_CUSTOM);
    }

    @Override
    public boolean hasTargetAspectRatio() {
        return containsOption(OPTION_TARGET_ASPECT_RATIO);
    }

    /**
     * Retrieves the aspect ratio of the target intending to use images from this configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @AspectRatio.Ratio
    @Override
    public int getTargetAspectRatio() {
        return retrieveOption(OPTION_TARGET_ASPECT_RATIO);
    }

    /**
     * Retrieves the rotation of the target intending to use images from this configuration.
     *
     * <p>This is one of four valid values: {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     * {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}. Rotation values are relative to
     * the device's "natural" rotation, {@link Surface#ROTATION_0}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Override
    @RotationValue
    public int getTargetRotation(int valueIfMissing) {
        return retrieveOption(OPTION_TARGET_ROTATION, valueIfMissing);
    }

    /**
     * Retrieves the rotation of the target intending to use images from this configuration.
     *
     * <p>This is one of four valid values: {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     * {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}. Rotation values are relative to
     * the device's "natural" rotation, {@link Surface#ROTATION_0}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @Override
    @RotationValue
    public int getTargetRotation() {
        return retrieveOption(OPTION_TARGET_ROTATION);
    }

    /**
     * Retrieves the resolution of the target intending to use from this configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Override
    @Nullable
    public Size getTargetResolution(@Nullable Size valueIfMissing) {
        return retrieveOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION, valueIfMissing);
    }

    /**
     * Retrieves the resolution of the target intending to use from this configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @Override
    @NonNull
    public Size getTargetResolution() {
        return retrieveOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION);
    }

    /**
     * Retrieves the default resolution of the target intending to use from this configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    @Override
    public Size getDefaultResolution(@Nullable Size valueIfMissing) {
        return retrieveOption(OPTION_DEFAULT_RESOLUTION, valueIfMissing);
    }

    /**
     * Retrieves the default resolution of the target intending to use from this configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    @Override
    public Size getDefaultResolution() {
        return retrieveOption(OPTION_DEFAULT_RESOLUTION);
    }

    @Override
    @Nullable
    public Size getMaxResolution(@Nullable Size valueIfMissing) {
        return retrieveOption(OPTION_MAX_RESOLUTION, valueIfMissing);
    }

    @Override
    @NonNull
    public Size getMaxResolution() {
        return retrieveOption(OPTION_MAX_RESOLUTION);
    }

    @Override
    @Nullable
    public List<Pair<Integer, Size[]>> getSupportedResolutions(
            @Nullable List<Pair<Integer, Size[]>> valueIfMissing) {
        return retrieveOption(OPTION_SUPPORTED_RESOLUTIONS, valueIfMissing);
    }

    @Override
    @NonNull
    public List<Pair<Integer, Size[]>> getSupportedResolutions() {
        return retrieveOption(OPTION_SUPPORTED_RESOLUTIONS);
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

    // Implementations of UseCaseConfig default methods

    @Override
    @Nullable
    public SessionConfig getDefaultSessionConfig(@Nullable SessionConfig valueIfMissing) {
        return retrieveOption(OPTION_DEFAULT_SESSION_CONFIG, valueIfMissing);
    }

    @Override
    @NonNull
    public SessionConfig getDefaultSessionConfig() {
        return retrieveOption(OPTION_DEFAULT_SESSION_CONFIG);
    }

    @Override
    @Nullable
    public CaptureConfig getDefaultCaptureConfig(@Nullable CaptureConfig valueIfMissing) {
        return retrieveOption(OPTION_DEFAULT_CAPTURE_CONFIG, valueIfMissing);
    }

    @Override
    @NonNull
    public CaptureConfig getDefaultCaptureConfig() {
        return retrieveOption(OPTION_DEFAULT_CAPTURE_CONFIG);
    }

    @Override
    @Nullable
    public SessionConfig.OptionUnpacker getSessionOptionUnpacker(
            @Nullable SessionConfig.OptionUnpacker valueIfMissing) {
        return retrieveOption(OPTION_SESSION_CONFIG_UNPACKER, valueIfMissing);
    }

    @Override
    @NonNull
    public SessionConfig.OptionUnpacker getSessionOptionUnpacker() {
        return retrieveOption(OPTION_SESSION_CONFIG_UNPACKER);
    }

    @Override
    @Nullable
    public CaptureConfig.OptionUnpacker getCaptureOptionUnpacker(
            @Nullable CaptureConfig.OptionUnpacker valueIfMissing) {
        return retrieveOption(OPTION_CAPTURE_CONFIG_UNPACKER, valueIfMissing);
    }

    @Override
    @NonNull
    public CaptureConfig.OptionUnpacker getCaptureOptionUnpacker() {
        return retrieveOption(OPTION_CAPTURE_CONFIG_UNPACKER);
    }

    @Override
    public int getSurfaceOccupancyPriority(int valueIfMissing) {
        return retrieveOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, valueIfMissing);
    }

    @Override
    public int getSurfaceOccupancyPriority() {
        return retrieveOption(OPTION_SURFACE_OCCUPANCY_PRIORITY);
    }

    @Override
    @Nullable
    public CameraSelector getCameraSelector(@Nullable CameraSelector valueIfMissing) {
        return retrieveOption(OPTION_CAMERA_SELECTOR, valueIfMissing);
    }

    @Override
    @NonNull
    public CameraSelector getCameraSelector() {
        return retrieveOption(OPTION_CAMERA_SELECTOR);
    }

    @Override
    @Nullable
    public UseCase.EventCallback getUseCaseEventCallback(
            @Nullable UseCase.EventCallback valueIfMissing) {
        return retrieveOption(OPTION_USE_CASE_EVENT_CALLBACK, valueIfMissing);
    }

    @Override
    @NonNull
    public UseCase.EventCallback getUseCaseEventCallback() {
        return retrieveOption(OPTION_USE_CASE_EVENT_CALLBACK);
    }

    // End of the default implementation of Config
    // *********************************************************************************************
}
