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
 * {@link PendingRecording#start(java.util.concurrent.Executor, androidx.core.util.Consumer)} returns. Application should not use the file referenced by
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
        super(fileDescriptorOutputOptionsInternal);
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
    public static final class Builder extends
            OutputOptions.Builder<FileDescriptorOutputOptions, Builder> {

        private final FileDescriptorOutputOptionsInternal.Builder mInternalBuilder;

        /**
         * Creates a builder of the {@link FileDescriptorOutputOptions} with a file descriptor.
         *
         * @param fileDescriptor the file descriptor to use as the output destination.
         */
        public Builder(@NonNull ParcelFileDescriptor fileDescriptor) {
            super(new AutoValue_FileDescriptorOutputOptions_FileDescriptorOutputOptionsInternal
                    .Builder());
            Preconditions.checkNotNull(fileDescriptor, "File descriptor can't be null.");
            mInternalBuilder = (FileDescriptorOutputOptionsInternal.Builder) mRootInternalBuilder;
            mInternalBuilder.setParcelFileDescriptor(fileDescriptor);
        }

        /** Builds the {@link FileDescriptorOutputOptions} instance. */
        @Override
        @NonNull
        public FileDescriptorOutputOptions build() {
            return new FileDescriptorOutputOptions(mInternalBuilder.build());
        }
    }

    @AutoValue
    abstract static class FileDescriptorOutputOptionsInternal extends OutputOptionsInternal {
        @NonNull
        abstract ParcelFileDescriptor getParcelFileDescriptor();

        @SuppressWarnings("NullableProblems") // Nullable problem in AutoValue generated class
        @AutoValue.Builder
        abstract static class Builder extends OutputOptionsInternal.Builder<Builder> {
            @NonNull
            abstract Builder setParcelFileDescriptor(
                    @NonNull ParcelFileDescriptor parcelFileDescriptor);
            @Override
            @NonNull
            abstract FileDescriptorOutputOptionsInternal build();
        }
    }
}
