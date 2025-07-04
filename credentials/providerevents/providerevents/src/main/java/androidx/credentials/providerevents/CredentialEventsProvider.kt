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

package androidx.credentials.providerevents

import android.os.IBinder
import androidx.credentials.providerevents.service.CredentialProviderEventsService

/**
 * Defines an interface for system components that handle credential event propagation APIs.
 *
 * System components implement this interface to return a stub [IBinder] to be used by
 * [CredentialProviderEventsService].
 *
 * **Usage:**
 * 1. System components implement this interface.
 * 2. When binding to [CredentialProviderEventsService], the service instantiates and returns the
 *    implemented class based on the `EVENTS_PROVIDER_KEY` extra in the intent.
 * 3. The returned [IBinder] allows the feature provider to execute custom logic before calling the
 *    public endpoints of [CredentialProviderEventsService].
 *
 * This mechanism enables system components to manage their own event propagation to credential
 * providers.
 */
public interface CredentialEventsProvider {
    /**
     * Returns the [IBinder] stub implementation.
     *
     * This stub is invoked by the feature provider after binding to
     * [CredentialProviderEventsService]. It allows the feature provider to execute custom logic
     * before interacting with the service.
     *
     * @param service The instance of [CredentialProviderEventsService] to interact with.
     */
    public fun getStubImplementation(service: CredentialProviderEventsService): IBinder?

    public companion object {
        /**
         * The key for the extra in the intent used to bind to the credential provider's service,
         * specifying the class name that implements [CredentialEventsProvider].
         */
        public const val EVENTS_SERVICE_PROVIDER_KEY: String =
            "androidx.credentials.providerevents.service.EVENTS_SERVICE_PROVIDER_KEY"
    }
}
