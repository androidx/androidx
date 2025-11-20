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

package androidx.credentials.providerevents.signal

import androidx.credentials.SignalCredentialStateRequest
import androidx.credentials.provider.CallingAppInfo

/**
 * Signal credential state request received by the provider
 *
 * This request contains the actual request coming from the calling app, and the application
 * information associated with the calling app.
 *
 * @property callingRequest the complete [androidx.credentials.SignalCredentialStateRequest] coming
 *   from the calling app that is sending the credential state signal
 * @property callingAppInfo information pertaining to the calling app making the request
 */
public class ProviderSignalCredentialStateRequest(
    public val callingRequest: SignalCredentialStateRequest,
    public val callingAppInfo: CallingAppInfo,
)
