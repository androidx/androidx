/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions

import android.os.Bundle
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * An exception that is thrown when an error occurs during an app function execution.
 *
 * This exception can be used by the app to report errors to the caller.
 */
public abstract class AppFunctionException
internal constructor(
    /** The error code. */
    @ErrorCode internal val internalErrorCode: Int,
    /** The error message. */
    public val errorMessage: String?,
    internal val extras: Bundle
) : Exception(errorMessage) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toPlatformExtensionsClass():
        com.android.extensions.appfunctions.AppFunctionException {
        return com.android.extensions.appfunctions.AppFunctionException(
            internalErrorCode,
            errorMessage,
            extras
        )
    }

    /**
     * Returns the error category.
     *
     * <p> This method categorizes errors based on their underlying cause, allowing developers to
     * implement targeted error handling and provide more informative error messages to users. It
     * maps ranges of error codes to specific error categories.
     *
     * <p> This method returns [ERROR_CATEGORY_UNKNOWN] if the error code does not belong to any
     * error category.
     */
    @ErrorCategory
    internal val errorCategory: Int =
        when (internalErrorCode) {
            in 1000..1999 -> ERROR_CATEGORY_REQUEST_ERROR
            in 2000..2999 -> ERROR_CATEGORY_SYSTEM
            in 3000..3999 -> ERROR_CATEGORY_APP
            else -> ERROR_CATEGORY_UNKNOWN
        }

    @IntDef(
        value =
            [
                ERROR_CATEGORY_UNKNOWN,
                ERROR_CATEGORY_REQUEST_ERROR,
                ERROR_CATEGORY_APP,
                ERROR_CATEGORY_SYSTEM,
            ]
    )
    internal annotation class ErrorCategory

    @IntDef(
        value =
            [
                ERROR_DENIED,
                ERROR_INVALID_ARGUMENT,
                ERROR_DISABLED,
                ERROR_FUNCTION_NOT_FOUND,
                ERROR_RESOURCE_NOT_FOUND,
                ERROR_LIMIT_EXCEEDED,
                ERROR_RESOURCE_ALREADY_EXISTS,
                ERROR_SYSTEM_ERROR,
                ERROR_CANCELLED,
                ERROR_APP_UNKNOWN_ERROR,
                ERROR_PERMISSION_REQUIRED,
                ERROR_NOT_SUPPORTED,
            ]
    )
    internal annotation class ErrorCode

    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @SuppressWarnings("WrongConstant")
        public fun fromPlatformExtensionsClass(
            exception: com.android.extensions.appfunctions.AppFunctionException
        ): AppFunctionException {
            return when (exception.errorCode) {
                ERROR_DENIED -> AppFunctionDeniedException(exception.errorMessage, exception.extras)
                ERROR_INVALID_ARGUMENT ->
                    AppFunctionInvalidArgumentException(exception.errorMessage, exception.extras)
                ERROR_DISABLED ->
                    AppFunctionDisabledException(exception.errorMessage, exception.extras)
                ERROR_FUNCTION_NOT_FOUND ->
                    AppFunctionFunctionNotFoundException(exception.errorMessage, exception.extras)
                ERROR_RESOURCE_NOT_FOUND ->
                    AppFunctionElementNotFoundException(exception.errorMessage, exception.extras)
                ERROR_LIMIT_EXCEEDED ->
                    AppFunctionLimitExceededException(exception.errorMessage, exception.extras)
                ERROR_RESOURCE_ALREADY_EXISTS ->
                    AppFunctionElementAlreadyExistsException(
                        exception.errorMessage,
                        exception.extras
                    )
                ERROR_SYSTEM_ERROR ->
                    AppFunctionSystemUnknownException(exception.errorMessage, exception.extras)
                ERROR_CANCELLED ->
                    AppFunctionCancelledException(exception.errorMessage, exception.extras)
                ERROR_APP_UNKNOWN_ERROR ->
                    AppFunctionAppUnknownException(exception.errorMessage, exception.extras)
                ERROR_PERMISSION_REQUIRED ->
                    AppFunctionPermissionRequiredException(exception.errorMessage, exception.extras)
                ERROR_NOT_SUPPORTED ->
                    AppFunctionNotSupportedException(exception.errorMessage, exception.extras)
                else ->
                    AppFunctionUnknownException(
                        exception.errorCode,
                        exception.errorMessage,
                        exception.extras,
                    )
            }
        }

        // Error categories
        /** The error category is unknown. */
        internal const val ERROR_CATEGORY_UNKNOWN: Int = 0

        /**
         * The error is caused by the app requesting a function execution.
         *
         * <p>For example, the caller provided invalid parameters in the execution request e.g. an
         * invalid function ID.
         *
         * <p>Errors in the category fall in the range 1000-1999 inclusive.
         */
        internal const val ERROR_CATEGORY_REQUEST_ERROR: Int = 1

        /**
         * The error is caused by an issue in the system.
         *
         * <p>For example, the AppFunctionService implementation is not found by the system.
         *
         * <p>Errors in the category fall in the range 2000-2999 inclusive.
         */
        internal const val ERROR_CATEGORY_SYSTEM: Int = 2

        /**
         * The error is caused by the app providing the function.
         *
         * <p>For example, the app crashed when the system is executing the request.
         *
         * <p>Errors in the category fall in the range 3000-3999 inclusive.
         */
        internal const val ERROR_CATEGORY_APP: Int = 3

        // Error codes
        /**
         * The caller does not have the permission to execute an app function.
         *
         * <p> This is different from [ERROR_PERMISSION_REQUIRED] in that the caller is missing this
         * specific permission, as opposed to the target app missing a permission.
         *
         * <p>This error is in the [ERROR_CATEGORY_REQUEST_ERROR] category.
         */
        internal const val ERROR_DENIED: Int = 1000

        /**
         * The caller supplied invalid arguments to the execution request.
         *
         * <p>This error may be considered similar to [IllegalArgumentException].
         *
         * <p>This error is in the [ERROR_CATEGORY_REQUEST_ERROR] category.
         */
        internal const val ERROR_INVALID_ARGUMENT: Int = 1001

        /**
         * The caller tried to execute a disabled app function.
         *
         * <p>This error is in the [ERROR_CATEGORY_REQUEST_ERROR] category.
         */
        internal const val ERROR_DISABLED: Int = 1002

        /**
         * The caller tried to execute a function that does not exist.
         *
         * <p>This error is in the [ERROR_CATEGORY_REQUEST_ERROR] category.
         */
        internal const val ERROR_FUNCTION_NOT_FOUND: Int = 1003

        // SDK-defined error codes in the [ERROR_CATEGORY_REQUEST_ERROR] category start from 1500.
        /**
         * The caller tried to request a resource/entity that does not exist.
         *
         * <p>This error is in the [ERROR_CATEGORY_REQUEST_ERROR] category.
         */
        internal const val ERROR_RESOURCE_NOT_FOUND: Int = 1500

        /**
         * The caller exceeded the allowed request rate.
         *
         * <p>This error is in the [ERROR_CATEGORY_REQUEST_ERROR] category.
         */
        internal const val ERROR_LIMIT_EXCEEDED: Int = 1501

        /**
         * The caller tried to create a resource/entity that already exists or has conflicts with
         * existing resource/entity.
         *
         * <p>This error is in the [ERROR_CATEGORY_REQUEST_ERROR] category.
         */
        internal const val ERROR_RESOURCE_ALREADY_EXISTS: Int = 1502

        /**
         * An internal unexpected error coming from the system.
         *
         * <p>This error is in the [ERROR_CATEGORY_SYSTEM] category.
         */
        internal const val ERROR_SYSTEM_ERROR: Int = 2000

        /**
         * The operation was cancelled. Use this error code to report that a cancellation is done
         * after receiving a cancellation signal.
         *
         * <p>This error is in the [ERROR_CATEGORY_SYSTEM] category.
         */
        internal const val ERROR_CANCELLED: Int = 2001

        /**
         * An unknown error occurred while processing the call in the AppFunctionService.
         *
         * <p>This error is thrown when the service is connected in the remote application but an
         * unexpected error is thrown from the bound application.
         *
         * <p>This error is in the [ERROR_CATEGORY_APP] category.
         */
        internal const val ERROR_APP_UNKNOWN_ERROR: Int = 3000

        // SDK-defined error codes in the [ERROR_CATEGORY_APP] category start from 3500.
        /**
         * Indicates the app lacks the necessary permission to fulfill the request.
         *
         * <p>This occurs when the app attempts an operation requiring user-granted permission that
         * has not been provided. For example, creating a calendar event requires access to the
         * calendar content. If the user hasn't granted this permission, this error should be
         * thrown.
         *
         * <p> This is different from [ERROR_DENIED] in that the required permission is missing from
         * the target app, as opposed to the caller.
         *
         * <p>This error is in the [ERROR_CATEGORY_APP] category.
         */
        internal const val ERROR_PERMISSION_REQUIRED: Int = 3500

        /**
         * Indicates the action is not supported by the app.
         *
         * <p>This error occurs when an app receives a request to perform an unsupported action. For
         * example, a clock app might support updating timer properties such as label but may not
         * allow updating the timer's duration once the timer has already started.
         *
         * <p>This error is in the [ERROR_CATEGORY_APP] category.
         */
        internal const val ERROR_NOT_SUPPORTED: Int = 3501
    }
}

/**
 * Thrown when an unknown error has occurred.
 *
 * <p> This Exception is used when the error doesn't belong to any other AppFunctionException. This
 * may happen due to version skews in the error codes between the platform and the sdk. E.g. if the
 * app is running on a newer platform version (with a new error code) and an older sdk.
 *
 * <p>Note that this is different from [AppFunctionAppUnknownException], in that the error wasn't
 * necessarily caused by the app.
 */
public class AppFunctionUnknownException
internal constructor(public val errorCode: Int, errorMessage: String? = null, extras: Bundle) :
    AppFunctionException(errorCode, errorMessage, extras) {
    /**
     * Create an [AppFunctionUnknownException].
     *
     * @param errorCode The error code.
     * @param errorMessage The error message.
     */
    public constructor(
        errorCode: Int,
        errorMessage: String? = null
    ) : this(errorCode, errorMessage, Bundle.EMPTY)
}
