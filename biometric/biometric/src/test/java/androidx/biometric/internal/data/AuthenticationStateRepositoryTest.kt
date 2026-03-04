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
 * distributed under the License is an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.biometric.internal.data

import androidx.biometric.AuthenticationRequest
import androidx.biometric.BiometricPrompt
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
class AuthenticationStateRepositoryTest {
    private val repository: AuthenticationStateRepository = AuthenticationStateRepositoryImpl()

    @Test
    fun testSetAuthenticationResult() =
        runTest(UnconfinedTestDispatcher()) {
            var actualResult: BiometricPrompt.AuthenticationResult? = null
            val job = launch { repository.authenticationResult.collect { actualResult = it } }

            val expectedResult =
                BiometricPrompt.AuthenticationResult(
                    null,
                    BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC,
                )
            repository.setAuthenticationResult(expectedResult)
            runCurrent()

            assertThat(actualResult).isEqualTo(expectedResult)
            job.cancel()
        }

    @Test
    fun testSetAuthenticationError() =
        runTest(UnconfinedTestDispatcher()) {
            var actualError: BiometricErrorData? = null
            val job = launch { repository.authenticationError.collect { actualError = it } }

            val expectedError = BiometricErrorData(BiometricPrompt.ERROR_HW_UNAVAILABLE, "Error")
            repository.setAuthenticationError(expectedError)
            runCurrent()

            assertThat(actualError).isEqualTo(expectedError)
            job.cancel()
        }

    @Test
    fun testSetAuthenticationHelpMessage() =
        runTest(UnconfinedTestDispatcher()) {
            var actualHelpMessage: CharSequence? = null
            val job = launch {
                repository.authenticationHelpMessage.collect { actualHelpMessage = it }
            }

            val expectedHelpMessage = "Help message"
            repository.setAuthenticationHelpMessage(expectedHelpMessage)
            runCurrent()

            assertThat(actualHelpMessage).isEqualTo(expectedHelpMessage)
            job.cancel()
        }

    @Test
    fun testSetAuthenticationFailurePending() =
        runTest(UnconfinedTestDispatcher()) {
            var failurePending = false
            val job = launch {
                repository.isAuthenticationFailurePending.collect { failurePending = true }
            }

            repository.setAuthenticationFailurePending()
            runCurrent()

            assertThat(failurePending).isTrue()
            job.cancel()
        }

    @Test
    fun testSetNegativeButtonPressPending() =
        runTest(UnconfinedTestDispatcher()) {
            var negativeButtonPressPending = false
            val job = launch {
                repository.isNegativeButtonPressPending.collect {
                    negativeButtonPressPending = true
                }
            }

            repository.setNegativeButtonPressPending()
            runCurrent()

            assertThat(negativeButtonPressPending).isTrue()
            job.cancel()
        }

    @Test
    fun testSetFallbackOptionPressPending() =
        runTest(UnconfinedTestDispatcher()) {
            var actualFallback: AuthenticationRequest.Biometric.Fallback.CustomOption? = null
            val job = launch {
                repository.isFallbackOptionPressPending.collect { actualFallback = it }
            }

            val expectedFallback = AuthenticationRequest.Biometric.Fallback.CustomOption("test")
            repository.setFallbackOptionPressPending(expectedFallback)
            runCurrent()

            assertThat(actualFallback).isEqualTo(expectedFallback)
            job.cancel()
        }

    @Test
    fun testSetMoreOptionsButtonPressPending() =
        runTest(UnconfinedTestDispatcher()) {
            var moreOptionsPressPending = false
            val job = launch {
                repository.isMoreOptionsButtonPressPending.collect {
                    moreOptionsPressPending = true
                }
            }

            repository.setMoreOptionsButtonPressPending()
            runCurrent()

            assertThat(moreOptionsPressPending).isTrue()
            job.cancel()
        }
}
