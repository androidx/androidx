/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may not use this file except in compliance with the License.
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
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.internal.data.CanceledFrom
import androidx.biometric.internal.data.FakeAuthenticationStateRepository
import androidx.biometric.internal.data.FakePromptConfigRepository
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class AuthenticationHandlerKeyguardManagerTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val promptRepository = FakePromptConfigRepository()
    private val authRepository = FakeAuthenticationStateRepository()
    private val viewModel: AuthenticationViewModel =
        AuthenticationViewModel(promptRepository, authRepository)
    private val clientExecutor: Executor = Executor { it.run() }
    private val clientAuthenticationCallback: BiometricPrompt.AuthenticationCallback = mock()

    private var isConfirmCredentialActivityLaunched = false

    private lateinit var handler: AuthenticationHandlerKeyguardManager

    @Before
    fun setUp() {
        val testLifecycleOwner = TestLifecycleOwner()

        handler =
            AuthenticationHandlerKeyguardManager(
                context = context,
                lifecycleOwner = testLifecycleOwner,
                viewModel = viewModel,
                confirmCredentialActivityLauncher = { isConfirmCredentialActivityLaunched = true },
                clientExecutor = clientExecutor,
                clientAuthenticationCallback = clientAuthenticationCallback,
            )

        testLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    @Test
    fun authenticate_launchesConfirmCredentialActivity() {
        handler.authenticate(getPromptInfo(), null)

        assertThat(isConfirmCredentialActivityLaunched).isTrue()
    }

    @Test
    fun cancelAuthentication_cancelsSignalAndSetsFlag() {
        handler.authenticate(getPromptInfo(), null)
        val cancellationSignal = viewModel.cancellationSignalProvider.biometricCancellationSignal
        assertThat(cancellationSignal.isCanceled).isFalse()

        handler.cancelAuthentication(CanceledFrom.CLIENT)

        assertThat(cancellationSignal.isCanceled).isTrue()
        assertThat(viewModel.canceledFrom).isEqualTo(CanceledFrom.CLIENT)
    }

    private fun getPromptInfo(): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Title")
            .setSubtitle("Subtitle")
            .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
}
