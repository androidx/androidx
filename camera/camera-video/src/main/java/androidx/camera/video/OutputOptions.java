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
 * Options for configuring output destination.
 */
public abstract class OutputOptions {

    /** Represents an unbound file size. */
    public static final int FILE_SIZE_UNLIMITED = 0;

    private final Type mType;

    OutputOptions(@NonNull Type type) {
        mType = type;
    }

    /**
     * Returns the subclass type of this output options.
     *
     * <p>The type can be used to determine which class cast the output options to in order to
     * obtain more detailed information about the particular output destination.
     * @see Type
     */
    @NonNull
    public Type getType() {
        return mType;
    }

    /**
     * Gets the limit for the file length in bytes.
     */
    public abstract long getFileSizeLimit();

    /**
     * Type of the output options.
     *
     * <p>Output options are limited to a distinct number of subclasses. Each subclass is
     * represented by a type.
     */
    public enum Type {
        /** Output options of {@link FileOutputOptions}. */
        FILE,
        /** Output options of {@link FileDescriptorOutputOptions}. */
        FILE_DESCRIPTOR,
        /** Output options of {@link MediaStoreOutputOptions}. */
        MEDIA_STORE
    }
}
