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
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

import java.io.File;

/**
 * A class providing options for storing the result to a given file.
 *
 * <p>The file must be in a path where the application has the write permission.
 *
 * <p>To use a {@link android.os.ParcelFileDescriptor} as an output destination instead of a
 * {@link File}, use {@link FileDescriptorOutputOptions}.
 */
public final class FileOutputOptions extends OutputOptions {

    private final FileOutputOptionsInternal mFileOutputOptionsInternal;

    FileOutputOptions(@NonNull FileOutputOptionsInternal fileOutputOptionsInternal) {
        Preconditions.checkNotNull(fileOutputOptionsInternal,
                "FileOutputOptionsInternal can't be null.");
        mFileOutputOptionsInternal = fileOutputOptionsInternal;
    }

    /** Gets the File instance */
    @NonNull
    public File getFile() {
        return mFileOutputOptionsInternal.getFile();
    }

    /**
     * Gets the limit for the file length in bytes.
     */
    @Override
    public long getFileSizeLimit() {
        return mFileOutputOptionsInternal.getFileSizeLimit();
    }

    @Override
    @NonNull
    public String toString() {
        // Don't use Class.getSimpleName(), class name will be changed by proguard obfuscation.
        return mFileOutputOptionsInternal.toString().replaceFirst("FileOutputOptionsInternal",
                "FileOutputOptions");
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileOutputOptions)) {
            return false;
        }
        return mFileOutputOptionsInternal.equals(
                ((FileOutputOptions) o).mFileOutputOptionsInternal);
    }

    @Override
    public int hashCode() {
        return mFileOutputOptionsInternal.hashCode();
    }

    /** The builder of the {@link FileOutputOptions} object. */
    public static final class Builder implements OutputOptions.Builder<FileOutputOptions, Builder> {
        private final FileOutputOptionsInternal.Builder mInternalBuilder =
                new AutoValue_FileOutputOptions_FileOutputOptionsInternal.Builder()
                        .setFileSizeLimit(OutputOptions.FILE_SIZE_UNLIMITED);

        /**
         * Creates a builder of the {@link FileOutputOptions} with a file object.
         *
         * <p>The file object can be created with a path using the {@link File} APIs. The path
         * must be seekable and writable.
         *
         * @param file the file object.
         * @see File
         */
        @SuppressWarnings("StreamFiles") // FileDescriptor API is in FileDescriptorOutputOptions
        public Builder(@NonNull File file) {
            Preconditions.checkNotNull(file, "File can't be null.");
            mInternalBuilder.setFile(file);
        }

        /**
         * Sets the limit for the file length in bytes.
         *
         * <p>When used to
         * {@link Recorder#prepareRecording(android.content.Context, FileOutputOptions) generate}
         * recording, if the specified file size limit is reached while the recording is being
         * recorded, the recording will be finalized with
         * {@link VideoRecordEvent.Finalize#ERROR_FILE_SIZE_LIMIT_REACHED}.
         *
         * <p>If not set, defaults to {@link #FILE_SIZE_UNLIMITED}.
         */
        @Override
        @NonNull
        public Builder setFileSizeLimit(long fileSizeLimitBytes) {
            mInternalBuilder.setFileSizeLimit(fileSizeLimitBytes);
            return this;
        }

        /** Builds the {@link FileOutputOptions} instance. */
        @Override
        @NonNull
        public FileOutputOptions build() {
            return new FileOutputOptions(mInternalBuilder.build());
        }
    }

    @AutoValue
    abstract static class FileOutputOptionsInternal {
        @NonNull
        abstract File getFile();

        abstract long getFileSizeLimit();

        @AutoValue.Builder
        abstract static class Builder {
            @NonNull
            abstract Builder setFile(@NonNull File file);

            @NonNull
            abstract Builder setFileSizeLimit(long fileSizeLimitBytes);

            @NonNull
            abstract FileOutputOptionsInternal build();
        }
    }
}
