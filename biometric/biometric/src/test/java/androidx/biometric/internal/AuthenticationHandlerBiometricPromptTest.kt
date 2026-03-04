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

package androidx.biometric.internal

import android.app.Application
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.AuthenticationRequest
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.internal.data.CanceledFrom
import androidx.biometric.internal.data.FakeAuthenticationStateRepository
import androidx.biometric.internal.data.FakePromptConfigRepository
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.biometric.utils.AuthenticatorUtils
import androidx.biometric.utils.BiometricErrorData
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowKeyguardManager

private const val NEGATIVE_BUTTON_TEXT = "test"

@RunWith(AndroidJUnit4::class)
@Config(minSdk = Build.VERSION_CODES.P)
class AuthenticationHandlerBiometricPromptTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val promptRepository = FakePromptConfigRepository()
    private val authRepository = FakeAuthenticationStateRepository()
    private val viewModel: AuthenticationViewModel =
        AuthenticationViewModel(promptRepository, authRepository)
    private val clientExecutor: Executor = Executor { it.run() }

    private var isConfirmCredentialActivityLaunched = false
    private var errorCode: Int = -1
    private var fallback: AuthenticationRequest.Biometric.Fallback.CustomOption? = null
    private val clientAuthenticationCallback =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                this@AuthenticationHandlerBiometricPromptTest.errorCode = errorCode
            }

            override fun onFallbackSelected(
                fallback: AuthenticationRequest.Biometric.Fallback.CustomOption
            ) {
                this@AuthenticationHandlerBiometricPromptTest.fallback = fallback
            }
        }

    private lateinit var authenticationHandler: AuthenticationHandlerBiometricPrompt

    @Before
    fun setUp() {
        // TODO(442913777): Inject isManagingDeviceCredentialButton to remove this Shadow usage.
        val shadowKeyguardManager: ShadowKeyguardManager =
            Shadows.shadowOf(context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
        shadowKeyguardManager.setIsDeviceSecure(true)
        val testLifecycleOwner = TestLifecycleOwner()
        testLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

        authenticationHandler =
            AuthenticationHandlerBiometricPrompt(
                context = context,
                lifecycleOwner = testLifecycleOwner,
                viewModel = viewModel,
                confirmCredentialActivityLauncher = { isConfirmCredentialActivityLaunched = true },
                clientExecutor = clientExecutor,
                clientAuthenticationCallback = clientAuthenticationCallback,
            )
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    fun onAuthenticationError_lockoutWhenManagingDeviceCredentialButton_showsKeyguard() {
        val allowedAuthenticators =
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        assertThat(context.isManagingDeviceCredentialButton(allowedAuthenticators)).isTrue()
        authenticationHandler.authenticate(getPromptInfo(allowedAuthenticators), null)

        viewModel.setAuthenticationError(
            BiometricErrorData(BiometricPrompt.ERROR_LOCKOUT, "Lockout")
        )

        assertThat(isConfirmCredentialActivityLaunched).isTrue()
        assertThat(errorCode).isEqualTo(-1)
    }

    @Test
    fun onAuthenticationError_lockoutWithoutDeviceCredential_sendsErrorToClient() {
        authenticationHandler.authenticate(getPromptInfo(), null)

        viewModel.setAuthenticationError(
            BiometricErrorData(BiometricPrompt.ERROR_LOCKOUT, "Lockout")
        )

        assertThat(isConfirmCredentialActivityLaunched).isFalse()
        assertThat(errorCode).isEqualTo(BiometricPrompt.ERROR_LOCKOUT)
    }

    @Test
    fun onAuthenticationError_nonLockoutError_sendsErrorToClient() {
        authenticationHandler.authenticate(getPromptInfo(), null)

        viewModel.setAuthenticationError(
            BiometricErrorData(BiometricPrompt.ERROR_HW_UNAVAILABLE, "HW Unavailable")
        )

        assertThat(isConfirmCredentialActivityLaunched).isFalse()
        assertThat(errorCode).isEqualTo(BiometricPrompt.ERROR_HW_UNAVAILABLE)
    }

    @Test
    fun onMoreOptionsButtonPressed_sendsErrorAndCancels() {
        authenticationHandler.authenticate(getPromptInfo(), null)
        val cancellationSignal = viewModel.cancellationSignalProvider.biometricCancellationSignal
        assertThat(cancellationSignal.isCanceled).isFalse()

        viewModel.setMoreOptionsButtonPressPending()

        assertThat(errorCode).isEqualTo(BiometricPrompt.ERROR_CONTENT_VIEW_MORE_OPTIONS_BUTTON)
        assertThat(cancellationSignal.isCanceled).isTrue()
        assertThat(viewModel.canceledFrom).isEqualTo(CanceledFrom.MORE_OPTIONS_BUTTON)
    }

    @Test
    fun onFallbackOptionPressed_sendsFallbackOptionAndCancels() {
        authenticationHandler.authenticate(getPromptInfo(), null)
        val cancellationSignal = viewModel.cancellationSignalProvider.biometricCancellationSignal
        assertThat(cancellationSignal.isCanceled).isFalse()

        val expectedFallback = AuthenticationRequest.Biometric.Fallback.CustomOption("test")
        viewModel.setFallbackOptionPressPending(expectedFallback)

        assertThat(fallback).isEqualTo(expectedFallback)
        assertThat(cancellationSignal.isCanceled).isTrue()
        assertThat(viewModel.canceledFrom).isEqualTo(CanceledFrom.FALLBACK_OPTION)
    }

    @Test
    fun onNegativeButtonPressed_sendsFallbackOptionAndCancels() {
        authenticationHandler.authenticate(getPromptInfo(), null)
        val cancellationSignal = viewModel.cancellationSignalProvider.biometricCancellationSignal
        assertThat(cancellationSignal.isCanceled).isFalse()

        viewModel.setNegativeButtonPressPending()

        assertThat(fallback?.text).isEqualTo(NEGATIVE_BUTTON_TEXT)
        assertThat(cancellationSignal.isCanceled).isTrue()
        assertThat(viewModel.canceledFrom).isEqualTo(CanceledFrom.NEGATIVE_BUTTON)
    }

    @Test
    fun onDefaultCancelButtonPressed_sendsErrorAndCancels() {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK
        val builder = BiometricPrompt.PromptInfo.Builder().setTitle("test")
        val defaultCancelText = "Cancel"
        builder.addFallbackOption(
            AuthenticationRequest.Biometric.Fallback.DefaultCancel(defaultCancelText)
        )
        builder.setAllowedAuthenticators(authenticators)
        authenticationHandler.authenticate(builder.build(), null)
        val cancellationSignal = viewModel.cancellationSignalProvider.biometricCancellationSignal
        assertThat(cancellationSignal.isCanceled).isFalse()

        viewModel.setNegativeButtonPressPending()

        assertThat(errorCode).isEqualTo(BiometricPrompt.ERROR_CANCELED)
        assertThat(cancellationSignal.isCanceled).isTrue()
        assertThat(viewModel.canceledFrom).isEqualTo(CanceledFrom.USER)
    }

    private fun getPromptInfo(
        authenticators: Int = BiometricManager.Authenticators.BIOMETRIC_WEAK
    ): BiometricPrompt.PromptInfo {
        val builder = BiometricPrompt.PromptInfo.Builder().setTitle("test")
        if (!AuthenticatorUtils.isDeviceCredentialAllowed(authenticators)) {
            builder.addFallbackOption(
                AuthenticationRequest.Biometric.Fallback.CustomOption(NEGATIVE_BUTTON_TEXT)
            )
        }
        builder.setAllowedAuthenticators(authenticators)
        return builder.build()
    }
}
