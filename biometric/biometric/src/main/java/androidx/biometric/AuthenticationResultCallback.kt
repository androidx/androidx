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

/** A callback to be called when an [AuthenticationResult] is available. */
public fun interface AuthenticationResultCallback {
    /**
     * Called when the authentication process is completed successfully or with an error.
     *
     * The authentication result will contain the details about the result.
     *
     * This callback will always be called once and only once per authentication session.
     *
     * For example:
     * ```kotlin
     * override fun onAuthResult(result: AuthenticationResult) {
     *      when (result) {
     *              is AuthenticationResult.Success -> {
     *                  // Handle success
     *              }
     *              is AuthenticationResult.Error -> {
     *                 // Handle authentication error, e.g. negative button click, user
     *                 // cancellation, etc
     *              }
     *      }
     * }
     * ```
     */
    public fun onAuthResult(result: AuthenticationResult)

    /**
     * The callback to be called if an authentication attempt has failed due to a wrong biometric
     * recognition (e.g. wrong fingerprint, wrong face). This callback will be called for every
     * failed attempt, if any.
     *
     * For example:
     * ```kotlin
     * override fun onAuthAttemptFailed() {
     *      // Track how many times a user fails (e.g., for internal analytics).
     *
     *      // Note: Do NOT show a Toast or Dialog here usually, because the system UI
     *      // already shows "Not recognized" text on Biometric Prompt.
     * }
     * ```
     */
    public fun onAuthAttemptFailed() {}
}
