/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.car.app.mediaextensions.analytics.event;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;

/**
 * Analytics event indicating a parsing error.
 */
public class ErrorEvent extends AnalyticsEvent{

    /** Indicates an invalid intent*/
    public static final int ERROR_CODE_INVALID_EXTRAS = 0;
    /** Indicates an invalid bundle*/
    public static final int ERROR_CODE_INVALID_BUNDLE = 1;
    /** Indicates invalid event */
    public static final int ERROR_CODE_INVALID_EVENT = 2;

    @Retention(SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef (
            value = {ERROR_CODE_INVALID_EXTRAS, ERROR_CODE_INVALID_BUNDLE, ERROR_CODE_INVALID_EVENT}
    )
    public @interface ErrorCode {}

    private @ErrorCode int mErrorCode;

    @RestrictTo(LIBRARY)
    public ErrorEvent(@NonNull Bundle eventBundle, @ErrorCode int errorCode) {
        super(eventBundle, EVENT_TYPE_ERROR_EVENT);
        mErrorCode = errorCode;
    }

    /**
     * Returns error code
     */
    public @ErrorCode int getErrorCode() {
        return mErrorCode;
    }

    @Override
    public @NonNull String toString() {
        return "ErrorEvent{"
                + "mErrorCode="
                + mErrorCode
                + '}';
    }
}
