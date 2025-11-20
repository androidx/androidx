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
 * Thrown when the error is caused by the app requesting a function execution.
 *
 * For example, the caller provided invalid parameters in the execution request e.g. an invalid
 * function ID.
 */
public abstract class AppFunctionRequestException
internal constructor(errorCode: Int, errorMessage: String? = null, extras: Bundle) :
    AppFunctionException(errorCode, errorMessage, extras)

/**
 * Thrown when the caller does not have the permission to execute an app function.
 *
 * This is different from [AppFunctionPermissionRequiredException] in that the caller is missing
 * this specific permission, as opposed to the target app missing a permission.
 */
public class AppFunctionDeniedException
internal constructor(errorMessage: String? = null, extras: Bundle) :
    AppFunctionRequestException(ERROR_DENIED, errorMessage, extras) {

    public constructor(errorMessage: String? = null) : this(errorMessage, Bundle.EMPTY)
}

/**
 * Thrown when the caller supplied invalid arguments to [ExecuteAppFunctionRequest]'s parameters.
 *
 * This error may be considered similar to [IllegalArgumentException].
 */
public class AppFunctionInvalidArgumentException
internal constructor(errorMessage: String? = null, extras: Bundle) :
    AppFunctionRequestException(ERROR_INVALID_ARGUMENT, errorMessage, extras) {

    public constructor(errorMessage: String? = null) : this(errorMessage, Bundle.EMPTY)
}

/**
 * Thrown when the caller tried to execute a disabled app function. An app function can be enabled
 * at runtime through the AppFunctionManager or by setting
 * [androidx.appfunctions.service.AppFunction.enabledByDefault] to true in the
 * [androidx.appfunctions.service.AppFunction] annotation.
 */
public class AppFunctionDisabledException
internal constructor(errorMessage: String? = null, extras: Bundle) :
    AppFunctionRequestException(ERROR_DISABLED, errorMessage, extras) {

    public constructor(errorMessage: String? = null) : this(errorMessage, Bundle.EMPTY)
}

/** Thrown when the caller tries to execute a function that does not exist. */
public class AppFunctionFunctionNotFoundException
internal constructor(errorMessage: String? = null, extras: Bundle) :
    AppFunctionRequestException(ERROR_FUNCTION_NOT_FOUND, errorMessage, extras) {

    public constructor(errorMessage: String? = null) : this(errorMessage, Bundle.EMPTY)
}

/** Thrown when the caller tried to request a resource/entity that does not exist. */
public class AppFunctionElementNotFoundException
internal constructor(errorMessage: String? = null, extras: Bundle) :
    AppFunctionRequestException(ERROR_RESOURCE_NOT_FOUND, errorMessage, extras) {

    public constructor(errorMessage: String? = null) : this(errorMessage, Bundle.EMPTY)
}

/** Thrown when the caller exceeded the allowed request rate. */
public class AppFunctionLimitExceededException
internal constructor(errorMessage: String? = null, extras: Bundle) :
    AppFunctionRequestException(ERROR_LIMIT_EXCEEDED, errorMessage, extras) {

    public constructor(errorMessage: String? = null) : this(errorMessage, Bundle.EMPTY)
}

/**
 * Thrown when the caller tried to create a resource/entity that already exists or has conflicts
 * with existing resource/entity.
 */
public class AppFunctionElementAlreadyExistsException
internal constructor(errorMessage: String? = null, extras: Bundle) :
    AppFunctionRequestException(ERROR_RESOURCE_ALREADY_EXISTS, errorMessage, extras) {

    public constructor(errorMessage: String? = null) : this(errorMessage, Bundle.EMPTY)
}
