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
 * An adapter for an interface annotated with `@AppFunctionSignature`.
 *
 * @param T The type of the interface annotated with `@AppFunctionSignature`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AppFunctionAdapter<T : Any> {
    /**
     * Gets the unique identifier of the app function.
     *
     * @return The unique identifier of the app function.
     */
    public fun getFunctionId(): String

    /**
     * Creates a [HandleAppFunctionRequest] for the given [instance].
     *
     * @param instance The implementation of the app function interface.
     * @return The [HandleAppFunctionRequest] wrapping the [instance].
     */
    public fun adapt(instance: T): HandleAppFunctionRequest
}
