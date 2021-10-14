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
import android.media.ImageReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysis.BackpressureStrategy;
import androidx.camera.core.ImageReaderProxyProvider;
import androidx.camera.core.internal.ThreadConfig;

/**
 * Configuration for an image analysis use case.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ImageAnalysisConfig
        implements UseCaseConfig<ImageAnalysis>,
        ImageOutputConfig,
        ThreadConfig {

    // Option Declarations:
    // *********************************************************************************************

    public static final Option<Integer> OPTION_BACKPRESSURE_STRATEGY =
            Option.create("camerax.core.imageAnalysis.backpressureStrategy",
                    BackpressureStrategy.class);
    public static final Option<Integer> OPTION_IMAGE_QUEUE_DEPTH =
            Option.create("camerax.core.imageAnalysis.imageQueueDepth", int.class);
    public static final Option<ImageReaderProxyProvider> OPTION_IMAGE_READER_PROXY_PROVIDER =
            Option.create("camerax.core.imageAnalysis.imageReaderProxyProvider",
                    ImageReaderProxyProvider.class);
    public static final Option<Integer> OPTION_OUTPUT_IMAGE_FORMAT =
            Option.create("camerax.core.imageAnalysis.outputImageFormat",
                    ImageAnalysis.OutputImageFormat.class);
    public static final Option<Boolean> OPTION_ONE_PIXEL_SHIFT_ENABLED =
            Option.create("camerax.core.imageAnalysis.onePixelShiftEnabled",
                    Boolean.class);

    // *********************************************************************************************

    private final OptionsBundle mConfig;

    public ImageAnalysisConfig(@NonNull OptionsBundle config) {
        mConfig = config;
    }

    @NonNull
    @Override
    public Config getConfig() {
        return mConfig;
    }

    /**
     * Retrieves the backpressure strategy applied to the image producer to deal with scenarios
     * where images may be produced faster than they can be analyzed.
     *
     * <p>The available values are {@link BackpressureStrategy#STRATEGY_BLOCK_PRODUCER} and {@link
     * BackpressureStrategy#STRATEGY_KEEP_ONLY_LATEST}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @see ImageAnalysis.Builder#setBackpressureStrategy(int)
     */
    @BackpressureStrategy
    public int getBackpressureStrategy(@BackpressureStrategy int valueIfMissing) {
        return retrieveOption(OPTION_BACKPRESSURE_STRATEGY, valueIfMissing);
    }

    /**
     * Returns the mode that the image is acquired from {@link ImageReader}.
     *
     * <p>The available values are {@link BackpressureStrategy#STRATEGY_BLOCK_PRODUCER} and {@link
     * BackpressureStrategy#STRATEGY_KEEP_ONLY_LATEST}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @BackpressureStrategy
    public int getBackpressureStrategy() {
        return retrieveOption(OPTION_BACKPRESSURE_STRATEGY);
    }

    /**
     * Returns the number of images available to the camera pipeline.
     *
     * <p>The image queue depth is the total number of images, including the image being analyzed,
     * available to the camera pipeline. If analysis takes long enough, the image queue may become
     * full and stall the camera pipeline.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public int getImageQueueDepth(int valueIfMissing) {
        return retrieveOption(OPTION_IMAGE_QUEUE_DEPTH, valueIfMissing);
    }

    /**
     * Returns the number of images available to the camera pipeline.
     *
     * <p>The image queue depth is the total number of images, including the image being analyzed,
     * available to the camera pipeline. If analysis takes long enough, the image queue may become
     * full and stall the camera pipeline.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public int getImageQueueDepth() {
        return retrieveOption(OPTION_IMAGE_QUEUE_DEPTH);
    }

    /**
     * Returns the output image format for image analysis.
     *
     * <p>The supported output image format
     * is {@link ImageAnalysis.OutputImageFormat#OUTPUT_IMAGE_FORMAT_YUV_420_888} and
     * {@link ImageAnalysis.OutputImageFormat#OUTPUT_IMAGE_FORMAT_RGBA_8888}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     *      configuration.
     */
    @ImageAnalysis.OutputImageFormat
    public int getOutputImageFormat(int valueIfMissing) {
        return retrieveOption(OPTION_OUTPUT_IMAGE_FORMAT, valueIfMissing);
    }

    /**
     * Gets if one pixel shift is requested or not.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Boolean getOnePixelShiftEnabled(@Nullable Boolean valueIfMissing) {
        return retrieveOption(OPTION_ONE_PIXEL_SHIFT_ENABLED, valueIfMissing);
    }

    /**
     * Gets the caller provided {@link ImageReaderProxy}.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public ImageReaderProxyProvider getImageReaderProxyProvider() {
        return retrieveOption(OPTION_IMAGE_READER_PROXY_PROVIDER, null);
    }

    /**
     * Retrieves the format of the image that is fed as input.
     *
     * <p>This should always be YUV_420_888 for ImageAnalysis.
     */
    @Override
    public int getInputFormat() {
        return ImageFormat.YUV_420_888;
    }
}
