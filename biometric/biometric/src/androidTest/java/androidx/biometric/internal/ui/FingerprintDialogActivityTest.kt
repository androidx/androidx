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

import android.content.Context
import android.hardware.biometrics.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.R
import androidx.biometric.internal.isUsingFingerprintDialog
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.biometric.internal.viewmodel.AuthenticationViewModelFactory
import androidx.biometric.internal.viewmodel.FingerprintDialogViewModel
import androidx.biometric.utils.AuthenticatorUtils
import androidx.biometric.utils.BiometricErrorData
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.lifecycle.LifecycleOwnerUtils.waitUntilState
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class FingerprintDialogActivityTest {
    private lateinit var scenario: ActivityScenario<FingerprintDialogActivity>
    private val authenticationViewModel: AuthenticationViewModel =
        AuthenticationViewModelFactory().create(AuthenticationViewModel::class.java)

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        assumeTrue(context.isUsingFingerprintDialog(authenticationViewModel.cryptoObject))
    }

    @After
    fun tearDown() {
        FingerprintDialogActivity.fingerprintDialogViewModelFactory = null
        // Only close the scenario if it has been initialized.
        // This is necessary because setUp may skip initialization via assumeTrue.
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    @Test
    fun whenPreAuthFails_activityFinishes() {
        startActivity(fingerprintPreAuthCheck = BiometricPrompt.ERROR_NO_BIOMETRICS)

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun whenDialogIsCancelled_cancelsAuthenticationAndFinishes() {
        startActivity()

        val cancellationSignal =
            authenticationViewModel.cancellationSignalProvider.fingerprintCancellationSignal
        assertThat(cancellationSignal.isCanceled).isFalse()
        onView(withText("Test Title")).check(matches(isDisplayed()))
        // Pressing back on the dialog triggers the cancel listener.
        pressBackUnconditionally()

        assertThat(cancellationSignal.isCanceled).isTrue()
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun whenAuthenticationSucceeds_finishesActivity() {
        startActivity()
        onView(withText("Test Title")).check(matches(isDisplayed()))

        lateinit var activityFromScenario: FingerprintDialogActivity
        scenario.onActivity { activity ->
            activityFromScenario = activity
            authenticationViewModel.setAuthenticationResult(
                BiometricPrompt.AuthenticationResult(
                    null,
                    BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC,
                )
            )
        }
        waitUntilState(activityFromScenario, Lifecycle.State.DESTROYED)
    }

    @Test
    fun whenAuthenticationError_finishesActivity() {
        startActivity()
        onView(withText("Test Title")).check(matches(isDisplayed()))

        val errorCode = BiometricPrompt.ERROR_HW_UNAVAILABLE
        val errorMessage = "test error"
        lateinit var activityFromScenario: FingerprintDialogActivity
        scenario.onActivity { activity ->
            activityFromScenario = activity
            authenticationViewModel.setAuthenticationError(
                BiometricErrorData(errorCode, errorMessage)
            )
        }
        waitUntilState(activityFromScenario, Lifecycle.State.DESTROYED)
    }

    @Test
    fun whenHelpMessageIsReceived_updatesDialogText() {
        startActivity()

        val helpMessage = "test help"
        scenario.onActivity { authenticationViewModel.setAuthenticationHelpMessage(helpMessage) }

        onView(withId(R.id.fingerprint_error)).check(matches(withText(helpMessage)))
    }

    @Test
    fun whenAuthenticationFails_updatesDialogText() {
        startActivity()

        scenario.onActivity { authenticationViewModel.setAuthenticationFailurePending() }

        onView(withId(R.id.fingerprint_error))
            .check(matches(withText(R.string.fingerprint_not_recognized)))
    }

    @Test
    fun whenActivityIsDestroyed_cancelsAuthentication() {
        startActivity()

        val cancellationSignal =
            authenticationViewModel.cancellationSignalProvider.fingerprintCancellationSignal
        assertThat(cancellationSignal.isCanceled).isFalse()
        scenario.moveToState(Lifecycle.State.DESTROYED)

        assertThat(cancellationSignal.isCanceled).isTrue()
    }

    private fun startActivity(fingerprintPreAuthCheck: Int = BiometricPrompt.BIOMETRIC_SUCCESS) {
        val fingerprintDialogViewModel =
            FingerprintDialogViewModel(fingerprintPreAuthChecker = { _ -> fingerprintPreAuthCheck })
        fingerprintDialogViewModel.isDismissedInstantly = true

        FingerprintDialogActivity.fingerprintDialogViewModelFactory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return fingerprintDialogViewModel as T
                }
            }

        authenticationViewModel.setPromptInfo(getPromptInfo())

        scenario = ActivityScenario.launch(FingerprintDialogActivity::class.java)
    }

    private fun getPromptInfo(
        title: CharSequence = "Test Title",
        subtitle: CharSequence? = "Test Subtitle",
        description: CharSequence? = "Test Description",
        negativeButtonText: CharSequence = "Test Button",
        authenticators: Int = BiometricManager.Authenticators.BIOMETRIC_WEAK,
    ): BiometricPrompt.PromptInfo {
        val builder =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
        if (!AuthenticatorUtils.isDeviceCredentialAllowed(authenticators)) {
            builder.setNegativeButtonText(negativeButtonText)
        }
        builder.setAllowedAuthenticators(authenticators)
        return builder.build()
    }
}
