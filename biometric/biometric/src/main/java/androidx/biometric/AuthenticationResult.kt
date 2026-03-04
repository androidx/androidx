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

/** Types of the terminal result of the authentication. */
public sealed interface AuthenticationResult {
    /**
     * A result when the user has successfully authenticated.
     *
     * @property crypto The [BiometricPrompt.CryptoObject] associated with this transaction.
     * @property authType An integer representing the type of authentication(e.g. device credential
     *   or biometric) that was requested from and successfully provided by the user.
     * @see [BiometricPrompt.AuthenticationResultType]
     */
    public class Success(
        public val crypto: BiometricPrompt.CryptoObject?,
        @param:BiometricPrompt.AuthenticationResultType public val authType: Int,
    ) : AuthenticationResult {
        override fun success(): Success = this
    }

    /**
     * A result when an error has been encountered and authentication has stopped.
     *
     * @property errorCode An integer ID associated with the error.
     * @property errString A human-readable string that describes the error.
     * @see [BiometricPrompt.AuthenticationError]
     */
    public class Error(
        @param:BiometricPrompt.AuthenticationError public val errorCode: Int,
        public val errString: CharSequence,
    ) : AuthenticationResult {
        override fun error(): Error = this
    }

    /** The prompt was dismissed because the user clicked a custom fallback option. */
    public class CustomFallbackSelected(
        public val fallback: AuthenticationRequest.Biometric.Fallback.CustomOption
    ) : AuthenticationResult {
        override fun customFallbackSelected(): CustomFallbackSelected = this
    }

    /** Whether this [AuthenticationResult] is a [Success]. */
    public fun isSuccess(): Boolean = this is Success

    /** Returns a [Success] only if it's a [Success], returns null otherwise. */
    public fun success(): Success? = null

    /** Whether this [AuthenticationResult] is an [Error]. */
    public fun isError(): Boolean = this is Error

    /** Returns a [Error] only if it's a [Error], returns null otherwise. */
    public fun error(): Error? = null

    /** Whether this [AuthenticationResult] is a [CustomFallbackSelected]. */
    public fun isCustomFallbackSelected(): Boolean = this is CustomFallbackSelected

    /**
     * Returns a [CustomFallbackSelected] only if it's a [CustomFallbackSelected], returns null
     * otherwise.
     */
    public fun customFallbackSelected(): CustomFallbackSelected? = null
}
