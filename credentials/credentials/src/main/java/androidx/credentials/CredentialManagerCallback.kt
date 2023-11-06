/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.credentials

/**
 * Callback interface intended for use when an asynchronous Credential Manager operation may result
 * in a failure.
 *
 * This interface may be used in cases where an asynchronous Credential Manager API may complete
 * either with a value, or an exception.
 *
 * @param R the type of the result that's being sent
 * @param E the type of the exception being returned
 */
interface CredentialManagerCallback<R : Any?, E : Any> {
    fun onResult(result: R)

    fun onError(e: E)
}
