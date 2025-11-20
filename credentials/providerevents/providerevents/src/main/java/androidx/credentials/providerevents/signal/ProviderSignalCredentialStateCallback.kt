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

/**
 * Callback to be used for responding to
 * [androidx.credentials.providerevents.service.CredentialProviderEventsService.onSignalCredentialStateRequest]
 */
public interface ProviderSignalCredentialStateCallback {
    /**
     * To be invoked when the provider received the signal request through
     * [androidx.credentials.providerevents.service.CredentialProviderEventsService.onSignalCredentialStateRequest]
     * and was able to service the signal request. This is an optional callback to be invoked which
     * means that if the provider does not need to consume the signal and does not invoke this
     * endpoint, there are no consequences.
     *
     * Note that this signal is only for the framework, and will not be propagated back to the
     * calling app and hence cannot be used as a means to collect information about credentials and
     * with which credential provider they live.
     */
    public fun onSignalConsumed()
}
