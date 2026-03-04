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
import androidx.biometric.AuthenticationRequest
import androidx.biometric.BiometricPrompt
import androidx.biometric.R
import androidx.biometric.internal.data.FakeAuthenticationStateRepository
import androidx.biometric.internal.data.FakePromptConfigRepository
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthenticationResultDispatcherTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val promptRepository = FakePromptConfigRepository()
    private val authRepository = FakeAuthenticationStateRepository()
    private val viewModel: AuthenticationViewModel =
        AuthenticationViewModel(promptRepository, authRepository)
    private val clientExecutor: Executor = Executor { it.run() }

    private var isConfirmCredentialActivityLaunched = false
    private var isDismissed = false
    private var authErrorCode: Int = -1
    private var authErrorString: CharSequence = ""
    private var authResult: BiometricPrompt.AuthenticationResult? = null
    private var authFallback: AuthenticationRequest.Biometric.Fallback.CustomOption? = null
    private var authFailed: Boolean = false
    private val clientAuthenticationCallback =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                this@AuthenticationResultDispatcherTest.authErrorCode = errorCode
                this@AuthenticationResultDispatcherTest.authErrorString = errString
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                this@AuthenticationResultDispatcherTest.authResult = result
            }

            override fun onFallbackSelected(
                fallback: AuthenticationRequest.Biometric.Fallback.CustomOption
            ) {
                this@AuthenticationResultDispatcherTest.authFallback = fallback
            }

            override fun onAuthenticationFailed() {
                this@AuthenticationResultDispatcherTest.authFailed = true
            }
        }

    private lateinit var dispatcher: AuthenticationResultDispatcher

    @Before
    fun setUp() {
        dispatcher =
            object :
                AuthenticationResultDispatcher(
                    context = context,
                    viewModel = viewModel,
                    clientExecutor = clientExecutor,
                    clientAuthenticationCallback = clientAuthenticationCallback,
                    confirmCredentialActivityLauncher = {
                        isConfirmCredentialActivityLaunched = true
                    },
                    dismiss = { isDismissed = true },
                ) {}
    }

    @Test
    fun testOnAuthenticationSucceeded_whenAwaitingResult_sendsSuccessAndDismisses() {
        viewModel.isAwaitingResult = true
        val result = BiometricPrompt.AuthenticationResult(null, 0)
        dispatcher.onAuthenticationSucceeded(result)

        assertThat(authResult).isEqualTo(result)
        assertThat(isDismissed).isTrue()
        assertThat(viewModel.isAwaitingResult).isFalse()
    }

    @Test
    fun testSendFallbackOptionAndDismiss_sendsFallbackAndDismisses() {
        viewModel.isAwaitingResult = true
        val fallback = AuthenticationRequest.Biometric.Fallback.CustomOption("test")
        dispatcher.sendFallbackOptionAndDismiss(fallback)

        assertThat(authFallback).isEqualTo(fallback)
        assertThat(isDismissed).isTrue()
        assertThat(viewModel.isAwaitingResult).isFalse()
    }

    @Test
    fun testOnAuthenticationSucceeded_whenNotAwaitingResult_doesNothing() {
        viewModel.isAwaitingResult = false
        val result = BiometricPrompt.AuthenticationResult(null, 0)
        dispatcher.onAuthenticationSucceeded(result)

        assertThat(authResult).isNull()
        assertThat(isDismissed).isTrue()
        assertThat(viewModel.isAwaitingResult).isFalse()
    }

    @Test
    fun testOnAuthenticationFailed_whenAwaitingResult_sendsFailure() {
        viewModel.isAwaitingResult = true
        dispatcher.onAuthenticationFailed()

        assertThat(authFailed).isTrue()
        assertThat(isDismissed).isFalse()
        assertThat(viewModel.isAwaitingResult).isTrue()
    }

    @Test
    fun testOnAuthenticationFailed_whenNotAwaitingResult_doesNothing() {
        viewModel.isAwaitingResult = false
        dispatcher.onAuthenticationFailed()

        assertThat(authFailed).isFalse()
        assertThat(isDismissed).isFalse()
    }

    @Test
    fun testOnAuthenticationError_whenAwaitingResult_sendsErrorAndDismisses() {
        viewModel.isAwaitingResult = true
        val errorCode = BiometricPrompt.ERROR_HW_UNAVAILABLE
        val errorMessage = "test error"
        dispatcher.onAuthenticationError(errorCode, errorMessage)

        assertThat(authErrorCode).isEqualTo(errorCode)
        assertThat(authErrorString).isEqualTo(errorMessage)
        assertThat(isDismissed).isTrue()
        assertThat(viewModel.isAwaitingResult).isFalse()
    }

    @Test
    fun testOnAuthenticationError_whenAwaitingResultAndNullMessage_sendsDefaultError() {
        viewModel.isAwaitingResult = true
        val errorCode = BiometricPrompt.ERROR_HW_UNAVAILABLE
        val defaultMessage = context.getString(R.string.default_error_msg)
        dispatcher.onAuthenticationError(errorCode, null)

        assertThat(authErrorCode).isEqualTo(errorCode)
        assertThat(authErrorString).isEqualTo(defaultMessage)
        assertThat(isDismissed).isTrue()
        assertThat(viewModel.isAwaitingResult).isFalse()
    }

    @Test
    fun testOnAuthenticationError_whenNotAwaitingResult_doesNothing() {
        viewModel.isAwaitingResult = false
        dispatcher.onAuthenticationError(0, "test error")

        assertThat(authErrorCode).isEqualTo(-1)
        assertThat(authErrorString).isEqualTo("")
        assertThat(isDismissed).isTrue()
        assertThat(viewModel.isAwaitingResult).isFalse()
    }

    @Test
    fun testShowKMAsFallback_runsLauncher() {
        dispatcher.showKMAsFallback()

        assertThat(isConfirmCredentialActivityLaunched).isTrue()
    }
}
