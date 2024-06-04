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

package androidx.credentials.provider

import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.core.os.BuildCompat

/**
 * Biometric prompt data that can be optionally used to provide information needed for the system to
 * show a biometric prompt directly embedded into the Credential Manager selector.
 *
 * If you opt to use this, the meta-data provided through the [CreateEntry] or [CredentialEntry]
 * will be shown along with a biometric / device credential capture mechanism, on a single dialog,
 * hence avoiding navigation through multiple screens. When user confirmation is retrieved through
 * the aforementioned biometric / device capture mechanism, the [android.app.PendingIntent]
 * associated with the entry is invoked, and the flow continues as explained in [CreateEntry] or
 * [CredentialEntry].
 *
 * Note that the value of [allowedAuthenticators] together with the features of a given device,
 * determines whether a biometric auth or a device credential mechanism will / can be shown. The
 * value for this property is found in [Authenticators].
 *
 * @property allowedAuthenticators specifies the type(s) of authenticators that may be invoked by
 *   the [BiometricPrompt] to authenticate the user, defaults to [BIOMETRIC_WEAK] if not set
 * @property cryptoObject a crypto object to be unlocked after successful authentication; When set,
 *   the value of [allowedAuthenticators] must be [BIOMETRIC_STRONG] or else an
 *   [IllegalArgumentException] is thrown
 * @throws IllegalArgumentException if [cryptoObject] is not null, and the [allowedAuthenticators]
 *   is not set to [BIOMETRIC_STRONG]
 * @see Authenticators
 */
@RequiresApi(35)
class BiometricPromptData
internal constructor(
    val cryptoObject: BiometricPrompt.CryptoObject? = null,
    val allowedAuthenticators: @AuthenticatorTypes Int = BIOMETRIC_WEAK,
    private var isCreatedFromBundle: Boolean = false,
) {

    /**
     * Biometric prompt data that can be optionally used to provide information needed for the
     * system to show a biometric prompt directly embedded into the Credential Manager selector.
     *
     * If you opt to use this, the meta-data provided through the [CreateEntry] or [CredentialEntry]
     * will be shown along with a biometric / device credential capture mechanism, on a single
     * dialog, hence avoiding navigation through multiple screens. When user confirmation is
     * retrieved through the aforementioned biometric / device capture mechanism, the
     * [android.app.PendingIntent] associated with the entry is invoked, and the flow continues as
     * explained in [CreateEntry] or [CredentialEntry].
     *
     * Note that the value of [allowedAuthenticators] together with the features of a given device,
     * determines whether a biometric auth or a device credential mechanism will / can be shown. The
     * value for this property is found in [Authenticators].
     *
     * @param allowedAuthenticators specifies the type(s) of authenticators that may be invoked by
     *   the [BiometricPrompt] to authenticate the user, defaults to [BIOMETRIC_WEAK] if not set
     * @param cryptoObject a crypto object to be unlocked after successful authentication; When set,
     *   the value of [allowedAuthenticators] must be [BIOMETRIC_STRONG] or else an
     *   [IllegalArgumentException] is thrown
     * @throws IllegalArgumentException if [cryptoObject] is not null, and the
     *   [allowedAuthenticators] is not set to [BIOMETRIC_STRONG]
     * @see Authenticators
     */
    @JvmOverloads
    constructor(
        cryptoObject: BiometricPrompt.CryptoObject? = null,
        allowedAuthenticators: @AuthenticatorTypes Int = BIOMETRIC_WEAK
    ) : this(cryptoObject, allowedAuthenticators, isCreatedFromBundle = false)

    init {
        if (!isCreatedFromBundle) {
            // This is not expected to throw for certain eligible callers who utilize the
            // isCreatedFromBundle hidden property.
            require(ALLOWED_AUTHENTICATOR_VALUES.contains(allowedAuthenticators)) {
                "The allowed authenticator must be specified according to the BiometricPrompt spec."
            }
        }
        if (cryptoObject != null) {
            require(isStrongAuthenticationType(allowedAuthenticators)) {
                "If the cryptoObject is non-null, the allowedAuthenticator value must be " +
                    "Authenticators.BIOMETRIC_STRONG."
            }
        }
    }

    internal companion object {

        private const val TAG = "BiometricPromptData"

        internal const val BUNDLE_HINT_ALLOWED_AUTHENTICATORS =
            "androidx.credentials.provider.BUNDLE_HINT_ALLOWED_AUTHENTICATORS"

        internal const val BUNDLE_HINT_CRYPTO_OP_ID =
            "androidx.credentials.provider.BUNDLE_HINT_CRYPTO_OP_ID"

        /**
         * Returns an instance of [BiometricPromptData] derived from a [Bundle] object.
         *
         * @param bundle the [Bundle] object constructed through [toBundle] method, often
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun fromBundle(bundle: Bundle): BiometricPromptData? {
            return try {
                if (!bundle.containsKey(BUNDLE_HINT_ALLOWED_AUTHENTICATORS)) {
                    throw IllegalArgumentException("Bundle lacks allowed authenticator key.")
                }
                if (BuildCompat.isAtLeastV()) {
                    Api35Impl.fromBundle(bundle)
                } else {
                    ApiMinImpl.fromBundle(bundle)
                }
            } catch (e: Exception) {
                Log.i(TAG, "fromSlice failed with: " + e.message)
                null
            }
        }

        /** Returns a [Bundle] that contains the [BiometricPromptData] representation. */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun toBundle(biometricPromptData: BiometricPromptData): Bundle {
            return if (BuildCompat.isAtLeastV()) {
                Api35Impl.toBundle(biometricPromptData)
            } else {
                ApiMinImpl.toBundle(biometricPromptData)
            }
        }

        private fun isStrongAuthenticationType(authenticationTypes: Int?): Boolean {
            if (authenticationTypes == null) {
                return false
            }
            val biometricStrength: Int = authenticationTypes and BIOMETRIC_WEAK
            if (biometricStrength and BiometricManager.Authenticators.BIOMETRIC_STRONG.inv() != 0) {
                return false
            }
            return true
        }

        private val ALLOWED_AUTHENTICATOR_VALUES =
            setOf(
                BIOMETRIC_STRONG,
                BIOMETRIC_WEAK,
                DEVICE_CREDENTIAL,
                BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
                BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            )
    }

    /** Builder for constructing an instance of [BiometricPromptData] */
    class Builder {
        private var cryptoObject: CryptoObject? = null
        private var allowedAuthenticators: Int? = null

        /**
         * Sets whether this [BiometricPromptData] should have a crypto object associated with this
         * authentication. If opting to pass in a value, the [allowedAuthenticators] must be
         * [BIOMETRIC_STRONG].
         *
         * @param cryptoObject the [CryptoObject] to be associated with this biometric
         *   authentication flow
         */
        fun setCryptoObject(cryptoObject: CryptoObject): Builder {
            this.cryptoObject = cryptoObject
            return this
        }

        /**
         * Specifies the type(s) of authenticators that may be invoked to authenticate the user.
         * Available authenticator types are defined in [Authenticators] and can be combined via
         * bitwise OR. Defaults to [BIOMETRIC_WEAK].
         *
         * If this method is used and no authenticator of any of the specified types is available at
         * the time an error code will be supplied as part of [android.content.Intent] that will be
         * launched by the containing [CredentialEntry] or [CreateEntry]'s corresponding
         * [android.app.PendingIntent].
         *
         * @param allowedAuthenticators A bit field representing all valid authenticator types that
         *   may be invoked by the Credential Manager selector.
         */
        fun setAllowedAuthenticators(allowedAuthenticators: @AuthenticatorTypes Int): Builder {
            this.allowedAuthenticators = allowedAuthenticators
            return this
        }

        /**
         * Builds the [BiometricPromptData] instance.
         *
         * @throws IllegalArgumentException If [cryptoObject] is not null, and the
         *   [allowedAuthenticators] is not set to [BIOMETRIC_STRONG]
         */
        fun build(): BiometricPromptData {
            val allowedAuthenticators = this.allowedAuthenticators ?: BIOMETRIC_WEAK
            return BiometricPromptData(
                cryptoObject = cryptoObject,
                allowedAuthenticators = allowedAuthenticators,
            )
        }
    }

    private object ApiMinImpl {
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun toBundle(biometricPromptData: BiometricPromptData): Bundle {
            val bundle = Bundle()
            bundle.putInt(
                BUNDLE_HINT_ALLOWED_AUTHENTICATORS,
                biometricPromptData.allowedAuthenticators
            )
            return bundle
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun fromBundle(bundle: Bundle): BiometricPromptData {
            val biometricPromptData =
                BiometricPromptData(
                    allowedAuthenticators = bundle.getInt(BUNDLE_HINT_ALLOWED_AUTHENTICATORS),
                    isCreatedFromBundle = true,
                )
            return biometricPromptData
        }
    }

    private object Api35Impl {

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun toBundle(biometricPromptData: BiometricPromptData): Bundle {
            val bundle = Bundle()
            bundle.putInt(
                BUNDLE_HINT_ALLOWED_AUTHENTICATORS,
                biometricPromptData.allowedAuthenticators
            )
            biometricPromptData.cryptoObject?.let {
                bundle.putLong(BUNDLE_HINT_CRYPTO_OP_ID, it.operationHandle)
            }

            return bundle
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun fromBundle(bundle: Bundle): BiometricPromptData {
            var cryptoObject: CryptoObject? = null
            if (bundle.containsKey(BUNDLE_HINT_CRYPTO_OP_ID)) {
                val opId = bundle.getLong(BUNDLE_HINT_CRYPTO_OP_ID)
                cryptoObject = CryptoObject(opId)
            }
            val biometricPromptData =
                BiometricPromptData(
                    allowedAuthenticators = bundle.getInt(BUNDLE_HINT_ALLOWED_AUTHENTICATORS),
                    cryptoObject = cryptoObject,
                    isCreatedFromBundle = true,
                )
            return biometricPromptData
        }
    }
}
