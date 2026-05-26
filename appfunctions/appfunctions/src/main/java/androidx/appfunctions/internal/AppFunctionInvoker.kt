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

package androidx.appfunctions.internal

import androidx.annotation.RestrictTo
import androidx.appfunctions.AppFunctionContext

/**
 * An interface for invoking app functions.
 *
 * This interface defines a contract for invoking a set of AppFunctions declared in an application.
 * Each AppFunction implementation class has a corresponding generated Invoker class that implements
 * this interface.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AppFunctionInvoker {
    /**
     * Sets of function ids that the invoker supports.
     *
     * For example, consider the following AppFunction implementation class:
     * ```kotlin
     * class NoteFunctions : CreateNote, EditNote {
     *   @AppFunction
     *   override suspend fun createNote() : Note { ... }
     *
     *   @AppFunction
     *   override suspend fun editNote() : Note { ... }
     * }
     * ```
     *
     * The set of supported Ids would include the respective function id for the `createNote` and
     * `editNote` functions.
     */
    public val supportedFunctionIds: Set<String>

    /**
     * Invokes an AppFunction identified by [functionIdentifier], with [parameters].
     *
     * @throws [androidx.appfunctions.AppFunctionFunctionNotFoundException] if [functionIdentifier]
     *   does not exist.
     * @throws [androidx.appfunctions.AppFunctionInvalidArgumentException] if [parameters] is
     *   invalid.
     */
    public suspend fun unsafeInvoke(
        appFunctionContext: AppFunctionContext,
        functionIdentifier: String,
        parameters: Map<String, Any?>,
    ): Any?
}
