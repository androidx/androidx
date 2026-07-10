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

package androidx.biometric.internal.data

import android.os.Build
import androidx.biometric.AuthenticationRequest.Biometric.Fallback
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.utils.AuthenticatorUtils
import androidx.biometric.utils.CryptoObjectUtils
import kotlin.collections.emptyList
import kotlinx.coroutines.delay

/**
 * A repository for both the initial configuration and the dynamic states of the biometric prompt.
 */
internal interface PromptConfigRepository {
    /** A key used to track the current authentication session. */
    var currentAuthenticationKey: Int

    /** Info about the appearance and behavior of the prompt provided by the client application. */
    var promptInfo: BiometricPrompt.PromptInfo?

    /** The crypto object associated with the current authentication session. */
    var cryptoObject: BiometricPrompt.CryptoObject?

    /** Whether the prompt is currently showing. */
    var isPromptShowing: Boolean

    /** Whether the user is currently authenticating with their PIN, pattern, or password. */
    var isConfirmingDeviceCredential: Boolean

    /** Whether the prompt should delay showing the authentication UI. */
    var isDelayingPrompt: Boolean

    /**
     * Whether [android.app.KeyguardManager] is being used directly for authentication with both
     * biometric and credential authenticator types allowed.
     */
    var isUsingKeyguardManagerForBiometricAndCredential: Boolean

    /** The type(s) of authenticators that may be invoked by the biometric prompt. */
    @BiometricManager.AuthenticatorTypes val allowedAuthenticators: Int

    /** Whether the identity check is available on the current API level. */
    var isIdentityCheckAvailable: Boolean

    /**
     * The fallback option list that should be shown on the biometric prompt screen or separate
     * fallback options page.
     */
    val fallbackOptionList: List<Fallback>

    /** Sets a text override for the negative button. */
    fun setNegativeButtonTextOverride(negativeButtonTextOverride: CharSequence?)

    /**
     * Sets whether the prompt should delay showing its UI after a given amount of time.
     *
     * @param delayingPrompt Whether the prompt UI should be delayed.
     * @param delayedTime The amount of time to wait, in milliseconds.
     */
    suspend fun setDelayedDelayingPrompt(delayingPrompt: Boolean, delayedTime: Long)

    companion object {
        val instance: PromptConfigRepository by lazy { PromptConfigRepositoryImpl() }
    }
}

/**
 * A repository for authentication state and events.
 *
 * This repository and all of its data is persisted over the lifetime of the client activity that
 * hosts the [BiometricPrompt].
 */
internal class PromptConfigRepositoryImpl : PromptConfigRepository {
    override var currentAuthenticationKey: Int = 0

    override var promptInfo: BiometricPrompt.PromptInfo? = null
        set(value) {
            field = value
            updateConsolidatedAuthenticators()
        }

    override var cryptoObject: BiometricPrompt.CryptoObject? = null
        set(value) {
            field = value

            // Use a fake crypto object to force Strong biometric auth prior to Android 11 (API 30).
            if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                    promptInfo != null &&
                    AuthenticatorUtils.isAtLeastStrength(
                        promptInfo!!.allowedAuthenticators,
                        BiometricManager.Authenticators.BIOMETRIC_STRONG,
                    ) &&
                    value == null
            ) {
                field = CryptoObjectUtils.createFakeCryptoObject()
            }

            updateConsolidatedAuthenticators()
        }

    override var isPromptShowing: Boolean = false

    override var isConfirmingDeviceCredential: Boolean = false

    override var isDelayingPrompt: Boolean = false

    override var isUsingKeyguardManagerForBiometricAndCredential = false

    /** Whether Identity check is available in the current API level. */
    override var isIdentityCheckAvailable = false
        set(value) {
            field = value
            updateConsolidatedAuthenticators()
        }

    private var _allowedAuthenticators: Int = 0
    /**
     * The type(s) of authenticators that may be invoked by the biometric prompt.
     *
     * If a non-null [BiometricPrompt.PromptInfo] has been set, this is the single consolidated set
     * of authenticators allowed by the prompt, taking into account the values of
     * [BiometricPrompt.PromptInfo.getAllowedAuthenticators],
     * [BiometricPrompt.PromptInfo.isDeviceCredentialAllowed], [cryptoObject] and
     * [isIdentityCheckAvailable].
     */
    override val allowedAuthenticators: Int
        @BiometricManager.AuthenticatorTypes get() = _allowedAuthenticators

    /**
     * A label for the negative button shown on the prompt.
     *
     * If set, this value overrides the one returned by
     * [BiometricPrompt.PromptInfo.getNegativeButtonText].
     */
    private var _negativeButtonTextOverride: CharSequence? = null
    override val fallbackOptionList
        get() =
            _negativeButtonTextOverride?.let {
                listOf(Fallback.OverriddenDeviceCredential(it.toString()))
            }
                ?: promptInfo
                    ?.negativeButtonText
                    ?.takeUnless { it.isEmpty() }
                    ?.let { listOf(Fallback.CustomOption(it.toString())) }
                ?: promptInfo?.fallbackOptionList
                ?: emptyList()

    override fun setNegativeButtonTextOverride(negativeButtonTextOverride: CharSequence?) {
        _negativeButtonTextOverride = negativeButtonTextOverride
    }

    override suspend fun setDelayedDelayingPrompt(delayingPrompt: Boolean, delayedTime: Long) {
        delay(delayedTime)
        isDelayingPrompt = delayingPrompt
    }

    private fun updateConsolidatedAuthenticators() {
        if (promptInfo == null) {
            return
        }
        _allowedAuthenticators =
            AuthenticatorUtils.getConsolidatedAuthenticators(
                promptInfo!!,
                cryptoObject,
                isIdentityCheckAvailable,
            )
    }
}
