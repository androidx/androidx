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

package androidx.credentials.exceptions.publickeycredential

import androidx.annotation.VisibleForTesting
import androidx.credentials.exceptions.domerrors.AbortError
import androidx.credentials.exceptions.domerrors.DomError

/**
 * During the create public key credential flow, this is thrown when a DOM Exception is thrown,
 * indicating the operation contains a DOMException error type. The fido spec can be found
 * [here](https://webidl.spec.whatwg.org/#idl-DOMException-error-names). To see the full list of
 * implemented DOMErrors, please see the API docs associated with this package. For example, one
 * such error is [AbortError].
 *
 * @property domError the specific error from the DOMException types defined in the fido spec found
 * [here](https://webidl.spec.whatwg.org/#idl-DOMException-error-names)
 * @throws NullPointerException If [domError] is null
 *
 * @see CreatePublicKeyCredentialException
 *
 */
class CreatePublicKeyCredentialDomException @JvmOverloads constructor(
    val domError: DomError,
    errorMessage: CharSequence? = null
) : CreatePublicKeyCredentialException(
    TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION + domError.type,
    errorMessage) {
    /** @hide */
    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
        const val TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION: String =
            "androidx.credentials.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION"
    }
}