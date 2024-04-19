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

import androidx.annotation.IntDef
import androidx.credentials.PriorityHints.Companion.PRIORITY_DEFAULT
import androidx.credentials.PriorityHints.Companion.PRIORITY_OIDC_OR_SIMILAR
import androidx.credentials.PriorityHints.Companion.PRIORITY_PASSKEY_OR_SIMILAR
import androidx.credentials.PriorityHints.Companion.PRIORITY_PASSWORD_OR_SIMILAR

/**
 * For our [CredentialOption] types, this allows us to categorize the default priority hint for
 * those entries. We expect [GetCustomCredentialOption] to utilize, rarely, these differing
 * priorities. These are subject to change by library owners, but will always remain backwards
 * compatible, and will always ensure relative ordering of older sets are maintained.
 */
@Suppress("PublicTypedef") // Custom types are able to choose amongst 3 of these
                                   // values, without having to repeat the constants elsewhere
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@IntDef(value = [PRIORITY_PASSKEY_OR_SIMILAR, PRIORITY_OIDC_OR_SIMILAR,
    PRIORITY_PASSWORD_OR_SIMILAR, PRIORITY_DEFAULT])
annotation class PriorityHints {
    companion object {

        // Only allowed to be set under library owner requirements
        internal const val PRIORITY_PASSKEY_OR_SIMILAR = 100
        // Can be set by non library owners
        const val PRIORITY_OIDC_OR_SIMILAR = 500
        const val PRIORITY_PASSWORD_OR_SIMILAR = 1000
        const val PRIORITY_DEFAULT = 2000
    }
}
