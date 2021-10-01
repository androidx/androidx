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
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.CameraCaptureMetaData.AeState;
import androidx.camera.core.impl.CameraCaptureMetaData.AfMode;
import androidx.camera.core.impl.CameraCaptureMetaData.AfState;
import androidx.camera.core.impl.CameraCaptureMetaData.AwbState;
import androidx.camera.core.impl.CameraCaptureMetaData.FlashState;
import androidx.camera.core.impl.utils.ExifData;

/**
 * The result of a single image capture.
 *
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface CameraCaptureResult {

    /** Returns the current auto focus mode of operation. */
    @NonNull
    AfMode getAfMode();

    /** Returns the current auto focus state. */
    @NonNull
    AfState getAfState();

    /** Returns the current auto exposure state. */
    @NonNull
    AeState getAeState();

    /** Returns the current auto white balance state.*/
    @NonNull
    AwbState getAwbState();

    /** Returns the current flash state. */
    @NonNull
    FlashState getFlashState();

    /**
     * Returns the timestamp in nanoseconds.
     *
     * <p> If the timestamp was unavailable then it will return {@code -1L}.
     */
    long getTimestamp();

    /** Returns the TagBundle object associated with the capture request. */
    @NonNull
    TagBundle getTagBundle();

    /** Populates the given Exif.Builder with attributes from this CameraCaptureResult. */
    default void populateExifData(@NonNull ExifData.Builder exifBuilder) {
        exifBuilder.setFlashState(getFlashState());
    }

    /** An implementation of CameraCaptureResult which always return default results. */
    final class EmptyCameraCaptureResult implements CameraCaptureResult {

        @NonNull
        public static CameraCaptureResult create() {
            return new EmptyCameraCaptureResult();
        }

        @NonNull
        @Override
        public AfMode getAfMode() {
            return AfMode.UNKNOWN;
        }

        @NonNull
        @Override
        public AfState getAfState() {
            return AfState.UNKNOWN;
        }

        @NonNull
        @Override
        public AeState getAeState() {
            return AeState.UNKNOWN;
        }

        @NonNull
        @Override
        public AwbState getAwbState() {
            return AwbState.UNKNOWN;
        }

        @NonNull
        @Override
        public FlashState getFlashState() {
            return FlashState.UNKNOWN;
        }

        @Override
        public long getTimestamp() {
            return -1L;
        }

        @Override
        @NonNull
        public TagBundle getTagBundle() {
            return TagBundle.emptyBundle();
        }
    }
}
