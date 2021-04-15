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

    public static final int FILE_SIZE_UNLIMITED = 0;

    Type mType;

    public OutputOptions(@NonNull Type type) {
        mType = type;
    }

    /**
     * To be used to cast OutputOptions to subtype.
     */
    Type getType() {
        return mType;
    }

    /**
     * Gets the limit for the file length in bytes.
     */
    public abstract int getFileSizeLimit();

    /**
     * Types of the output options.
     */
    enum Type {
        FILE, FILE_DESCRIPTOR, MEDIA_STORE
    }
}
