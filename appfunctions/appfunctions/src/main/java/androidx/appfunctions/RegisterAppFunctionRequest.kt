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
import java.util.concurrent.Executor

/**
 * A request to register a [CallbackAppFunction] implementation, provided to
 * [AppFunctionManager.registerAppFunctions].
 *
 * This encapsulates the information needed to register a single app function.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RegisterAppFunctionRequest(
    /** The unique identifier of the app function. */
    public val functionIdentifier: String,
    /** The [Executor] on which the function will be invoked. */
    public val executor: Executor,
    /** The implementation of the app function. */
    public val appFunction: CallbackAppFunction,
)
