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

package androidx.biometric.internal.ui

import android.app.Activity
import android.app.Application
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.biometric.BiometricPrompt
import androidx.biometric.internal.data.FakeAuthenticationStateRepository
import androidx.biometric.internal.data.FakePromptConfigRepository
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.biometric.utils.BiometricErrorData
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowKeyguardManager

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmCredentialUiLauncherTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val shadowKeyguardManager: ShadowKeyguardManager =
        Shadows.shadowOf(context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)

    private val mockViewModelStoreOwner: ViewModelStoreOwner = mock()
    private val viewModelStore = ViewModelStore()
    private val promptRepository = FakePromptConfigRepository()
    private val authRepository = FakeAuthenticationStateRepository()
    private val viewModel: AuthenticationViewModel =
        AuthenticationViewModel(promptRepository, authRepository)

    @Before
    fun setUp() {
        viewModelStore.put(
            "androidx.lifecycle.ViewModelProvider.DefaultKey:" +
                "${AuthenticationViewModel::class.java.canonicalName}",
            viewModel,
        )
        whenever(mockViewModelStoreOwner.viewModelStore).thenReturn(viewModelStore)

        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        viewModelStore.clear()
    }

    @Test
    fun handleConfirmCredentialResult_withViewModelStoreOwner_resultOk() =
        runTest(UnconfinedTestDispatcher()) {
            val mockContext: Context = mock()
            whenever(mockContext.getString(any())).thenReturn("test")
            var authenticationResult: BiometricPrompt.AuthenticationResult? = null
            val job = launch {
                viewModel.authenticationResult.collect { authenticationResult = it }
            }

            handleConfirmCredentialResult(mockContext, viewModel, Activity.RESULT_OK)
            runCurrent()

            assertThat(authenticationResult).isNotNull()
            job.cancel()
        }

    @Test
    fun launchConfirmCredentialActivity_withLauncher_launchesIntent() {
        shadowKeyguardManager.setIsDeviceSecure(true)

        val mockLauncher: ActivityResultLauncher<Intent> = mock()

        context.launchConfirmCredentialActivity(mockViewModelStoreOwner, mockLauncher)

        val intentCaptor = argumentCaptor<Intent>()
        verify(mockLauncher).launch(intentCaptor.capture())
        assertThat(intentCaptor.lastValue.action)
            .isEqualTo("android.app.action.CONFIRM_DEVICE_CREDENTIAL")
        assertThat(intentCaptor.lastValue.flags and Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            .isNotEqualTo(0)
        assertThat(intentCaptor.lastValue.flags and Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            .isNotEqualTo(0)
    }

    @Test
    fun launchConfirmCredentialActivity_withLauncher_noIntent_doesNotLaunch() {
        // This makes getConfirmCredentialIntent return null
        shadowKeyguardManager.setIsDeviceSecure(false)
        val mockLauncher: ActivityResultLauncher<Intent> = mock()

        context.launchConfirmCredentialActivity(mockViewModelStoreOwner, mockLauncher)

        verify(mockLauncher, never()).launch(any())
    }

    @Test
    fun handleConfirmCredentialResult_resultOk_wasNotUsingKeyguardManager() =
        runTest(UnconfinedTestDispatcher()) {
            var authenticationResult: BiometricPrompt.AuthenticationResult? = null
            val job = launch {
                viewModel.authenticationResult.collect { authenticationResult = it }
            }

            viewModel.isUsingKeyguardManagerForBiometricAndCredential = false
            handleConfirmCredentialResult(context, viewModel, Activity.RESULT_OK)
            runCurrent()

            assertThat(authenticationResult).isNotNull()
            assertThat(authenticationResult!!.authenticationType)
                .isEqualTo(BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL)
            assertThat(authenticationResult.cryptoObject).isNull()
            job.cancel()
        }

    @Test
    fun handleConfirmCredentialResult_resultOk_wasUsingKeyguardManager() =
        runTest(UnconfinedTestDispatcher()) {
            var authenticationResult: BiometricPrompt.AuthenticationResult? = null
            val job = launch {
                viewModel.authenticationResult.collect { authenticationResult = it }
            }

            viewModel.isUsingKeyguardManagerForBiometricAndCredential = true
            handleConfirmCredentialResult(context, viewModel, Activity.RESULT_OK)
            runCurrent()

            assertThat(authenticationResult).isNotNull()
            assertThat(authenticationResult!!.authenticationType)
                .isEqualTo(BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN)
            assertThat(authenticationResult.cryptoObject).isNull()
            job.cancel()
        }

    @Test
    fun handleConfirmCredentialResult_resultCanceled() =
        runTest(UnconfinedTestDispatcher()) {
            var authenticationError: BiometricErrorData? = null
            val job = launch { viewModel.authenticationError.collect { authenticationError = it } }

            handleConfirmCredentialResult(context, viewModel, Activity.RESULT_CANCELED)
            runCurrent()

            assertThat(authenticationError).isNotNull()
            assertThat(authenticationError!!.errorCode)
                .isEqualTo(BiometricPrompt.ERROR_USER_CANCELED)
            job.cancel()
        }

    @Test
    fun getConfirmCredentialIntent_noKeyguardManager_setsError() =
        runTest(UnconfinedTestDispatcher()) {
            var authenticationError: BiometricErrorData? = null
            val job = launch { viewModel.authenticationError.collect { authenticationError = it } }

            val mockContext: Context = mock()
            whenever(mockContext.getSystemService(Context.KEYGUARD_SERVICE)).thenReturn(null)

            val intent = mockContext.getConfirmCredentialIntent(mockViewModelStoreOwner)
            runCurrent()

            assertThat(intent).isNull()
            assertThat(authenticationError).isNotNull()
            assertThat(authenticationError!!.errorCode)
                .isEqualTo(BiometricPrompt.ERROR_HW_NOT_PRESENT)
            job.cancel()
        }

    @Test
    fun getConfirmCredentialIntent_noDeviceCredential_setsError() =
        runTest(UnconfinedTestDispatcher()) {
            var authenticationError: BiometricErrorData? = null
            val job = launch { viewModel.authenticationError.collect { authenticationError = it } }

            shadowKeyguardManager.setIsDeviceSecure(false)

            val intent = context.getConfirmCredentialIntent(mockViewModelStoreOwner)
            runCurrent()

            assertThat(intent).isNull()
            assertThat(authenticationError).isNotNull()
            assertThat(authenticationError!!.errorCode)
                .isEqualTo(BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL)
            job.cancel()
        }

    @Test
    fun getConfirmCredentialIntent_success_returnsIntent() {
        shadowKeyguardManager.setIsDeviceSecure(true)

        val intent = context.getConfirmCredentialIntent(mockViewModelStoreOwner)

        assertThat(intent).isNotNull()
        assertThat(intent!!.action).isEqualTo("android.app.action.CONFIRM_DEVICE_CREDENTIAL")
    }
}
