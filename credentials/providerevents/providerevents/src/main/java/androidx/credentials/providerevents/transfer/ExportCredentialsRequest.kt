/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.credentials.providerevents.transfer

import androidx.credentials.providerevents.internal.RequestValidationHelper

/**
 * a request to export a set of credentials into the provider
 *
 * @property credentialsJson the credentials that are delivered to the provider formatted as per
 *   [Fido Credential Exchange Format](https://fidoalliance.org/specs/cx/cxf-v1.0-rd-20250313.html)
 * @throws IllegalArgumentException If [credentialsJson] is empty, or if it is not a valid JSON
 */
public class ExportCredentialsRequest(public val credentialsJson: String) {
    init {
        require(RequestValidationHelper.isValidJSON(credentialsJson)) {
            "requestJson must not be empty, and must be a valid JSON"
        }
    }
}
