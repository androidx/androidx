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

/**
 * A result of exporting the credentials to the provider
 *
 * @property credentialType type of credential. Includes, but not limited to, [CredentialTypes]
 * @property numSuccess the number of credentials successfully stored
 * @property numFailure the number of credentials failed to store because they are invalid
 * @property numIgnored the number of credentials ignored because they require additional user input
 */
public class PerTypeExportResult(
    public val credentialType: String,
    public val numSuccess: Int,
    public val numFailure: Int,
    public val numIgnored: Int,
)
