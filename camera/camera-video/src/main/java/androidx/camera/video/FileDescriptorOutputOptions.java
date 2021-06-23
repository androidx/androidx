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

import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

/**
 * A class to store the result to a given file descriptor.
 *
 * <p>The file descriptor must be seekable and writable. And the caller should be responsible for
 * closing the file descriptor.
 *
 * <p>To use a {@link java.io.File} as an output destination instead of a file descriptor, use
 * {@link FileOutputOptions}.
 */
@AutoValue
public abstract class FileDescriptorOutputOptions extends OutputOptions {

    FileDescriptorOutputOptions() {
        super(Type.FILE_DESCRIPTOR);
    }

    /** Returns a builder for this FileDescriptorOutputOptions. */
    @NonNull
    public static Builder builder() {
        return new AutoValue_FileDescriptorOutputOptions.Builder()
                .setFileSizeLimit(FILE_SIZE_UNLIMITED);
    }

    /**
     * Gets the limit for the file length in bytes.
     */
    @Override
    public abstract int getFileSizeLimit();

    /**
     * Gets the file descriptor instance.
     * @return the file descriptor used as the output destination.
     */
    @NonNull
    public abstract ParcelFileDescriptor getParcelFileDescriptor();

    /** The builder of the {@link FileDescriptorOutputOptions}. */
    @AutoValue.Builder
    @SuppressWarnings("StaticFinalBuilder")
    public abstract static class Builder {
        Builder() {
        }

        /**
         * Defines the file descriptor used to store the result.
         * @param fileDescriptor the file descriptor to use as the output destination.
         */
        @NonNull
        public abstract Builder setParcelFileDescriptor(
                @NonNull ParcelFileDescriptor fileDescriptor);

        /**
         * Sets the limit for the file length in bytes. Zero or negative values are considered
         * unlimited.
         *
         * <p>If not set, defaults to {@link #FILE_SIZE_UNLIMITED}.
         */
        @NonNull
        public abstract Builder setFileSizeLimit(int bytes);

        /** Builds the FileDescriptorOutputOptions instance. */
        @NonNull
        public abstract FileDescriptorOutputOptions build();
    }
}
