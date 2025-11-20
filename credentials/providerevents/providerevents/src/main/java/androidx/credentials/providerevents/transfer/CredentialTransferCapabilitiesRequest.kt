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
 * A request for the state of the provider's credentials that can be exported
 *
 * @property requestJson the request according to the
 *   [Fido Credential Exchange Protocol format](https://fidoalliance.org/specs/cx/cxp-v1.0-wd-20240522.html)
 * @throws IllegalArgumentException If [requestJson] is empty, or if it is not a valid JSON
 */
public class CredentialTransferCapabilitiesRequest(public val requestJson: String) {
    init {
        require(RequestValidationHelper.isValidJSON(requestJson)) {
            "requestJson must not be empty, and must be a valid JSON"
        }
    }
}
