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

package androidx.appfunctions

import androidx.annotation.RestrictTo

/**
 * An implementation of an app function that supports suspend execution.
 *
 * This interface is expected as part of [HandleAppFunctionRequest].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface SuspendingAppFunction {
    /**
     * Called when the app function is invoked using [AppFunctionManager.executeAppFunction].
     * - If this method completes successfully, the returned [ExecuteAppFunctionResponse] is
     *   delivered to the caller.
     * - If this method throws an [AppFunctionException], it is caught and returned to the caller as
     *   an [ExecuteAppFunctionResponse.Error] wrapping the exception.
     * - If execution is cancelled via a platform cancellation signal, an
     *   [AppFunctionCancelledException] error response is delivered to the caller.
     * - If this method throws any other uncaught exception (such as [RuntimeException]), an
     *   [AppFunctionAppUnknownException] error response is reported to the caller, and the
     *   exception is rethrown to unregister the app function and terminate its handling coroutine
     *   scope.
     *
     * @param request The request details.
     * @return The execution response result.
     */
    public suspend fun executeAppFunction(
        request: ExecuteAppFunctionRequest
    ): ExecuteAppFunctionResponse
}
