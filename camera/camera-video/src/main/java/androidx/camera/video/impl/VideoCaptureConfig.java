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

package androidx.camera.video.impl;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.arch.core.util.Function;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.internal.ThreadConfig;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoOutput;
import androidx.camera.video.internal.encoder.VideoEncoderConfig;
import androidx.camera.video.internal.encoder.VideoEncoderInfo;

import java.util.Objects;

/**
 * Config for a video capture use case.
 *
 * <p>In the earlier stage, the VideoCapture is deprioritized.
 *
 * @param <T> the type of VideoOutput
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class VideoCaptureConfig<T extends VideoOutput>
        implements UseCaseConfig<VideoCapture<T>>,
        ImageOutputConfig,
        ThreadConfig {

    // Option Declarations:
    // *********************************************************************************************

    public static final Option<VideoOutput> OPTION_VIDEO_OUTPUT =
            Option.create("camerax.video.VideoCapture.videoOutput", VideoOutput.class);

    public static final Option<Function<VideoEncoderConfig, VideoEncoderInfo>>
            OPTION_VIDEO_ENCODER_INFO_FINDER =
            Option.create("camerax.video.VideoCapture.videoEncoderInfoFinder", Function.class);

    // *********************************************************************************************

    private final OptionsBundle mConfig;

    public VideoCaptureConfig(@NonNull OptionsBundle config) {
        mConfig = config;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public T getVideoOutput() {
        return (T) retrieveOption(OPTION_VIDEO_OUTPUT);
    }

    @NonNull
    public Function<VideoEncoderConfig, VideoEncoderInfo> getVideoEncoderInfoFinder() {
        return Objects.requireNonNull(retrieveOption(OPTION_VIDEO_ENCODER_INFO_FINDER));
    }

    /**
     * Retrieves the format of the image that is fed as input.
     *
     * <p>This should always be PRIVATE for VideoCapture.
     */
    @Override
    public int getInputFormat() {
        return ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
    }

    @NonNull
    @Override
    public Config getConfig() {
        return mConfig;
    }
}
