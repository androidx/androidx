/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.biometric

import androidx.annotation.MainThread

/**
 * A launcher for a previously-[prepared call][registerForAuthenticationResult] to start the process
 * of executing an authentication.
 */
@MainThread
public interface AuthenticationResultLauncher {
    /**
     * Executes an authentication given the required [input].This can't be called until the fragment
     * or activity's Lifecycle has reached CREATED.
     */
    public fun launch(input: AuthenticationRequest)

    /**
     * Cancel the authentication, unregisters this launcher, releasing the underlying result
     * callback, and any references captured within it.
     */
    public fun cancel()
}
