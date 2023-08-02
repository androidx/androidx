/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import androidx.credentials.exceptions.domerrors.AbortError
import androidx.credentials.exceptions.domerrors.ConstraintError
import androidx.credentials.exceptions.domerrors.DataCloneError
import androidx.credentials.exceptions.domerrors.DataError
import androidx.credentials.exceptions.domerrors.DomError
import androidx.credentials.exceptions.domerrors.EncodingError
import androidx.credentials.exceptions.domerrors.HierarchyRequestError
import androidx.credentials.exceptions.domerrors.InUseAttributeError
import androidx.credentials.exceptions.domerrors.InvalidCharacterError
import androidx.credentials.exceptions.domerrors.InvalidModificationError
import androidx.credentials.exceptions.domerrors.InvalidNodeTypeError
import androidx.credentials.exceptions.domerrors.InvalidStateError
import androidx.credentials.exceptions.domerrors.NamespaceError
import androidx.credentials.exceptions.domerrors.NetworkError
import androidx.credentials.exceptions.domerrors.NoModificationAllowedError
import androidx.credentials.exceptions.domerrors.NotAllowedError
import androidx.credentials.exceptions.domerrors.NotFoundError
import androidx.credentials.exceptions.domerrors.NotReadableError
import androidx.credentials.exceptions.domerrors.NotSupportedError
import androidx.credentials.exceptions.domerrors.OperationError
import androidx.credentials.exceptions.domerrors.OptOutError
import androidx.credentials.exceptions.domerrors.QuotaExceededError
import androidx.credentials.exceptions.domerrors.ReadOnlyError
import androidx.credentials.exceptions.domerrors.SecurityError
import androidx.credentials.exceptions.domerrors.SyntaxError
import androidx.credentials.exceptions.domerrors.TimeoutError
import androidx.credentials.exceptions.domerrors.TransactionInactiveError
import androidx.credentials.exceptions.domerrors.UnknownError
import androidx.credentials.exceptions.domerrors.VersionError
import androidx.credentials.exceptions.domerrors.WrongDocumentError
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * An internal class that parses dom exceptions originating from providers.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class DomExceptionUtils {
    companion object {

        const val SEPARATOR = "/"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal inline fun <reified T>
            generateDomException(type: String, prefix: String, msg: String?, t: T): T {

            return when (type) {
                prefix + AbortError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_ABORT_ERROR ->
                    generateException(AbortError(), msg, t)
                prefix + ConstraintError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_CONSTRAINT_ERROR ->
                    generateException(ConstraintError(), msg, t)
                prefix + DataCloneError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_DATA_CLONE_ERROR ->
                    generateException(DataCloneError(), msg, t)
                prefix + DataError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_DATA_ERROR ->
                    generateException(DataError(), msg, t)
                prefix + EncodingError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_ENCODING_ERROR ->
                    generateException(EncodingError(), msg, t)
                prefix + HierarchyRequestError
                    .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_HIERARCHY_REQUEST_ERROR ->
                    generateException(HierarchyRequestError(), msg, t)
                prefix + InUseAttributeError
                    .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_IN_USE_ATTRIBUTE_ERROR ->
                    generateException(InUseAttributeError(), msg, t)
                prefix + InvalidCharacterError
                    .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_INVALID_CHARACTER_ERROR ->
                    generateException(InvalidCharacterError(), msg, t)
                prefix + InvalidModificationError
                    .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_INVALID_MODIFICATION_ERROR ->
                    generateException(InvalidModificationError(), msg, t)
                prefix + InvalidNodeTypeError
                    .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_INVALID_NODE_TYPE_ERROR ->
                    generateException(InvalidNodeTypeError(), msg, t)
                prefix + InvalidStateError
                    .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_INVALID_STATE_ERROR ->
                    generateException(InvalidStateError(), msg, t)
                prefix + NamespaceError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_NAMESPACE_ERROR ->
                    generateException(NamespaceError(), msg, t)
                prefix + NetworkError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_NETWORK_ERROR ->
                    generateException(NetworkError(), msg, t)
                prefix + NoModificationAllowedError
                    .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_NO_MODIFICATION_ALLOWED_ERROR ->
                    generateException(NoModificationAllowedError(), msg, t)
                prefix + NotAllowedError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_NOT_ALLOWED_ERROR ->
                    generateException(NotAllowedError(), msg, t)
                prefix + NotFoundError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_NOT_FOUND_ERROR ->
                    generateException(NotFoundError(), msg, t)
                prefix + NotReadableError
                    .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_NOT_READABLE_ERROR ->
                    generateException(NotReadableError(), msg, t)
                prefix + NotSupportedError
                    .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_NOT_SUPPORTED_ERROR ->
                    generateException(NotSupportedError(), msg, t)
                prefix + OperationError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_OPERATION_ERROR ->
                    generateException(OperationError(), msg, t)
                prefix + OptOutError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_OPT_OUT_ERROR ->
                    generateException(OptOutError(), msg, t)
                prefix + QuotaExceededError
                    .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_QUOTA_EXCEEDED_ERROR ->
                    generateException(QuotaExceededError(), msg, t)
                prefix + ReadOnlyError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_READ_ONLY_ERROR ->
                    generateException(ReadOnlyError(), msg, t)
                prefix + SecurityError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_SECURITY_ERROR ->
                    generateException(SecurityError(), msg, t)
                prefix + SyntaxError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_SYNTAX_ERROR ->
                    generateException(SyntaxError(), msg, t)
                prefix + TimeoutError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_TIMEOUT_ERROR ->
                    generateException(TimeoutError(), msg, t)
                prefix + TransactionInactiveError
                    .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_TRANSACTION_INACTIVE_ERROR ->
                    generateException(TransactionInactiveError(), msg, t)
                prefix + UnknownError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_UNKNOWN_ERROR ->
                    generateException(UnknownError(), msg, t)
                prefix + VersionError.TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_VERSION_ERROR ->
                    generateException(VersionError(), msg, t)
                prefix + WrongDocumentError
                    .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_WRONG_DOCUMENT_ERROR ->
                    generateException(WrongDocumentError(), msg, t)
                else -> throw FrameworkClassParsingException()
            }
        }

        @Suppress("UNCHECKED_CAST") // Checked, worst case we throw a parsing exception
        private fun <T> generateException(domError: DomError, msg: String?, t: T): T {
            return when (t) {
                is CreatePublicKeyCredentialDomException -> {
                    CreatePublicKeyCredentialDomException(domError, msg) as T
                }
                is GetPublicKeyCredentialDomException -> {
                    GetPublicKeyCredentialDomException(domError, msg) as T
                }
                else -> {
                    throw FrameworkClassParsingException()
                }
            }
        }
    }
}
