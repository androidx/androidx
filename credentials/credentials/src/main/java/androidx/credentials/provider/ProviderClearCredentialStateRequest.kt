/*
 * Copyright 2023 The Android Open Source Project
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

/**
 * Request class for clearing a user's credential state from the credential providers.
 *
 * @constructor constructs an instance of [ProviderClearCredentialStateRequest]
 *
 * @param callingAppInfo info pertaining to the calling app that's making the request
 *
 * @throws NullPointerException If [callingAppInfo] is null
 *
 * Note : Credential providers are not expected to utilize the constructor in this class for any
 * production flow. This constructor must only be used for testing purposes.
 */
class ProviderClearCredentialStateRequest constructor(val callingAppInfo: CallingAppInfo)
