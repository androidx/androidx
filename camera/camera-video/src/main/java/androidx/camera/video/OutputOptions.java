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

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Options for configuring output destination.
 */
public abstract class OutputOptions {

    /** Represents an unbound file size. */
    public static final int FILE_SIZE_UNLIMITED = 0;

    /** Output options of {@link FileOutputOptions}. */
    public static final int OPTIONS_TYPE_FILE = 0;
    /** Output options of {@link FileDescriptorOutputOptions}. */
    public static final int OPTIONS_TYPE_FILE_DESCRIPTOR = 1;
    /** Output options of {@link MediaStoreOutputOptions}. */
    public static final int OPTIONS_TYPE_MEDIA_STORE = 2;

    /** @hide */
    @IntDef({OPTIONS_TYPE_FILE, OPTIONS_TYPE_FILE_DESCRIPTOR, OPTIONS_TYPE_MEDIA_STORE})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface OptionsType {
    }

    @OptionsType
    private final int mType;

    OutputOptions(@OptionsType int type) {
        mType = type;
    }

    /**
     * Returns the subclass type of this output options.
     *
     * <p>Output options are limited to a distinct number of subclasses. Each subclass is
     * represented by a type. The type can be used to determine which class cast the output
     * options to in order to obtain more detailed information about the particular output
     * destination.
     *
     * @return the type of this output options.
     */
    @OptionsType
    public int getType() {
        return mType;
    }

    /**
     * Gets the limit for the file size in bytes.
     *
     * @return the file size limit in bytes.
     */
    public abstract long getFileSizeLimit();
}
