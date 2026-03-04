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

package androidx.biometric.internal.viewmodel

import androidx.biometric.AuthenticationRequest
import androidx.biometric.BiometricPrompt
import androidx.biometric.internal.data.FakeAuthenticationStateRepository
import androidx.biometric.internal.data.FakePromptConfigRepository
import androidx.biometric.utils.BiometricErrorData
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticationViewModelTest {
    private val promptRepository = FakePromptConfigRepository()
    private val authRepository = FakeAuthenticationStateRepository()
    private val viewModel: AuthenticationViewModel =
        AuthenticationViewModel(promptRepository, authRepository)

    @Test
    fun testAuthenticationResult() =
        runTest(UnconfinedTestDispatcher()) {
            var result: BiometricPrompt.AuthenticationResult? = null
            val job = launch { viewModel.authenticationResult.collect { result = it } }

            val expectedResult =
                BiometricPrompt.AuthenticationResult(
                    null,
                    BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC,
                )
            authRepository.setAuthenticationResult(expectedResult)
            runCurrent()

            assertThat(result).isEqualTo(expectedResult)
            job.cancel()
        }

    @Test
    fun testAuthenticationError() =
        runTest(UnconfinedTestDispatcher()) {
            var error: BiometricErrorData? = null
            val job = launch { viewModel.authenticationError.collect { error = it } }

            val expectedError = BiometricErrorData(BiometricPrompt.ERROR_HW_UNAVAILABLE, "Error")
            authRepository.setAuthenticationError(expectedError)
            runCurrent()

            assertThat(error).isEqualTo(expectedError)
            job.cancel()
        }

    @Test
    fun testAuthenticationHelpMessage() =
        runTest(UnconfinedTestDispatcher()) {
            var help: CharSequence? = null
            val job = launch { viewModel.authenticationHelpMessage.collect { help = it } }

            authRepository.setAuthenticationHelpMessage("Help message")
            runCurrent()

            assertThat(help).isEqualTo("Help message")
            job.cancel()
        }

    @Test
    fun testAuthenticationFailurePending() =
        runTest(UnconfinedTestDispatcher()) {
            var failurePending = false
            val job = launch {
                viewModel.isAuthenticationFailurePending.collect { failurePending = true }
            }

            authRepository.setAuthenticationFailurePending()
            runCurrent()

            assertThat(failurePending).isTrue()
            job.cancel()
        }

    @Test
    fun testNegativeButtonPressPending() =
        runTest(UnconfinedTestDispatcher()) {
            var negativeButtonPressPending = false
            val job = launch {
                viewModel.isNegativeButtonPressPending.collect { negativeButtonPressPending = true }
            }

            authRepository.setNegativeButtonPressPending()
            runCurrent()

            assertThat(negativeButtonPressPending).isTrue()
            job.cancel()
        }

    @Test
    fun testFallbackOptionPressPending() =
        runTest(UnconfinedTestDispatcher()) {
            var actualFallback: AuthenticationRequest.Biometric.Fallback.CustomOption? = null
            val job = launch {
                viewModel.isFallbackOptionPressPending.collect { actualFallback = it }
            }

            val expectedFallback = AuthenticationRequest.Biometric.Fallback.CustomOption("test")
            authRepository.setFallbackOptionPressPending(expectedFallback)
            runCurrent()

            assertThat(actualFallback).isEqualTo(expectedFallback)
            job.cancel()
        }

    @Test
    fun testMoreOptionsButtonPressPending() =
        runTest(UnconfinedTestDispatcher()) {
            var moreOptionsPressPending = false
            val job = launch {
                viewModel.isMoreOptionsButtonPressPending.collect { moreOptionsPressPending = true }
            }

            authRepository.setMoreOptionsButtonPressPending()
            runCurrent()

            assertThat(moreOptionsPressPending).isTrue()
            job.cancel()
        }

    @Test
    fun testGenerateNextManagerKey() {
        assertThat(viewModel.generateNextManagerKey()).isEqualTo(1)
        assertThat(viewModel.generateNextManagerKey()).isEqualTo(2)
    }

    @Test
    fun testResetManagerKey() {
        viewModel.generateNextManagerKey()
        viewModel.generateNextManagerKey()

        viewModel.resetManagerKey()

        assertThat(viewModel.generateNextManagerKey()).isEqualTo(1)
    }

    @Test
    fun testCurrentAuthenticationKey() {
        assertThat(viewModel.currentAuthenticationKey).isEqualTo(0)

        viewModel.currentAuthenticationKey = 1
        assertThat(viewModel.currentAuthenticationKey).isEqualTo(1)
    }
}
