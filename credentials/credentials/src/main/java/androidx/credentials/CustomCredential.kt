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

import android.os.Bundle

/**
 * Base class for a custom credential with which the user consented to authenticate to the app.
 *
 * If you get a [CustomCredential] instead of a type-safe credential class such as
 * [PasswordCredential], [PublicKeyCredential], etc., then you should check if
 * you have any other library at interest that supports this custom [type] of credential,
 * and if so use its parsing utilities to resolve to a type-safe class within that library.
 *
 *
 * Note: The Bundle keys for [data] should not be in the form of `androidx.credentials.*` as they
 * are reserved for internal use by this androidx library.
 *
 * @param type the credential type determined by the credential-type-specific subclass for custom
 * use cases
 * @param data the credential data in the [Bundle] format for custom use cases (note: bundle keys in
 * the form of `androidx.credentials.*` and `android.credentials.*` are reserved for internal
 * library usage)
 * @throws IllegalArgumentException If [type] is empty
 * @throws NullPointerException If [data] or [type] is null
 */
open class CustomCredential(
    type: String,
    data: Bundle
) : Credential(type, data) {

    init {
        require(type.isNotEmpty()) { "type should not be empty" }
    }
}