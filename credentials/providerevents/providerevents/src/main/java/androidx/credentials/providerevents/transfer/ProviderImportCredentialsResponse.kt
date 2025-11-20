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

import androidx.credentials.provider.CallingAppInfo

/**
 * Response received by the provider after the transfer phase of the import flow is complete i.e.
 * the user has now made a selection from the list of [ExportEntry] presented on the selector UI,
 * and the provider returned credentials corresponding to the request.
 *
 * @property response a response of credential import
 * @property callingAppInfo the exporter's app info
 *
 * TODO(b/445237915): Replace callingAppInfo with attestation
 */
public class ProviderImportCredentialsResponse(
    public val response: ImportCredentialsResponse,
    public val callingAppInfo: CallingAppInfo,
)
