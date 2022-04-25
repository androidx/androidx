/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.video.internal.encoder;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** An exception thrown to indicate an error has occurred during encoding. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class EncodeException extends Exception {
    /** Unknown error. */
    public static final int ERROR_UNKNOWN = 0;

    /** Error occurred during encoding. */
    public static final int ERROR_CODEC = 1;

    /** Describes the error that occurred during encoding. */
    @IntDef({ERROR_UNKNOWN, ERROR_CODEC})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ErrorType {}

    @ErrorType
    private final int mErrorType;

    public EncodeException(@ErrorType int errorType, @Nullable String message,
            @Nullable Throwable cause) {
        super(message, cause);
        mErrorType = errorType;
    }

    /**
     * Returns the encode error type, can have one of the following values:
     * {@link #ERROR_UNKNOWN}, {@link #ERROR_CODEC}
     */
    @ErrorType
    public int getErrorType() {
        return mErrorType;
    }
}
