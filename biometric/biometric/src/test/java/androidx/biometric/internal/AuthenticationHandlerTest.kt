/*
 * Copyright 2026 The Android Open Source Project
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
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.internal.data.FakeAuthenticationStateRepository
import androidx.biometric.internal.data.FakePromptConfigRepository
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.biometric.utils.DeviceUtils
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class AuthenticationHandlerTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val promptRepository = FakePromptConfigRepository()
    private val authRepository = FakeAuthenticationStateRepository()
    private val viewModel: AuthenticationViewModel =
        AuthenticationViewModel(promptRepository, authRepository)
    private val testLifecycleOwner = TestLifecycleOwner()

    private val bioAndCredentialInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Title")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

    private val bioOnlyInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Title 1")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("Cancel")
            .build()

    @Test
    fun create_doesNotEagerlyCreateHandler_whenAuthenticationIsNotActive() {
        assertThat(viewModel.isPromptShowing).isFalse()

        val handler = createHandler()

        handler.assertInternalHandlerIsNull()
    }

    @Test
    fun authenticate_createHandler() {
        assertThat(viewModel.isPromptShowing).isFalse()

        val handler = createHandler()
        handler.authenticate(bioAndCredentialInfo, null)

        handler.assertInternalHandlerIsNotNull()
    }

    @Test
    fun init_reconnectsToHandler_whenPromptIsShowing_andKeyMatches() {
        val handlerInitial = createHandler()
        handlerInitial.authenticate(bioAndCredentialInfo, null)
        assertThat(handlerInitial.key).isEqualTo(1)
        assertThat(viewModel.currentAuthenticationKey).isEqualTo(1)

        // Simulate activity rotation
        // resetManagerKey is called in AuthenticationManager.ON_DESTROY
        viewModel.resetHandlerKey()
        // Activity recreated, ON_START happens
        // Launcher is recreated, should call generateNextManagerKey() and get 1 again
        val handlerRecreated = createHandler()

        handlerRecreated.assertInternalHandlerIsNotNull()
        assertThat(handlerRecreated.key).isEqualTo(1)
        assertThat(viewModel.currentAuthenticationKey).isEqualTo(1)
    }

    @Test
    fun onAuthenticationResult_multipleHandlers_dispatchedToCorrectHandler() {
        var authResult1: BiometricPrompt.AuthenticationResult? = null
        val handler1 =
            createHandler(
                mainExecutor = { it.run() },
                authenticationCallback =
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult
                        ) {
                            authResult1 = result
                        }
                    },
            )

        var authResult2: BiometricPrompt.AuthenticationResult? = null
        val handler2 =
            createHandler(
                mainExecutor = { it.run() },
                authenticationCallback =
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult
                        ) {
                            authResult2 = result
                        }
                    },
            )

        // handler2 starts authentication, it should be the active one.
        handler2.authenticate(bioAndCredentialInfo, null)
        assertThat(viewModel.currentAuthenticationKey).isEqualTo(2)
        val result = BiometricPrompt.AuthenticationResult(null, 0)
        viewModel.setAuthenticationResult(result)

        assertThat(authResult1).isNull()
        assertThat(authResult2).isEqualTo(result)
    }

    @Test
    fun init_reconnectsToHandler_onlyValidForCorrectKey() {
        // create two handlers and authenticate with the second one
        val handler1Initial = createHandler()
        val handler2Initial = createHandler()

        handler2Initial.authenticate(bioAndCredentialInfo, null)
        viewModel.resetHandlerKey()

        // Activity recreated, handlers are recreated in the same order
        // handler1 gets key 1, should NOT reconnect
        val handler1Recreated = createHandler()
        handler1Recreated.assertInternalHandlerIsNull()

        val handler2Recreated = createHandler()
        handler2Recreated.assertInternalHandlerIsNotNull()
    }

    @Test
    fun authenticate_isKeyguardManagerNeeded_wearOS() {
        val mockContext = setupMockContext(isWatch = true)

        val handler = createHandler(context = mockContext)
        handler.authenticate(bioAndCredentialInfo, null)

        assertThat(DeviceUtils.isWearOS(mockContext)).isTrue()
        assertThat(handler.internalHandler is AuthenticationHandlerKeyguardManager).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun authenticate_isKeyguardManagerNeeded_forBiometricAndCredential() {
        val handler = createHandler()
        handler.authenticate(bioAndCredentialInfo, null)

        assertThat(viewModel.isUsingKeyguardManagerForBiometricAndCredential).isTrue()
        assertThat(handler.internalHandler is AuthenticationHandlerKeyguardManager).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun authenticate_isKeyguardManagerNeeded_forNoBiometric_api29() {
        val mockContext = setupMockContext(hasFingerprint = false, hasFace = false, hasIris = false)

        val handler = createHandler(context = mockContext)
        handler.authenticate(bioAndCredentialInfo, null)

        assertThat(
                mockContext.isKeyguardManagerNeededForNoBiometric(
                    bioAndCredentialInfo.allowedAuthenticators
                )
            )
            .isTrue()
        assertThat(handler.internalHandler is AuthenticationHandlerKeyguardManager).isTrue()
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    fun authenticate_isKeyguardManagerNeeded_forNoBiometric_apiIs28OrLower() {
        val mockContext = setupMockContext()

        val handler = createHandler(context = mockContext)
        handler.authenticate(bioAndCredentialInfo, null)

        assertThat(viewModel.isUsingKeyguardManagerForBiometricAndCredential).isFalse()
        assertThat(
                mockContext.isKeyguardManagerNeededForNoBiometric(
                    bioAndCredentialInfo.allowedAuthenticators
                )
            )
            .isTrue()
        assertThat(handler.internalHandler is AuthenticationHandlerKeyguardManager).isTrue()
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.O_MR1)
    fun authenticate_isUsingFingerprintDialog_apiIs27OrLower() {
        val handler = createHandler()
        handler.authenticate(bioAndCredentialInfo, null)

        assertThat(context.isUsingFingerprintDialog(viewModel.cryptoObject)).isTrue()
        assertThat(handler.internalHandler is AuthenticationHandlerFingerprintManager).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun authenticate_isUsingFingerprintDialog_noFingerprint_api28() {
        val mockContext = setupMockContext(hasFingerprint = false)

        val handler = createHandler(context = mockContext)
        handler.authenticate(bioOnlyInfo, null)

        assertThat(mockContext.isUsingFingerprintDialog(viewModel.cryptoObject)).isTrue()
        assertThat(handler.internalHandler is AuthenticationHandlerFingerprintManager).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @Suppress("deprecation")
    fun authenticate_isBiometricPrompt_withFingerprint_api28() {
        val mockContext = setupMockContext(hasFingerprint = true)
        val fingerprintManager = mock(android.hardware.fingerprint.FingerprintManager::class.java)
        whenever(
                mockContext.getSystemService(
                    android.hardware.fingerprint.FingerprintManager::class.java
                )
            )
            .thenReturn(fingerprintManager)

        val handler = createHandler(context = mockContext)
        handler.authenticate(bioOnlyInfo, null)

        assertThat(handler.internalHandler is AuthenticationHandlerBiometricPrompt).isTrue()
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.R)
    fun authenticate_isBiometricPrompt_api30() {
        val handler = createHandler()
        handler.authenticate(bioAndCredentialInfo, null)

        assertThat(handler.internalHandler is AuthenticationHandlerBiometricPrompt).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun authenticate_recreatesHandler_whenRequirementsChange() {
        val mockContext = setupMockContext(hasFingerprint = true)

        val handler = createHandler(context = mockContext)

        // 1. First call: Biometric only
        handler.authenticate(bioOnlyInfo, null)
        assertThat(handler.internalHandler is AuthenticationHandlerBiometricPrompt).isTrue()

        // 2. Second call: Biometric + Credential
        handler.authenticate(bioAndCredentialInfo, null)
        assertThat(handler.internalHandler is AuthenticationHandlerKeyguardManager).isTrue()
    }

    private fun setupMockContext(
        hasFingerprint: Boolean = false,
        hasFace: Boolean = false,
        hasIris: Boolean = false,
        isWatch: Boolean = false,
    ): Application {
        val mockContext = mock(Application::class.java)
        val packageManager = mock(PackageManager::class.java)
        val keyguardManager = mock(KeyguardManager::class.java)
        val resources = mock(Resources::class.java)

        whenever(mockContext.packageManager).thenReturn(packageManager)
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        whenever(mockContext.resources).thenReturn(resources)
        whenever(resources.getStringArray(anyInt())).thenReturn(emptyArray())

        whenever(mockContext.getSystemService(KeyguardManager::class.java))
            .thenReturn(keyguardManager)
        whenever(keyguardManager.isDeviceSecure).thenReturn(true)

        whenever(packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
            .thenReturn(hasFingerprint)
        whenever(packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(hasFace)
        whenever(packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS)).thenReturn(hasIris)
        whenever(packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(isWatch)

        return mockContext
    }

    private fun createHandler(
        context: Context = this.context,
        onConfirmButtonClicked: () -> Unit = {},
        mainExecutor: Executor? = null,
        authenticationCallback: BiometricPrompt.AuthenticationCallback? = null,
    ): AuthenticationHandler =
        AuthenticationHandler.create(
            context,
            testLifecycleOwner,
            viewModel,
            onConfirmButtonClicked,
            mainExecutor,
            authenticationCallback,
        )

    private val AuthenticationHandler.internalHandler
        get() = (this as DefaultAuthenticationHandler).internalHandler

    private val AuthenticationHandler.key
        get() = (this as DefaultAuthenticationHandler).key

    private fun AuthenticationHandler.assertInternalHandlerIsNull() {
        assertThat(internalHandler).isNull()
    }

    private fun AuthenticationHandler.assertInternalHandlerIsNotNull() {
        assertThat(internalHandler).isNotNull()
    }
}
