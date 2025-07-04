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

/**
 * Thrown when an error is caused by the app providing the function.
 *
 * For example, the app crashed when the system is executing the request.
 */
public abstract class AppFunctionAppException
internal constructor(errorCode: Int, errorMessage: String? = null, extras: Bundle) :
    AppFunctionException(errorCode, errorMessage, extras)

/**
 * Thrown when an unknown error occurred while processing the call in the AppFunctionService.
 *
 * This error is thrown when the service is connected in the remote application but an unexpected
 * error is thrown from the bound application.
 */
public class AppFunctionAppUnknownException
internal constructor(errorMessage: String? = null, extras: Bundle) :
    AppFunctionAppException(ERROR_APP_UNKNOWN_ERROR, errorMessage, extras) {

    public constructor(errorMessage: String? = null) : this(errorMessage, Bundle.EMPTY)
}

/**
 * Thrown when the app lacks the necessary permission to fulfill the request.
 *
 * This occurs when the app attempts an operation requiring user-granted permission that has not
 * been provided. For example, creating a calendar event requires access to the calendar content. If
 * the user hasn't granted this permission, this error should be thrown.
 *
 * This is different from [AppFunctionDeniedException] in that the required permission is missing
 * from the target app, as opposed to the caller.
 */
public class AppFunctionPermissionRequiredException
internal constructor(errorMessage: String? = null, extras: Bundle) :
    AppFunctionAppException(ERROR_PERMISSION_REQUIRED, errorMessage, extras) {

    public constructor(errorMessage: String? = null) : this(errorMessage, Bundle.EMPTY)
}

/**
 * Thrown when an app receives a request to perform an unsupported action.
 *
 * For example, a clock app might support updating timer properties such as label but may not allow
 * updating the timer's duration once the timer has already started.
 */
public class AppFunctionNotSupportedException
internal constructor(errorMessage: String? = null, extras: Bundle) :
    AppFunctionAppException(ERROR_NOT_SUPPORTED, errorMessage, extras) {

    public constructor(errorMessage: String? = null) : this(errorMessage, Bundle.EMPTY)
}
