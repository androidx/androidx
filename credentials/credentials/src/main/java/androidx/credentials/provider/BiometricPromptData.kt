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

import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricManager.Authenticators
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.hardware.biometrics.BiometricPrompt.CryptoObject
import android.os.Bundle
import android.util.Log
import androidx.annotation.RestrictTo

/**
 * Biometric prompt data that can be optionally used by you to provide information needed for the
 * system to show a biometric prompt directly embedded into the Credential Manager selector.
 *
 * If you opt to use this, the meta-data provided through the [CreateEntry] or [CredentialEntry]
 * will be shown along with a biometric / device credential capture mechanism, on a single dialog,
 * hence avoiding navigation through multiple screens. When user confirmation is retrieved through
 * the aforementioned biometric / device capture mechanism, the [android.app.PendingIntent]
 * associated with the entry is invoked, and the flow continues as explained in [CreateEntry] or
 * [CredentialEntry].
 *
 * Note that the value of [allowedAuthenticators] together with the features of a given device,
 * determines whether a biometric auth or a device credential mechanism will / can be shown. It is
 * recommended you use [Authenticators] to select these values, though you can find equivalent
 * behaviour from usage of [BiometricManager.Authenticators]. This documentation will refer to
 * [Authenticators] constants, which easily map to their [BiometricManager.Authenticators]
 * counterparts, and in some cases, provide a more useful abstraction.
 *
 * @property allowedAuthenticators specifies the type(s) of authenticators that may be invoked by
 *   the [BiometricPrompt] to authenticate the user, defaults to [BIOMETRIC_WEAK] if not set
 * @property cryptoObject a crypto object to be unlocked after successful authentication; When set,
 *   the value of [allowedAuthenticators] must be [BIOMETRIC_STRONG] or else an
 *   [IllegalArgumentException] is thrown
 * @throws IllegalArgumentException if [cryptoObject] is not null, and the [allowedAuthenticators]
 *   is not set to [BIOMETRIC_STRONG]
 * @see Authenticators
 * @see BiometricManager.Authenticators
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class BiometricPromptData
internal constructor(
    val cryptoObject: BiometricPrompt.CryptoObject? = null,
    val allowedAuthenticators: @AuthenticatorTypes Int = BIOMETRIC_WEAK,
    private var isCreatedFromBundle: Boolean = false,
) {

    /**
     * Biometric prompt data that can be optionally used by you to provide information needed for
     * the system to show a biometric prompt directly embedded into the Credential Manager selector.
     *
     * If you opt to use this, the meta-data provided through the [CreateEntry] or [CredentialEntry]
     * will be shown along with a biometric / device credential capture mechanism, on a single
     * dialog, hence avoiding navigation through multiple screens. When user confirmation is
     * retrieved through the aforementioned biometric / device capture mechanism, the
     * [android.app.PendingIntent] associated with the entry is invoked, and the flow continues as
     * explained in [CreateEntry] or [CredentialEntry].
     *
     * Note that the value of [allowedAuthenticators] together with the features of a given device,
     * determines whether a biometric auth or a device credential mechanism will / can be shown. It
     * is recommended you use [Authenticators] to select these values, though you can find
     * equivalent behaviour from usage of [BiometricManager.Authenticators]. This documentation will
     * refer to [Authenticators] constants, which easily map to their
     * [BiometricManager.Authenticators] counterparts, and in some cases, provide a more useful
     * abstraction.
     *
     * If you opt to use this constructor, you are confirming you are not building from a slice.
     *
     * @param allowedAuthenticators specifies the type(s) of authenticators that may be invoked by
     *   the [BiometricPrompt] to authenticate the user, defaults to [BIOMETRIC_WEAK] if not set
     * @param cryptoObject a crypto object to be unlocked after successful authentication; When set,
     *   the value of [allowedAuthenticators] must be [BIOMETRIC_STRONG] or else an
     *   [IllegalArgumentException] is thrown
     * @throws IllegalArgumentException If [cryptoObject] is not null, and the
     *   [allowedAuthenticators] is not set to [BIOMETRIC_STRONG]
     * @see Authenticators
     * @see BiometricManager.Authenticators
     */
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
            "androidx.credentials.provider.credentialEntry.BUNDLE_HINT_ALLOWED_AUTHENTICATORS"

        internal const val BUNDLE_HINT_CRYPTO_OP_ID =
            "androidx.credentials.provider.credentialEntry.BUNDLE_HINT_CRYPTO_OP_ID"

        /**
         * Returns an instance of [BiometricPromptData] derived from a [Bundle] object.
         *
         * @param bundle the [Bundle] object constructed through [toBundle] method, often
         */
        // TODO(b/333444288) : Once available from BiometricPrompt, structure CryptoObject / opId
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun fromBundle(bundle: Bundle): BiometricPromptData? {
            return try {
                if (!bundle.containsKey(BUNDLE_HINT_ALLOWED_AUTHENTICATORS)) {
                    throw IllegalArgumentException("Bundle lacks allowed authenticator key.")
                }
                BiometricPromptData(
                    allowedAuthenticators = bundle.getInt(BUNDLE_HINT_ALLOWED_AUTHENTICATORS),
                    isCreatedFromBundle = true
                )
            } catch (e: Exception) {
                Log.i(TAG, "fromSlice failed with: " + e.message)
                null
            }
        }

        /** Returns a [Bundle] that contains the [BiometricPromptData] representation. */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun toBundle(biometricPromptData: BiometricPromptData): Bundle? {
            val bundle = Bundle()
            val biometricPromptMap: MutableMap<String, Int?> =
                mutableMapOf(
                    BUNDLE_HINT_ALLOWED_AUTHENTICATORS to biometricPromptData.allowedAuthenticators,
                    // TODO(b/325469910) : Use the proper opId method when available
                    BUNDLE_HINT_CRYPTO_OP_ID to Integer.MIN_VALUE
                )
            biometricPromptMap.forEach {
                if (it.value != null) {
                    bundle.putInt(it.key, it.value!!)
                }
            }
            return bundle
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
         * authentication. If opting to pass in a value for cryptoObject, it must not be null.
         *
         * @param cryptoObject the [CryptoObject] to be associated with this biometric
         *   authentication flow
         */
        fun setCryptoObject(cryptoObject: CryptoObject?): Builder {
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
            if (cryptoObject != null && allowedAuthenticators != null) {
                require(isStrongAuthenticationType(this.allowedAuthenticators)) {
                    "If the cryptoObject is non-null, the allowedAuthenticator value must be " +
                        "Authenticators.BIOMETRIC_STRONG"
                }
            }
            val allowedAuthenticators = this.allowedAuthenticators ?: BIOMETRIC_WEAK
            return BiometricPromptData(
                cryptoObject = cryptoObject,
                allowedAuthenticators = allowedAuthenticators,
            )
        }
    }
}
