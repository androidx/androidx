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

package androidx.car.app;

import static java.util.Objects.requireNonNull;

import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.serialization.BundlerException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.InvalidParameterException;

/**
 * Denotes a failure in the client to a host request.
 *
 * <p>This is used for the failure response for a IOnDoneCallback.
 */
public class FailureResponse {
    /**
     * The exception type of the failure.
     *
     * @hide
     */
    @IntDef(
            value = {
                    UNKNOWN_ERROR,
                    BUNDLER_EXCEPTION,
                    ILLEGAL_STATE_EXCEPTION,
                    INVALID_PARAMETER_EXCEPTION,
                    SECURITY_EXCEPTION,
                    RUNTIME_EXCEPTION,
                    REMOTE_EXCEPTION
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ErrorType {
    }

    public static final int UNKNOWN_ERROR = 0;
    public static final int BUNDLER_EXCEPTION = 1;
    public static final int ILLEGAL_STATE_EXCEPTION = 2;
    public static final int INVALID_PARAMETER_EXCEPTION = 3;
    public static final int SECURITY_EXCEPTION = 4;
    public static final int RUNTIME_EXCEPTION = 5;
    public static final int REMOTE_EXCEPTION = 6;

    @Nullable
    private final String mStackTrace;
    @ErrorType
    private final int mErrorType;

    public FailureResponse(@NonNull Throwable exception) {
        this.mStackTrace = Log.getStackTraceString(requireNonNull(exception));
        if (exception instanceof BundlerException) {
            mErrorType = BUNDLER_EXCEPTION;
        } else if (exception instanceof IllegalStateException) {
            mErrorType = ILLEGAL_STATE_EXCEPTION;
        } else if (exception instanceof InvalidParameterException) {
            mErrorType = INVALID_PARAMETER_EXCEPTION;
        } else if (exception instanceof SecurityException) {
            mErrorType = SECURITY_EXCEPTION;
        } else if (exception instanceof RuntimeException) {
            mErrorType = RUNTIME_EXCEPTION;
        } else if (exception instanceof RemoteException) {
            mErrorType = REMOTE_EXCEPTION;
        } else {
            mErrorType = UNKNOWN_ERROR;
        }
    }

    // Used for serialization.
    public FailureResponse() {
        mStackTrace = null;
        mErrorType = UNKNOWN_ERROR;
    }

    @NonNull
    public String getStackTrace() {
        return requireNonNull(mStackTrace);
    }

    @ErrorType
    public int getErrorType() {
        return mErrorType;
    }
}
