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
package androidx.credentials.provider

import android.app.PendingIntent
import androidx.credentials.GetCredentialOption

/**
 * Request received by the provider after the query phase of the get flow is complete and the
 * user has made a selection from the list of [CredentialEntry] that was set on the
 * [BeginGetCredentialsProviderResponse].
 *
 * This request will be added to the intent that starts the activity invoked by the [PendingIntent]
 * set on the [CredentialEntry] that the user selected. The request can be extracted by using
 * the PendingIntentHandler.
 *
 * @property getCredentialOption an instance of [GetCredentialOption] that contains the credential
 * type request parameters for the final credential request
 * @property callingAppInfo information pertaining to the calling application
 *
 * @hide
 */
class GetCredentialProviderRequest internal constructor(
    val getCredentialOption: GetCredentialOption,
    val callingAppInfo: ApplicationInfo
    )