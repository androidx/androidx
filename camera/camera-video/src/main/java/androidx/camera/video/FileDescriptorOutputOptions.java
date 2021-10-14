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
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

/**
 * A class providing options for storing the result to a given file descriptor.
 *
 * <p>The file descriptor must be seekable and writable. The caller is responsible for closing
 * the file descriptor, which can be safely closed after the recording starts. That is, after
 * {@link PendingRecording#start()} returns. Application should not use the file referenced by
 * this file descriptor until the recording is complete.
 *
 * <p>To use a {@link java.io.File} as an output destination instead of a file descriptor, use
 * {@link FileOutputOptions}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class FileDescriptorOutputOptions extends OutputOptions {

    private final FileDescriptorOutputOptionsInternal mFileDescriptorOutputOptionsInternal;

    FileDescriptorOutputOptions(
            @NonNull FileDescriptorOutputOptionsInternal fileDescriptorOutputOptionsInternal) {
        Preconditions.checkNotNull(fileDescriptorOutputOptionsInternal,
                "FileDescriptorOutputOptionsInternal can't be null.");
        mFileDescriptorOutputOptionsInternal = fileDescriptorOutputOptionsInternal;
    }

    /**
     * Gets the file descriptor instance.
     *
     * @return the file descriptor used as the output destination.
     */
    @NonNull
    public ParcelFileDescriptor getParcelFileDescriptor() {
        return mFileDescriptorOutputOptionsInternal.getParcelFileDescriptor();
    }

    /**
     * Gets the limit for the file length in bytes.
     */
    @Override
    public long getFileSizeLimit() {
        return mFileDescriptorOutputOptionsInternal.getFileSizeLimit();
    }

    @Override
    @NonNull
    public String toString() {
        // Don't use Class.getSimpleName(), class name will be changed by proguard obfuscation.
        return mFileDescriptorOutputOptionsInternal.toString().replaceFirst(
                "FileDescriptorOutputOptionsInternal", "FileDescriptorOutputOptions");
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileDescriptorOutputOptions)) {
            return false;
        }
        return mFileDescriptorOutputOptionsInternal.equals(
                ((FileDescriptorOutputOptions) o).mFileDescriptorOutputOptionsInternal);
    }

    @Override
    public int hashCode() {
        return mFileDescriptorOutputOptionsInternal.hashCode();
    }

    /** The builder of the {@link FileDescriptorOutputOptions} object. */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static final class Builder implements
            OutputOptions.Builder<FileDescriptorOutputOptions, Builder> {
        private final FileDescriptorOutputOptionsInternal.Builder mInternalBuilder =
                new AutoValue_FileDescriptorOutputOptions_FileDescriptorOutputOptionsInternal
                        .Builder()
                        .setFileSizeLimit(FILE_SIZE_UNLIMITED);

        /**
         * Creates a builder of the {@link FileDescriptorOutputOptions} with a file descriptor.
         *
         * @param fileDescriptor the file descriptor to use as the output destination.
         */
        public Builder(@NonNull ParcelFileDescriptor fileDescriptor) {
            Preconditions.checkNotNull(fileDescriptor, "File descriptor can't be null.");
            mInternalBuilder.setParcelFileDescriptor(fileDescriptor);
        }

        /**
         * Sets the limit for the file length in bytes.
         *
         * <p>When used to
         * {@link Recorder#prepareRecording(android.content.Context, FileDescriptorOutputOptions)
         * generate} recording, if the specified file size limit is reached while the recording
         * is being recorded, the recording will be finalized with
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

        /** Builds the {@link FileDescriptorOutputOptions} instance. */
        @Override
        @NonNull
        public FileDescriptorOutputOptions build() {
            return new FileDescriptorOutputOptions(mInternalBuilder.build());
        }
    }

    @AutoValue
    abstract static class FileDescriptorOutputOptionsInternal {
        @NonNull
        abstract ParcelFileDescriptor getParcelFileDescriptor();
        abstract long getFileSizeLimit();

        @AutoValue.Builder
        abstract static class Builder {
            @NonNull
            abstract Builder setParcelFileDescriptor(
                    @NonNull ParcelFileDescriptor parcelFileDescriptor);
            @NonNull
            abstract Builder setFileSizeLimit(long fileSizeLimitBytes);
            @NonNull
            abstract FileDescriptorOutputOptionsInternal build();
        }
    }
}
