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

package androidx.credentials.exceptions.domerrors

/**
 * This is thrown when the create public key credential operation failed with no more detailed
 * information. This could be something such as out of memory or some other transient reason -
 * either from fido directly or through the public key credential flow in general. The fido spec
 * can be found [here](https://webidl.spec.whatwg.org/#idl-DOMException-error-names).
 */
@Suppress("ExtendsError") // This is not a real java `Error`
class UnknownError :
    DomError(TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_UNKNOWN_ERROR) {
    internal companion object {
        internal const val TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_UNKNOWN_ERROR: String =
            "androidx.credentials.TYPE_UNKNOWN_ERROR"
    }
}
