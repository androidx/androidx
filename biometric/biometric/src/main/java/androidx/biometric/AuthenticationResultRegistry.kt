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

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.annotation.RestrictTo
import androidx.biometric.AuthenticationRequest.Biometric
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.biometric.BiometricPrompt.LifecycleContainer
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelStoreOwner
import java.util.concurrent.Executor

/**
 * A registry that stores [auth result callbacks][AuthenticationCallback] for
 * [registered calls][registerForAuthenticationResult].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AuthenticationResultRegistry {
    /**
     * Register a new callback with this registry. This is normally called by a higher level
     * convenience methods like [registerForAuthenticationResult].
     *
     * If [lifecycleContainer] is null,you must call [AuthenticationResultLauncher.unregister] on
     * the returned [AuthenticationResultLauncher] when the launcher is no longer needed to release
     * any values that might be captured in the registered callback.
     */
    public fun register(
        viewModelStoreOwner: ViewModelStoreOwner,
        fragmentManager: FragmentManager, // TODO(b/178855209): Remove fragmentManager
        resultCallback: AuthenticationResultCallback,
        lifecycleContainer: LifecycleContainer? = null,
        callbackExecutor: Executor? = null,
    ): AuthenticationResultLauncher {
        val callback = createAuthenticationCallback(resultCallback)
        var biometricPrompt: BiometricPrompt? = null

        fun unregister() {
            lifecycleContainer?.clearObservers()
            biometricPrompt?.destroy()
            biometricPrompt = null
        }

        if (lifecycleContainer != null) {
            val lifecycleObserver = LifecycleEventObserver { _, event ->
                when (event) {
                    ON_START -> {
                        biometricPrompt =
                            BiometricPrompt(
                                viewModelStoreOwner,
                                fragmentManager,
                                callback,
                                callbackExecutor,
                            )
                    }
                    ON_STOP -> {}
                    ON_DESTROY -> {
                        // The authentication should not be canceled here using
                        // cancelAuthentication().
                        // Instead, rely on the app to manage cancellation through cancel() calls,
                        // ensuring the authentication survives configuration changes.
                        unregister()
                    }
                    else -> {}
                }
            }
            lifecycleContainer.addObserver(lifecycleObserver)
        } else {
            biometricPrompt =
                BiometricPrompt(viewModelStoreOwner, fragmentManager, callback, callbackExecutor)
        }

        return object : AuthenticationResultLauncher {
            override fun launch(input: AuthenticationRequest) {
                biometricPrompt?.let { onLaunch(input, it) }
            }

            override fun cancel() {
                biometricPrompt?.cancelAuthentication()
                unregister()
            }

            override fun unregister() {
                unregister()
            }
        }
    }
}

private fun onLaunch(input: AuthenticationRequest, biometricPrompt: BiometricPrompt) {
    when (input) {
        is AuthenticationRequest.Biometric ->
            biometricPrompt.authInternal(
                title = input.title,
                subtitle = input.subtitle,
                content = input.content,
                logoBitmap = input.logoBitmap,
                logoRes = input.logoRes,
                logoDescription = input.logoDescription,
                minBiometricStrength = input.minStrength,
                isConfirmationRequired = input.isConfirmationRequired,
                authFallback = input.authFallback,
            )
        is AuthenticationRequest.Credential ->
            biometricPrompt.authInternal(
                title = input.title,
                subtitle = input.subtitle,
                content = input.content,
                cryptoObjectForCredentialOnly = input.cryptoObject,
            )
    }
}

private fun createAuthenticationCallback(
    resultCallback: AuthenticationResultCallback
): AuthenticationCallback {
    return object : AuthenticationCallback() {
        override fun onAuthenticationFailed() {
            resultCallback.onAuthFailure()
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            resultCallback.onAuthResult(
                AuthenticationResult.Success(result.cryptoObject, result.authenticationType)
            )
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            resultCallback.onAuthResult(AuthenticationResult.Error(errorCode, errString))
        }
    }
}

/** Shows the authentication prompt to the user with biometric and/or device credential. */
@SuppressLint("MissingPermission")
private fun BiometricPrompt.authInternal(
    title: String,
    subtitle: String? = null,
    content: AuthenticationRequest.BodyContent? = null,
    logoBitmap: Bitmap? = null,
    logoRes: Int = 0,
    logoDescription: String? = null,
    minBiometricStrength: Biometric.Strength? = null,
    isConfirmationRequired: Boolean = true,
    cryptoObjectForCredentialOnly: CryptoObject? = null,
    authFallback: Biometric.Fallback? = null,
) {

    PromptInfo.Builder().apply {
        // Set authenticators and fallbacks
        var authType =
            minBiometricStrength?.toAuthenticationType()
                ?: BiometricManager.Authenticators.DEVICE_CREDENTIAL
        when (authFallback) {
            is Biometric.Fallback.DeviceCredential ->
                authType = authType or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            is Biometric.Fallback.NegativeButton ->
                setNegativeButtonText(authFallback.negativeButtonText)
        }
        setAllowedAuthenticators(authType)

        // Set body content
        when (content) {
            is AuthenticationRequest.BodyContent.PlainText -> setDescription(content.description)
            is AuthenticationRequest.BodyContent.VerticalList -> {
                PromptVerticalListContentView.Builder().apply {
                    content.items.forEach { addListItem(it) }
                    content.description?.let { setDescription(content.description) }
                    setContentView(build())
                }
            }
            is AuthenticationRequest.BodyContent.ContentViewWithMoreOptionsButton -> {
                PromptContentViewWithMoreOptionsButton.Builder().apply {
                    content.description?.let { setDescription(content.description) }
                    setContentView(build())
                }
            }
        }

        // Set logo
        if (logoRes != 0) {
            setLogoRes(logoRes)
        } else if (logoBitmap != null) {
            setLogoBitmap(logoBitmap)
        }
        if (logoDescription != null) {
            setLogoDescription(logoDescription)
        }

        // Set other configurations
        setTitle(title)
        setSubtitle(subtitle)
        setConfirmationRequired(isConfirmationRequired)

        val cryptoObject =
            when (minBiometricStrength) {
                is Biometric.Strength.Class3 -> minBiometricStrength.cryptoObject
                null -> cryptoObjectForCredentialOnly
                else -> null
            }

        if (cryptoObject == null) {
            authenticate(build())
        } else {
            authenticate(build(), cryptoObject)
        }
    }
}
