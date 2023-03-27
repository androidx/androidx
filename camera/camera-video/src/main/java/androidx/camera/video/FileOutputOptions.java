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
import androidx.annotation.RequiresApi;
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
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class FileOutputOptions extends OutputOptions {

    private final FileOutputOptionsInternal mFileOutputOptionsInternal;

    FileOutputOptions(@NonNull FileOutputOptionsInternal fileOutputOptionsInternal) {
        super(fileOutputOptionsInternal);
        mFileOutputOptionsInternal = fileOutputOptionsInternal;
    }

    /** Gets the File instance */
    @NonNull
    public File getFile() {
        return mFileOutputOptionsInternal.getFile();
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
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static final class Builder extends OutputOptions.Builder<FileOutputOptions, Builder> {

        private final FileOutputOptionsInternal.Builder mInternalBuilder;

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
            super(new AutoValue_FileOutputOptions_FileOutputOptionsInternal.Builder());
            Preconditions.checkNotNull(file, "File can't be null.");
            mInternalBuilder = (FileOutputOptionsInternal.Builder) mRootInternalBuilder;
            mInternalBuilder.setFile(file);
        }

        /** Builds the {@link FileOutputOptions} instance. */
        @Override
        @NonNull
        public FileOutputOptions build() {
            return new FileOutputOptions(mInternalBuilder.build());
        }
    }

    @AutoValue
    abstract static class FileOutputOptionsInternal extends OutputOptions.OutputOptionsInternal {
        @NonNull
        abstract File getFile();

        @SuppressWarnings("NullableProblems") // Nullable problem in AutoValue generated class
        @AutoValue.Builder
        abstract static class Builder extends OutputOptions.OutputOptionsInternal.Builder<Builder> {
            @NonNull
            abstract Builder setFile(@NonNull File file);
            @Override
            @NonNull
            abstract FileOutputOptionsInternal build();
        }
    }
}
