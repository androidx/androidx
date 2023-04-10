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

package androidx.credentials.exceptions

import androidx.credentials.CredentialManager

/**
 * Represents a custom error thrown during a clear flow with Credential Manager. See
 * [CredentialManager] for more details on how Exceptions work for Credential Manager flows.
 *
 * @see CredentialManager
 *
 * @property type a string that indicates the type of the credential exception
 * @throws IllegalArgumentException If [type] is empty
 * @throws NullPointerException If [type] is null
 */
open class ClearCustomCredentialException @JvmOverloads constructor(
    override val type: String,
    errorMessage: CharSequence? = null
) : ClearCredentialException(type, errorMessage) {
    init {
        require(type.isNotEmpty()) { "type must not be empty" }
    }
}