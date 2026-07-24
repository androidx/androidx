/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.registry.provider

import android.os.IBinder
import androidx.annotation.RestrictTo

/**
 * Defines an interface for system components that handle UI delegation fulfillment APIs.
 *
 * System components implement this interface to return a stub [IBinder] to be used by
 * [UiDelegationFulfillmentService].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface UiDelegationFulfillmentProvider {
    /**
     * Returns the [IBinder] stub implementation.
     *
     * @param service The instance of [UiDelegationFulfillmentService] to interact with.
     */
    public fun getStubImplementation(service: UiDelegationFulfillmentService): IBinder?

    public companion object {
        /**
         * The key for the extra in the intent used to bind to the credential provider's service,
         * specifying the class name that implements [UiDelegationFulfillmentProvider].
         */
        public const val EXTRA_STUB_IMPL_CLASS_NAME: String =
            "androidx.credentials.registry.provider.extra.STUB_IMPL_CLASS_NAME"
    }
}
