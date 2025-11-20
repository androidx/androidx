/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law of agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.biometric.internal

import android.app.Application
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class AuthenticationManagerTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val promptRepository = FakePromptConfigRepository()
    private val authRepository = FakeAuthenticationStateRepository()
    private val viewModel: AuthenticationViewModel =
        AuthenticationViewModel(promptRepository, authRepository)
    private val clientExecutor: Executor = Executor { it.run() }

    private var isConfirmCredentialActivityLaunched = false
    private var isAuthenticationShown = false
    private var authErrorCode: Int = -1
    private var authErrorString: CharSequence = ""
    private var authResult: BiometricPrompt.AuthenticationResult? = null
    private var authFailed: Boolean = false
    private val clientAuthenticationCallback =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                this@AuthenticationManagerTest.authErrorCode = errorCode
                this@AuthenticationManagerTest.authErrorString = errString
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                this@AuthenticationManagerTest.authResult = result
            }

            override fun onAuthenticationFailed() {
                this@AuthenticationManagerTest.authFailed = true
            }
        }

    private lateinit var manager: AuthenticationManager

    @Before
    fun setUp() {
        val testLifecycleOwner = TestLifecycleOwner()

        manager =
            AuthenticationManager(
                context = context,
                lifecycleOwner = testLifecycleOwner,
                viewModel = viewModel,
                confirmCredentialActivityLauncher = { isConfirmCredentialActivityLaunched = true },
                clientExecutor = clientExecutor,
                clientAuthenticationCallback = clientAuthenticationCallback,
            )
        manager.initialize()

        testLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    @Test
    fun authenticate_preparesAuthAndShowsPrompt() {
        val promptInfo = getPromptInfo()
        manager.authenticate(promptInfo, null) { isAuthenticationShown = true }

        assertThat(isAuthenticationShown).isTrue()
        assertThat(viewModel.isPromptShowing).isTrue()
        assertThat(viewModel.isAwaitingResult).isTrue()
        assertThat(viewModel.title).isEqualTo(promptInfo.title)
        assertThat(viewModel.negativeButtonText).isEqualTo(promptInfo.negativeButtonText)
        assertThat(viewModel.allowedAuthenticators).isEqualTo(promptInfo.allowedAuthenticators)
    }

    @Test
    fun authenticate_withDelay_showsPromptAfterDelay() {
        val testDispatcher = StandardTestDispatcher()
        try {
            Dispatchers.setMain(testDispatcher)
            runTest(testDispatcher) {
                viewModel.isDelayingPrompt = true
                val promptInfo = getPromptInfo()
                manager.authenticate(promptInfo, null) { isAuthenticationShown = true }

                assertThat(isAuthenticationShown).isFalse()
                assertThat(viewModel.isPromptShowing).isFalse()

                testDispatcher.scheduler.advanceUntilIdle()

                assertThat(isAuthenticationShown).isTrue()
                assertThat(viewModel.isPromptShowing).isTrue()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun cancelAuthentication_cancels() {
        val cancellationSignal = viewModel.cancellationSignalProvider.fingerprintCancellationSignal
        assertThat(cancellationSignal.isCanceled).isFalse()
        manager.authenticate(getPromptInfo(), null) {}

        manager.cancelAuthentication(CanceledFrom.CLIENT)

        assertThat(cancellationSignal.isCanceled).isTrue()
        assertThat(viewModel.canceledFrom).isEqualTo(CanceledFrom.CLIENT)
    }

    @Test
    fun cancelAuthentication_whenIgnoringCancel_doesNothingForUserCancel() {
        val cancellationSignal = viewModel.cancellationSignalProvider.fingerprintCancellationSignal
        assertThat(cancellationSignal.isCanceled).isFalse()
        manager.authenticate(getPromptInfo(), null) {}
        viewModel.isIgnoringCancel = true

        manager.cancelAuthentication(CanceledFrom.USER)

        assertThat(cancellationSignal.isCanceled).isFalse()
    }

    @Test
    fun cancelAuthentication_whenIgnoringCancel_stillWorksForClientCancel() {
        val cancellationSignal = viewModel.cancellationSignalProvider.fingerprintCancellationSignal
        assertThat(cancellationSignal.isCanceled).isFalse()
        manager.authenticate(getPromptInfo(), null) {}
        viewModel.isIgnoringCancel = true

        manager.cancelAuthentication(CanceledFrom.CLIENT)

        assertThat(cancellationSignal.isCanceled).isTrue()
        assertThat(viewModel.canceledFrom).isEqualTo(CanceledFrom.CLIENT)
    }

    @Test
    fun dismiss_updatesViewModelAndDestroys() {
        manager.authenticate(getPromptInfo(), null) {}

        manager.dismiss()

        assertThat(viewModel.isPromptShowing).isFalse()
        assertThat(viewModel.isConfirmingDeviceCredential).isFalse()
    }

    @Test
    fun onAuthenticationResult_isDispatched() {
        manager.authenticate(getPromptInfo(), null) {}
        val result = BiometricPrompt.AuthenticationResult(null, 0)

        viewModel.setAuthenticationResult(result)

        assertThat(authResult).isEqualTo(result)
    }

    @Test
    fun onAuthenticationError_isDispatched() {
        manager.authenticate(getPromptInfo(), null) {}
        val error = BiometricErrorData(1, "error")

        viewModel.setAuthenticationError(error)

        assertThat(authErrorCode).isEqualTo(error.errorCode)
        assertThat(authErrorString).isEqualTo(error.errorMessage)
    }

    @Test
    fun onAuthenticationFailure_isDispatched() {
        manager.authenticate(getPromptInfo(), null) {}

        viewModel.setAuthenticationFailurePending()

        assertThat(authFailed).isTrue()
    }

    @Test
    fun onAuthenticationResult_notDispatchedWhenNotShowing() {
        val result = BiometricPrompt.AuthenticationResult(null, 0)

        viewModel.setAuthenticationResult(result)

        assertThat(authResult).isNull()
    }

    private fun getPromptInfo(
        authenticators: Int = BiometricManager.Authenticators.BIOMETRIC_WEAK
    ): BiometricPrompt.PromptInfo {
        val builder = BiometricPrompt.PromptInfo.Builder().setTitle("test")
        if (!AuthenticatorUtils.isDeviceCredentialAllowed(authenticators)) {
            builder.setNegativeButtonText("test")
        }
        builder.setAllowedAuthenticators(authenticators)
        return builder.build()
    }
}
