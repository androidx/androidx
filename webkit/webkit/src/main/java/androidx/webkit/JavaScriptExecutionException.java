/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.webkit;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import org.chromium.support_lib_boundary.ExecuteJavaScriptCallbackBoundaryInterface.ExecuteJavaScriptExceptionTypeBoundaryInterface;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exception thrown when there was an error executing JavaScript via {@link
 * JavaScriptReplyProxy#executeJavaScript(String, OutcomeReceiverCompat)}.
 */
public class JavaScriptExecutionException extends Exception {
    /** Error code for a generic/unknown error occurred while executing JavaScript. */
    public static final int ERROR_GENERIC =
            ExecuteJavaScriptExceptionTypeBoundaryInterface.GENERIC;

    /**
     * Error code for the web frame that was executing JavaScript was destroyed before execution.
     */
    public static final int ERROR_FRAME_DESTROYED =
            ExecuteJavaScriptExceptionTypeBoundaryInterface.FRAME_DESTROYED;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(value = {ERROR_GENERIC, ERROR_FRAME_DESTROYED})
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface ErrorType {
    }

    private final int mErrorType;

    /**
     * Creates a new JavaScriptExecutionException with the specified error type and message.
     *
     * @param errorType The type of error that occurred.
     * @param message   A detailed error message, if available.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public JavaScriptExecutionException(@ErrorType int errorType, @Nullable String message) {
        super(message == null ? "" : message);
        mErrorType = errorType;
    }

    /**
     * @return the type of error that caused this exception. See {@link ERROR_FRAME_DESTROYED}
     * and {@link ERROR_GENERIC}.
     */
    public @ErrorType int getErrorType() {
        return mErrorType;
    }
}
