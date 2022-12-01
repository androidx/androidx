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

import CreatePublicKeyCredentialDataCloneException
import CreatePublicKeyCredentialHierarchyRequestException
import CreatePublicKeyCredentialInUseAttributeException
import CreatePublicKeyCredentialInvalidCharacterException
import CreatePublicKeyCredentialInvalidModificationException
import CreatePublicKeyCredentialInvalidNodeTypeException
import CreatePublicKeyCredentialNamespaceException
import CreatePublicKeyCredentialNoModificationAllowedException
import CreatePublicKeyCredentialNotFoundException
import CreatePublicKeyCredentialOperationException
import CreatePublicKeyCredentialOptOutException
import CreatePublicKeyCredentialQuotaExceededException
import CreatePublicKeyCredentialReadOnlyException
import CreatePublicKeyCredentialSyntaxException
import CreatePublicKeyCredentialTransactionInactiveException
import CreatePublicKeyCredentialVersionException
import CreatePublicKeyCredentialWrongDocumentException
import androidx.annotation.RestrictTo
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialException

/**
 * A subclass of CreateCredentialException for unique errors specific only to PublicKeyCredentials.
 * See [CredentialManager] for more details on how Credentials work for Credential Manager flows.
 * See [GMS Error Codes](https://developers.google.com/android/reference/com/google/android/gms/fido/fido2/api/common/ErrorCode)
 * for more details on some of the subclasses.
 *
 * @see CredentialManager
 * @see CreatePublicKeyCredentialInterruptedException
 * @see CreatePublicKeyCredentialUnknownException
 * @see CreatePublicKeyCredentialNotReadableException
 * @see CreatePublicKeyCredentialAbortException
 * @see CreatePublicKeyCredentialConstraintException
 * @see CreatePublicKeyCredentialDataException
 * @see CreatePublicKeyCredentialEncodingException
 * @see CreatePublicKeyCredentialInvalidStateException
 * @see CreatePublicKeyCredentialNetworkException
 * @see CreatePublicKeyCredentialNotAllowedException
 * @see CreatePublicKeyCredentialNotSupportedException
 * @see CreatePublicKeyCredentialSecurityException
 * @see CreatePublicKeyCredentialTimeoutException
 * @see CreatePublicKeyCredentialDataCloneException
 * @see CreatePublicKeyCredentialHierarchyRequestException
 * @see CreatePublicKeyCredentialInUseAttributeException
 * @see CreatePublicKeyCredentialInvalidCharacterException
 * @see CreatePublicKeyCredentialInvalidModificationException
 * @see CreatePublicKeyCredentialInvalidNodeTypeException
 * @see CreatePublicKeyCredentialNamespaceException
 * @see CreatePublicKeyCredentialNoModificationAllowedException
 * @see CreatePublicKeyCredentialNotFoundException
 * @see CreatePublicKeyCredentialOperationException
 * @see CreatePublicKeyCredentialOptOutException
 * @see CreatePublicKeyCredentialQuotaExceededException
 * @see CreatePublicKeyCredentialReadOnlyException
 * @see CreatePublicKeyCredentialSyntaxException
 * @see CreatePublicKeyCredentialTransactionInactiveException
 * @see CreatePublicKeyCredentialVersionException
 * @see CreatePublicKeyCredentialWrongDocumentException
 *
 * @property errorMessage a human-readable string that describes the error
 * @throws NullPointerException if [type] is null
 * @throws IllegalArgumentException if [type] is empty
 * @hide
 */
open class CreatePublicKeyCredentialException @JvmOverloads internal constructor(
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val type: String,
    errorMessage: CharSequence? = null
) : CreateCredentialException(type, errorMessage) {
    init {
        require(type.isNotEmpty()) { "type must not be empty" }
    }
}