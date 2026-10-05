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
import android.os.SystemClock
import androidx.biometric.BiometricPrompt
import androidx.biometric.R
import androidx.biometric.TestActivity
import androidx.biometric.internal.isUsingFingerprintDialog
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.biometric.internal.viewmodel.FingerprintDialogModel
import androidx.biometric.utils.AuthenticatorUtils
import androidx.biometric.utils.BiometricErrorData
import androidx.lifecycle.Lifecycle
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
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class FingerprintDialogControllerTest {
    private lateinit var scenario: ActivityScenario<TestActivity>
    private lateinit var dialogController: FingerprintDialogController
    private lateinit var authenticationViewModel: AuthenticationViewModel

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        assumeTrue(context.isUsingFingerprintDialog(crypto = null))
    }

    @After
    fun tearDown() {
        // Only close the scenario if it has been initialized.
        // This is necessary because setUp may skip initialization via assumeTrue.
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    @Test
    fun whenPreAuthFails_dismissesDialog() {
        startAuthentication(fingerprintPreAuthCheck = BiometricPrompt.ERROR_NO_BIOMETRICS)

        scenario.onActivity { assertThat(dialogController.isDialogShowing).isFalse() }
    }

    @Test
    fun whenDialogIsCancelled_cancelsAuthenticationAndDismissesDialog() {
        startAuthentication()

        val cancellationSignal =
            authenticationViewModel.cancellationSignalProvider.fingerprintCancellationSignal
        assertThat(cancellationSignal.isCanceled).isFalse()
        onView(withText("Test Title")).check(matches(isDisplayed()))
        // Pressing back on the dialog triggers the cancel listener.
        pressBackUnconditionally()

        assertThat(cancellationSignal.isCanceled).isTrue()
        scenario.onActivity { assertThat(dialogController.isDialogShowing).isFalse() }
    }

    @Test
    fun whenAuthenticationSucceeds_dismissesDialog() {
        startAuthentication()
        onView(withText("Test Title")).check(matches(isDisplayed()))

        scenario.onActivity {
            authenticationViewModel.setAuthenticationResult(
                BiometricPrompt.AuthenticationResult(
                    null,
                    BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC,
                )
            )
        }
        scenario.onActivity { assertThat(dialogController.isDialogShowing).isFalse() }
    }

    @Test
    fun whenAuthenticationError_andDismissedInstantly_dismissesDialog() {
        startAuthentication()
        onView(withText("Test Title")).check(matches(isDisplayed()))

        val errorCode = BiometricPrompt.ERROR_HW_UNAVAILABLE
        val errorMessage = "test error"
        scenario.onActivity {
            // Pin the flag on the main thread so the DISMISS_INSTANTLY_DELAY_MS timer started by
            // showAlertDialog() cannot race this test into the delayed-dismiss branch.
            authenticationViewModel.fingerprintDialogModel.isDismissedInstantly = true
            authenticationViewModel.setAuthenticationError(
                BiometricErrorData(errorCode, errorMessage)
            )
        }
        scenario.onActivity { assertThat(dialogController.isDialogShowing).isFalse() }
    }

    @Test
    fun whenAuthenticationError_andNotDismissedInstantly_showsErrorThenDismisses() {
        startAuthentication()
        onView(withText("Test Title")).check(matches(isDisplayed()))

        val errorCode = BiometricPrompt.ERROR_HW_UNAVAILABLE
        val errorMessage = "test error"
        scenario.onActivity {
            authenticationViewModel.fingerprintDialogModel.isDismissedInstantly = false
            authenticationViewModel.setAuthenticationError(
                BiometricErrorData(errorCode, errorMessage)
            )
        }

        // The error is shown first, and the dialog stays up for HIDE_DIALOG_DELAY_MS.
        onView(withId(R.id.fingerprint_error)).check(matches(withText(errorMessage)))
        scenario.onActivity { assertThat(dialogController.isDialogShowing).isTrue() }

        waitUntil(timeoutMs = FingerprintDialogController.HIDE_DIALOG_DELAY_MS * 2L) {
            !dialogController.isDialogShowing
        }
    }

    @Test
    fun whenHelpMessageIsReceived_updatesDialogText() {
        startAuthentication()

        val helpMessage = "test help"
        scenario.onActivity { authenticationViewModel.setAuthenticationHelpMessage(helpMessage) }

        onView(withId(R.id.fingerprint_error)).check(matches(withText(helpMessage)))
    }

    @Test
    fun whenAuthenticationFails_updatesDialogText() {
        startAuthentication()

        scenario.onActivity { authenticationViewModel.setAuthenticationFailurePending() }

        onView(withId(R.id.fingerprint_error))
            .check(matches(withText(R.string.fingerprint_not_recognized)))
    }

    @Test
    fun whenActivityIsDestroyed_cancelsAuthentication() {
        startAuthentication()

        val cancellationSignal =
            authenticationViewModel.cancellationSignalProvider.fingerprintCancellationSignal
        assertThat(cancellationSignal.isCanceled).isFalse()
        scenario.moveToState(Lifecycle.State.DESTROYED)

        assertThat(cancellationSignal.isCanceled).isTrue()
        assertThat(dialogController.isDialogShowing).isFalse()
    }

    @Test
    fun whenReconnectedAfterConfigChange_preservesDialogState() {
        startAuthentication()

        val helpMessage = "Temporary help message"
        scenario.onActivity {
            authenticationViewModel.setAuthenticationHelpMessage(helpMessage)
            dialogController.dismiss()
            dialogController = FingerprintDialogController(it, it, authenticationViewModel)
            dialogController.showDialogForReconnect()
        }

        onView(withId(R.id.fingerprint_error)).check(matches(withText(helpMessage)))
    }

    private fun startAuthentication(
        fingerprintPreAuthCheck: Int = BiometricPrompt.BIOMETRIC_SUCCESS
    ) {
        val fingerprintDialogModel =
            FingerprintDialogModel(fingerprintPreAuthChecker = { _ -> fingerprintPreAuthCheck })
        fingerprintDialogModel.isDismissedInstantly = true
        authenticationViewModel =
            AuthenticationViewModel(fingerprintDialogModel = fingerprintDialogModel)
        authenticationViewModel.setPromptInfo(getPromptInfo())

        scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            dialogController =
                FingerprintDialogController(activity, activity, authenticationViewModel)
            dialogController.showAuthentication()
        }
    }

    /** Polls [condition] on the main thread until it is true or [timeoutMs] elapses. */
    private fun waitUntil(timeoutMs: Long, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (true) {
            var satisfied = false
            scenario.onActivity { satisfied = condition() }
            if (satisfied) {
                return
            }
            if (SystemClock.uptimeMillis() >= deadline) {
                throw AssertionError("Condition not met within $timeoutMs ms")
            }
            SystemClock.sleep(50)
        }
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
