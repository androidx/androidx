/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef
import androidx.credentials.ClearCredentialStateRequest.Companion.TYPE_CLEAR_CREDENTIAL_STATE
import androidx.credentials.ClearCredentialStateRequest.Companion.TYPE_CLEAR_RESTORE_CREDENTIAL

/**
 * This allows verification when the user passes in the request type for their
 * [ClearCredentialStateRequest].
 *
 * If the request type is [TYPE_CLEAR_CREDENTIAL_STATE], then the request will be sent to the
 * credential providers to clear the user's credential state.
 *
 * If the request type is [TYPE_CLEAR_RESTORE_CREDENTIAL], then the request will be sent to the
 * restore credential provider to delete any stored [RestoreCredential].
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@StringDef(value = [TYPE_CLEAR_CREDENTIAL_STATE, TYPE_CLEAR_RESTORE_CREDENTIAL])
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
annotation class ClearCredentialRequestTypes
