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

package androidx.wear.phone.interactions.samples

import androidx.annotation.Sampled
import androidx.wear.phone.interactions.authentication.OAuthRequest
import androidx.wear.phone.interactions.authentication.RemoteAuthClient

@Sampled
suspend fun AuthAvailabilitySample(
    remoteAuthClient: RemoteAuthClient,
    oAuthRequest: OAuthRequest,
    myAuthCallback: RemoteAuthClient.Callback
) {
    remoteAuthClient.availabilityStatus.collect { status ->
        when (status) {
            RemoteAuthClient.STATUS_UNAVAILABLE ->
                TODO("Present alternative flow as remote auth is not available")
            RemoteAuthClient.STATUS_TEMPORARILY_UNAVAILABLE ->
                TODO("Present education to user to connect devices or bring to proximity.")
            RemoteAuthClient.STATUS_AVAILABLE,
            RemoteAuthClient.STATUS_UNKNOWN ->
                // Present normal auth flow when we don't know (old devices)
                // or when we know it is available.
                remoteAuthClient.sendAuthorizationRequest(
                    oAuthRequest,
                    Runnable::run,
                    myAuthCallback
                )
        }
    }
}
