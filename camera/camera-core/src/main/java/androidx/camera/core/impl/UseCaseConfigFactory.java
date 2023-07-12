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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCapture.CaptureMode;
import androidx.camera.core.InitializationException;

/**
 * A Repository for generating use case configurations.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface UseCaseConfigFactory {

    enum CaptureType {
        /**
         * Capture type for still image capture. A still capture which can be a single or
         * multiple frames which are combined into a single image.
         */
        IMAGE_CAPTURE,

        /**
         * Capture type for preview. A use case of this type is consuming a stream of frames.
         */
        PREVIEW,

        /**
         * Capture type for image analysis. A use case of this type is consuming a stream of frames.
         */
        IMAGE_ANALYSIS,

        /**
         * Capture type for video capture. A use case of this type is consuming a stream of frames.
         */
        VIDEO_CAPTURE,

        /**
         * Capture type for stream sharing. A use case of this type is consuming a stream of frames.
         */
        STREAM_SHARING
    }

    /**
     * Interface for deferring creation of a UseCaseConfigFactory.
     */
    interface Provider {
        /**
         * Creates a new, initialized instance of a UseCaseConfigFactory.
         *
         * @param context the android context
         * @return the factory instance
         * @throws InitializationException if it fails to create the factory
         */
        @NonNull
        UseCaseConfigFactory newInstance(@NonNull Context context) throws InitializationException;
    }

    /**
     * Returns the configuration for the given capture type, or <code>null</code> if the
     * configuration cannot be produced.
     *
     * @param captureType The {@link CaptureType} for the configuration.
     * @param captureMode The {@link CaptureMode} for the configuration.
     * @return The use case configuration.
     */
    @Nullable
    Config getConfig(@NonNull CaptureType captureType, @CaptureMode int captureMode);

    UseCaseConfigFactory EMPTY_INSTANCE = new UseCaseConfigFactory() {
        @Nullable
        @Override
        public Config getConfig(@NonNull CaptureType captureType, @CaptureMode int captureMode) {
            return null;
        }
    };
}
