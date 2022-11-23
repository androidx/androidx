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

/**
 * During the create public key credential flow, this is returned when an authenticator response
 * exception contains a constraint_err code or equivalent, indicating that some mutation operation
 * occurring during a transaction failed by not satisfying constraints.
 *
 * @see CreatePublicKeyCredentialException
 *
 * @hide
 */
class CreatePublicKeyCredentialConstraintException @JvmOverloads constructor(
    errorMessage: CharSequence? = null
) : CreatePublicKeyCredentialException(
    TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_CONSTRAINT_EXCEPTION,
    errorMessage) {

    /** @hide */
    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_CONSTRAINT_EXCEPTION: String =
            "androidx.credentials.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL" +
                "_CONSTRAINT_EXCEPTION"
    }
}