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

import com.google.auto.value.AutoValue;

import java.io.File;

/**
 * A class to store the result to a given file.
 *
 * <p> The file could be in a path where the application has permission to write in.
 */
@AutoValue
public abstract class FileOutputOptions extends OutputOptions {

    FileOutputOptions() {
        super(Type.FILE);
    }

    /** Returns a builder for this FileOutputOptions. */
    @NonNull
    public static Builder builder() {
        return new AutoValue_FileOutputOptions.Builder()
                .setFileSizeLimit(FILE_SIZE_UNLIMITED);
    }

    /**
     * Gets the limit for the file length in bytes.
     */
    @Override
    public abstract int getFileSizeLimit();

    /** Gets the File instance */
    @NonNull
    public abstract File getFile();

    /** The builder of the {@link FileOutputOptions}. */
    @AutoValue.Builder
    public abstract static class Builder {
        Builder() {
        }

        /** Defines how to store the result. */
        @NonNull
        public abstract Builder setFile(@NonNull File file);

        /**
         * Sets the limit for the file length in bytes. Zero or negative values are considered
         * unlimited.
         *
         * <p>If not set, defaults to {@link #FILE_SIZE_UNLIMITED}.
         */
        @NonNull
        public abstract Builder setFileSizeLimit(int bytes);

        /** Builds the FileOutputOptions instance. */
        @NonNull
        public abstract FileOutputOptions build();
    }
}
