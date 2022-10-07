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
 * Base request class for getting a registered public key credential.
 *
 * @property requestJson the request in JSON format
 * @throws NullPointerException If [requestJson] is null - auto handled by the
 * Kotlin runtime
 * @throws IllegalArgumentException If [requestJson] is empty
 *
 * @hide
 */
abstract class GetPublicKeyCredentialBaseOption constructor(
    val requestJson: String
) : GetCredentialOption() {

    init {
        require(requestJson.isNotEmpty()) { "request json must not be empty" }
    }
}