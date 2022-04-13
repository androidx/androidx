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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Preview;
import androidx.camera.core.internal.ThreadConfig;

/**
 * Configuration for a {@link Preview} use case.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class PreviewConfig
        implements UseCaseConfig<Preview>,
        ImageOutputConfig,
        ThreadConfig {

    // Options declarations

    public static final Option<ImageInfoProcessor> IMAGE_INFO_PROCESSOR = Option.create(
            "camerax.core.preview.imageInfoProcessor", ImageInfoProcessor.class);
    public static final Option<CaptureProcessor> OPTION_PREVIEW_CAPTURE_PROCESSOR =
            Option.create("camerax.core.preview.captureProcessor", CaptureProcessor.class);
    private final OptionsBundle mConfig;

    /** Creates a new configuration instance. */
    public PreviewConfig(@NonNull OptionsBundle config) {
        mConfig = config;
    }

    @NonNull
    @Override
    public Config getConfig() {
        return mConfig;
    }

    /**
     * Returns the {@link ImageInfoProcessor}.
     *
     * @return The stored value, if it exists in this configuration.
     */
    @Nullable
    public ImageInfoProcessor getImageInfoProcessor(@Nullable ImageInfoProcessor valueIfMissing) {
        return retrieveOption(IMAGE_INFO_PROCESSOR, valueIfMissing);
    }

    @NonNull
    ImageInfoProcessor getImageInfoProcessor() {
        return retrieveOption(IMAGE_INFO_PROCESSOR);
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
        return retrieveOption(OPTION_PREVIEW_CAPTURE_PROCESSOR, valueIfMissing);
    }

    /**
     * Returns the {@link CaptureProcessor}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    public CaptureProcessor getCaptureProcessor() {
        return retrieveOption(OPTION_PREVIEW_CAPTURE_PROCESSOR);
    }

    /**
     * Retrieves the format of the image that is fed as input.
     *
     * <p>This should be YUV_420_888, when processing is run on the image. Otherwise it is PRIVATE.
     */
    @Override
    public int getInputFormat() {
        return retrieveOption(OPTION_INPUT_FORMAT);
    }
}
