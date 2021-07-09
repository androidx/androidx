/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video;

import androidx.annotation.NonNull;

/**
 * Options for configuring output destination for generating a recording.
 *
 * <p>A {@link PendingRecording} can be generated with {@link Recorder#prepareRecording} for
 * different types of output destination, such as {@link FileOutputOptions},
 * {@link FileDescriptorOutputOptions} and {@link MediaStoreOutputOptions}.
 *
 * @see FileOutputOptions
 * @see FileDescriptorOutputOptions
 * @see MediaStoreOutputOptions
 */
public abstract class OutputOptions {

    /** Represents an unbound file size. */
    public static final int FILE_SIZE_UNLIMITED = 0;

    OutputOptions() {
    }

    /**
     * Gets the limit for the file size in bytes.
     *
     * @return the file size limit in bytes.
     */
    public abstract long getFileSizeLimit();

    /**
     * The builder of the {@link OutputOptions}.
     */
    interface Builder<T extends OutputOptions, B> {

        /**
         * Sets the limit for the file length in bytes.
         *
         * <p>If not set, defaults to {@link #FILE_SIZE_UNLIMITED}.
         */
        @NonNull
        B setFileSizeLimit(long bytes);

        /**
         * Builds the {@link OutputOptions} instance.
         */
        @NonNull
        T build();
    }
}
