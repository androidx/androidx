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

import androidx.biometric.BiometricPrompt

/** A fake implementation of [PromptConfigRepository] for testing purposes. */
internal class FakePromptConfigRepository : PromptConfigRepository {
    override var promptInfo: BiometricPrompt.PromptInfo? = null

    override var cryptoObject: BiometricPrompt.CryptoObject? = null

    override var isPromptShowing: Boolean = false

    override var isConfirmingDeviceCredential = false

    override var isDelayingPrompt = false

    override var isUsingKeyguardManagerForBiometricAndCredential = false

    override val allowedAuthenticators: Int
        get() = promptInfo?.allowedAuthenticators ?: 0

    private var _negativeButtonTextOverride: CharSequence? = null
    override val negativeButtonText: CharSequence?
        get() = _negativeButtonTextOverride ?: promptInfo?.negativeButtonText

    override var isIdentityCheckAvailable = false

    override fun setNegativeButtonTextOverride(negativeButtonTextOverride: CharSequence?) {
        _negativeButtonTextOverride = negativeButtonTextOverride
    }

    override suspend fun setDelayedDelayingPrompt(delayingPrompt: Boolean, delayedTime: Long) {
        isDelayingPrompt = delayingPrompt
    }
}
