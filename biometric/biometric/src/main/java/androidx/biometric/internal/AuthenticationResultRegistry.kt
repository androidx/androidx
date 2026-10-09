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

package androidx.biometric.internal

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationRequest.Biometric
import androidx.biometric.AuthenticationRequest.Credential
import androidx.biometric.AuthenticationResult
import androidx.biometric.AuthenticationResultCallback
import androidx.biometric.AuthenticationResultLauncher
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.biometric.BiometricPrompt.LifecycleContainer
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.biometric.PromptContentViewWithMoreOptionsButton
import androidx.biometric.PromptVerticalListContentView
import androidx.biometric.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import java.util.concurrent.Executor

private const val TAG = "AuthResultRegistry"

/**
 * A registry that stores [auth result callbacks][AuthenticationCallback] for
 * [registered calls][androidx.biometric.registerForAuthenticationResult].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AuthenticationResultRegistry {
    /**
     * Register a new callback with this registry. This is normally called by a higher level
     * convenience methods like [androidx.biometric.registerForAuthenticationResult].
     */
    public fun register(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        viewModelStoreOwner: ViewModelStoreOwner,
        confirmCredentialActivityLauncher: Runnable,
        resultCallback: AuthenticationResultCallback,
        callbackExecutor: Executor? = null,
    ): AuthenticationResultLauncher =
        register(
            contextProvider = { context },
            lifecycleOwner = lifecycleOwner,
            viewModelStoreOwner = viewModelStoreOwner,
            confirmCredentialActivityLauncher = confirmCredentialActivityLauncher,
            resultCallback = resultCallback,
            callbackExecutor = callbackExecutor,
        )

    /**
     * Register a new callback with this registry using a lazy [contextProvider]. This allows
     * registration before a [androidx.fragment.app.Fragment] is attached to its host context.
     */
    public fun register(
        contextProvider: () -> Context,
        lifecycleOwner: LifecycleOwner,
        viewModelStoreOwner: ViewModelStoreOwner,
        confirmCredentialActivityLauncher: Runnable,
        resultCallback: AuthenticationResultCallback,
        callbackExecutor: Executor? = null,
    ): AuthenticationResultLauncher {
        val callback = createAuthenticationCallback(resultCallback)
        var biometricPrompt: BiometricPrompt? = null
        var pendingRequest: AuthenticationRequest? = null
        val lifecycleContainer = LifecycleContainer(lifecycleOwner.lifecycle)
        val initPrompt = {
            biometricPrompt
                ?: BiometricPrompt(
                        contextProvider(),
                        lifecycleOwner,
                        viewModelStoreOwner,
                        confirmCredentialActivityLauncher,
                        callbackExecutor,
                        callback,
                    )
                    .also { biometricPrompt = it }
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                initPrompt()
                pendingRequest?.let { request ->
                    pendingRequest = null
                    initPrompt().onLaunch(contextProvider(), request)
                }
            } else if (event == Lifecycle.Event.ON_DESTROY) {
                // The authentication itself should not be canceled in ON_DESTROY.
                // Instead, rely on the app to manage cancellation through cancel() calls,
                // ensuring the authentication survives configuration changes.
                pendingRequest = null
                lifecycleContainer.clearObservers()
            }
        }
        lifecycleContainer.addObserver(lifecycleObserver)

        return object : AuthenticationResultLauncher {
            override fun launch(input: AuthenticationRequest) {
                val state = lifecycleOwner.lifecycle.currentState
                if (state == Lifecycle.State.DESTROYED) {
                    // ON_DESTROY has already cleared any pending request and observers, so a
                    // queued request would never be dispatched. Drop it rather than leak it.
                    Log.w(TAG, "Ignoring launch() on a destroyed LifecycleOwner.")
                    return
                }
                if (state.isAtLeast(Lifecycle.State.STARTED)) {
                    val context = contextProvider()
                    initPrompt().onLaunch(context, input)
                } else {
                    pendingRequest = input
                }
            }

            override fun cancel() {
                pendingRequest = null
                biometricPrompt?.cancelAuthentication()
            }
        }
    }
}

private fun BiometricPrompt.onLaunch(context: Context, input: AuthenticationRequest) {
    when (input) {
        is Biometric ->
            authInternal(
                context = context,
                title = input.title,
                subtitle = input.subtitle,
                content = input.content,
                logoBitmap = input.logoBitmap,
                logoRes = input.logoRes,
                logoDescription = input.logoDescription,
                minBiometricStrength = input.minStrength,
                isConfirmationRequired = input.isConfirmationRequired,
                authFallbacks = input.authFallbacks,
            )
        is Credential ->
            authInternal(
                context = context,
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
            resultCallback.onAuthAttemptFailed()
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            resultCallback.onAuthResult(
                AuthenticationResult.Success(result.cryptoObject, result.authenticationType)
            )
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            resultCallback.onAuthResult(AuthenticationResult.Error(errorCode, errString))
        }

        override fun onFallbackSelected(fallback: Biometric.Fallback.CustomOption) {
            resultCallback.onAuthResult(AuthenticationResult.CustomFallbackSelected(fallback))
        }
    }
}

/** Shows the authentication prompt to the user with biometric and/or device credential. */
@SuppressLint("MissingPermission")
private fun BiometricPrompt.authInternal(
    context: Context,
    title: String,
    subtitle: String? = null,
    content: AuthenticationRequest.BodyContent? = null,
    logoBitmap: Bitmap? = null,
    logoRes: Int = 0,
    logoDescription: String? = null,
    minBiometricStrength: Biometric.Strength? = null,
    isConfirmationRequired: Boolean = true,
    cryptoObjectForCredentialOnly: CryptoObject? = null,
    authFallbacks: List<Biometric.Fallback>? = null,
) {
    PromptInfo.Builder().apply {
        // Set authenticators and fallbacks
        var authType =
            minBiometricStrength?.toAuthenticationType()
                ?: BiometricManager.Authenticators.DEVICE_CREDENTIAL

        minBiometricStrength?.toAuthenticationType()?.let {
            fun defaultCancelButton(): Biometric.Fallback =
                Biometric.Fallback.DefaultCancel(context.getString(android.R.string.cancel))

            val fallbacksToProcess =
                when {
                    authFallbacks.multipleFallbackOptionsValid() -> authFallbacks!!
                    !authFallbacks.isNullOrEmpty() -> listOf(authFallbacks.first())
                    else -> listOf(defaultCancelButton())
                }

            fallbacksToProcess.forEach { fallback ->
                when (fallback) {
                    is Biometric.Fallback.DeviceCredential -> {
                        authType = authType or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    }
                    else -> addFallbackOption(fallback)
                }
            }
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

internal fun List<Biometric.Fallback>?.multipleFallbackOptionsValid(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
        Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1 &&
        this != null &&
        this.size > 1
