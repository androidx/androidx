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
import android.hardware.biometrics.BiometricManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResultLauncher
import androidx.biometric.BiometricPrompt
import androidx.biometric.internal.data.CanceledFrom
import androidx.biometric.internal.data.FakeAuthenticationStateRepository
import androidx.biometric.internal.data.FakePromptConfigRepository
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.biometric.internal.viewmodel.FingerprintDialogModel
import androidx.biometric.registerForAuthenticationResult
import androidx.biometric.utils.AuthenticatorUtils
import androidx.biometric.utils.BiometricErrorData
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowKeyguardManager

private const val NEGATIVE_BUTTON_TEXT = "test"

@RunWith(AndroidJUnit4::class)
class AuthenticationHandlerFingerprintManagerTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val promptRepository = FakePromptConfigRepository()
    private val authRepository = FakeAuthenticationStateRepository()
    private val viewModel: AuthenticationViewModel =
        AuthenticationViewModel(
            promptRepository,
            authRepository,
            // Robolectric has no fingerprint hardware; treat the pre-auth check as passing so that
            // authenticate() reaches the sensor call instead of failing with ERROR_HW_NOT_PRESENT.
            FingerprintDialogModel(
                fingerprintPreAuthChecker = {
                    isPreAuthCheckInvoked = true
                    BiometricPrompt.BIOMETRIC_SUCCESS
                }
            ),
        )
    private val clientExecutor: Executor = Executor { it.run() }

    private var isConfirmCredentialActivityLaunched = false
    private var onConfirmCredentialLaunched: (() -> Unit)? = null
    private var isPreAuthCheckInvoked = false
    private var errorCode: Int = -1
    private var fallback: AuthenticationRequest.Biometric.Fallback.CustomOption? = null
    private val clientAuthenticationCallback =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                this@AuthenticationHandlerFingerprintManagerTest.errorCode = errorCode
            }

            override fun onFallbackSelected(
                fallback: AuthenticationRequest.Biometric.Fallback.CustomOption
            ) {
                this@AuthenticationHandlerFingerprintManagerTest.fallback = fallback
            }
        }

    private lateinit var authenticationHandler: AuthenticationHandlerFingerprintManager

    @Before
    fun setUp() {
        val shadowKeyguardManager: ShadowKeyguardManager =
            shadowOf(context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
        shadowKeyguardManager.setIsDeviceSecure(true)
        val testLifecycleOwner = TestLifecycleOwner()
        testLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

        authenticationHandler =
            AuthenticationHandlerFingerprintManager(
                AuthenticationManager(
                    context = context,
                    lifecycleOwner = testLifecycleOwner,
                    viewModel = viewModel,
                    confirmCredentialActivityLauncher = {
                        isConfirmCredentialActivityLaunched = true
                        onConfirmCredentialLaunched?.invoke()
                    },
                    clientExecutor = clientExecutor,
                    clientAuthenticationCallback = clientAuthenticationCallback,
                    onDismissed = {},
                )
            )
    }

    @Test
    fun cancelAuthentication_whenIgnoringCancel_doesNothing() {
        viewModel.isIgnoringCancel = true
        val cancellationSignal = viewModel.cancellationSignalProvider.fingerprintCancellationSignal
        assertThat(cancellationSignal.isCanceled).isFalse()

        authenticationHandler.cancelAuthentication(CanceledFrom.USER)

        assertThat(cancellationSignal.isCanceled).isFalse()
    }

    @Test
    fun cancelAuthentication_whenNotIgnoringCancel_doesCancellation() {
        viewModel.isIgnoringCancel = false
        val cancellationSignal = viewModel.cancellationSignalProvider.fingerprintCancellationSignal
        assertThat(cancellationSignal.isCanceled).isFalse()

        authenticationHandler.cancelAuthentication(CanceledFrom.USER)

        assertThat(cancellationSignal.isCanceled).isTrue()
        assertThat(viewModel.canceledFrom).isEqualTo(CanceledFrom.USER)
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    fun onAuthenticationError_lockoutWithDeviceCredential_showsKeyguard() {
        authenticationHandler.authenticate(
            getPromptInfo(
                BiometricManager.Authenticators.DEVICE_CREDENTIAL or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            ),
            null,
        )

        viewModel.setAuthenticationError(
            BiometricErrorData(BiometricPrompt.ERROR_LOCKOUT, "Lockout")
        )

        assertThat(isConfirmCredentialActivityLaunched).isTrue()
    }

    @Test
    fun onAuthenticationError_canceledFromClient_sendsErrorToClient() {
        authenticationHandler.authenticate(getPromptInfo(), null)
        viewModel.canceledFrom = CanceledFrom.CLIENT

        viewModel.setAuthenticationError(
            BiometricErrorData(BiometricPrompt.ERROR_CANCELED, "Canceled")
        )

        assertThat(errorCode).isEqualTo(BiometricPrompt.ERROR_CANCELED)
    }

    @Test
    fun onAuthenticationError_canceledFromInternal_sendsErrorToClient() {
        authenticationHandler.authenticate(getPromptInfo(), null)
        viewModel.canceledFrom = CanceledFrom.INTERNAL

        viewModel.setAuthenticationError(
            BiometricErrorData(BiometricPrompt.ERROR_CANCELED, "Canceled")
        )

        assertThat(errorCode).isEqualTo(BiometricPrompt.ERROR_CANCELED)
    }

    @Test
    fun onAuthenticationError_canceledFromUser_doesNotSendErrorToClient() {
        authenticationHandler.authenticate(getPromptInfo(), null)
        viewModel.canceledFrom = CanceledFrom.USER

        viewModel.setAuthenticationError(
            BiometricErrorData(BiometricPrompt.ERROR_CANCELED, "Canceled")
        )

        assertThat(errorCode).isEqualTo(-1)
    }

    @Test
    fun onAuthenticationError_otherError_sendsErrorToClient() {
        authenticationHandler.authenticate(getPromptInfo(), null)

        viewModel.setAuthenticationError(
            BiometricErrorData(BiometricPrompt.ERROR_HW_UNAVAILABLE, "HW unavailable")
        )

        assertThat(errorCode).isEqualTo(BiometricPrompt.ERROR_HW_UNAVAILABLE)
    }

    @Test
    fun onNegativeButtonPressed_sendsFallbackOptionAndCancels() {
        authenticationHandler.authenticate(getPromptInfo(), null)
        val cancellationSignal = viewModel.cancellationSignalProvider.fingerprintCancellationSignal
        assertThat(cancellationSignal.isCanceled).isFalse()

        viewModel.setNegativeButtonPressPending()

        assertThat(fallback?.text).isEqualTo(NEGATIVE_BUTTON_TEXT)
        assertThat(cancellationSignal.isCanceled).isTrue()
        assertThat(viewModel.canceledFrom).isEqualTo(CanceledFrom.NEGATIVE_BUTTON)
    }

    @Test
    fun onDefaultCancelButtonPressed_sendsErrorAndCancels() {
        val authenticators = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
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

    @Test
    @Config(maxSdk = Build.VERSION_CODES.O_MR1)
    fun authenticate_withActivityContext_showsInWindowAlertDialogWithoutStartingActivity() {
        val activityController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = activityController.get()

        val handler =
            AuthenticationHandlerFingerprintManager(
                AuthenticationManager(
                    context = activity,
                    lifecycleOwner = activity,
                    viewModel = viewModel,
                    confirmCredentialActivityLauncher = {
                        isConfirmCredentialActivityLaunched = true
                    },
                    clientExecutor = clientExecutor,
                    clientAuthenticationCallback = clientAuthenticationCallback,
                    onDismissed = {},
                )
            )

        handler.authenticate(getPromptInfo(), null)

        assertThat(shadowOf(context).nextStartedActivity).isNull()
        val latestDialog = ShadowAlertDialog.getLatestAlertDialog()
        assertThat(latestDialog).isNotNull()
        assertThat(latestDialog.isShowing).isTrue()

        handler.cancelAuthentication(CanceledFrom.CLIENT)
        assertThat(latestDialog.isShowing).isFalse()
        activityController.destroy()
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.O_MR1)
    fun authenticate_withNonActivityContextWrapper_showsInWindowAlertDialog() {
        val activityController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = activityController.get()
        // Models a host Context (such as GMS Core ChimeraX Activity) that extends ContextWrapper
        // around Application rather than android.app.Activity, delegating window/inflater services.
        val nonActivityUiContext =
            object : android.content.ContextWrapper(context) {
                override fun getSystemService(name: String): Any? {
                    if (Context.WINDOW_SERVICE == name || Context.LAYOUT_INFLATER_SERVICE == name) {
                        return activity.getSystemService(name)
                    }
                    return super.getSystemService(name)
                }
            }

        val handler =
            AuthenticationHandlerFingerprintManager(
                AuthenticationManager(
                    context = nonActivityUiContext,
                    lifecycleOwner = activity,
                    viewModel = viewModel,
                    confirmCredentialActivityLauncher = {
                        isConfirmCredentialActivityLaunched = true
                    },
                    clientExecutor = clientExecutor,
                    clientAuthenticationCallback = clientAuthenticationCallback,
                    onDismissed = {},
                )
            )

        handler.authenticate(getPromptInfo(), null)

        val latestDialog = ShadowAlertDialog.getLatestAlertDialog()
        assertThat(latestDialog).isNotNull()
        assertThat(latestDialog.isShowing).isTrue()

        handler.cancelAuthentication(CanceledFrom.CLIENT)
        assertThat(latestDialog.isShowing).isFalse()
        activityController.destroy()
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    fun showKMAsFallback_launchesConfirmCredentialBeforeCancelingSensor() {
        authenticationHandler.authenticate(
            getPromptInfo(
                BiometricManager.Authenticators.DEVICE_CREDENTIAL or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            ),
            null,
        )
        val cancellationSignal = viewModel.cancellationSignalProvider.fingerprintCancellationSignal
        var wasSensorCanceledAtLaunch: Boolean? = null
        onConfirmCredentialLaunched = { wasSensorCanceledAtLaunch = cancellationSignal.isCanceled }

        viewModel.setAuthenticationError(
            BiometricErrorData(BiometricPrompt.ERROR_LOCKOUT, "Lockout")
        )

        // The credential screen must be launched (which sets isConfirmingDeviceCredential) before
        // the sensor is canceled, so the resulting ERROR_CANCELED is suppressed deterministically.
        assertThat(isConfirmCredentialActivityLaunched).isTrue()
        assertThat(wasSensorCanceledAtLaunch).isFalse()
        assertThat(cancellationSignal.isCanceled).isTrue()
        assertThat(errorCode).isEqualTo(-1)
    }

    @Test
    fun authenticate_withApplicationContext_startsAuthenticationWithoutDialog() {
        // The default fixture uses an Application context, which has no window for a dialog.
        authenticationHandler.authenticate(getPromptInfo(), null)

        assertThat(ShadowAlertDialog.getLatestAlertDialog()).isNull()
        assertThat(isPreAuthCheckInvoked).isTrue()
        assertThat(viewModel.isAwaitingResult).isTrue()
        assertThat(errorCode).isEqualTo(-1)

        // The client can still cancel and receive a result.
        viewModel.canceledFrom = CanceledFrom.CLIENT
        viewModel.setAuthenticationError(
            BiometricErrorData(BiometricPrompt.ERROR_CANCELED, "Canceled")
        )
        assertThat(errorCode).isEqualTo(BiometricPrompt.ERROR_CANCELED)
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.O_MR1)
    fun authenticate_whenHostActivityFinishing_sendsCanceledErrorToClient() {
        val activityController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = activityController.get()
        val handler =
            AuthenticationHandlerFingerprintManager(
                AuthenticationManager(
                    context = activity,
                    lifecycleOwner = activity,
                    viewModel = viewModel,
                    confirmCredentialActivityLauncher = {
                        isConfirmCredentialActivityLaunched = true
                    },
                    clientExecutor = clientExecutor,
                    clientAuthenticationCallback = clientAuthenticationCallback,
                    onDismissed = {},
                )
            )
        activity.finish()

        handler.authenticate(getPromptInfo(), null)

        assertThat(ShadowAlertDialog.getLatestAlertDialog()).isNull()
        assertThat(isPreAuthCheckInvoked).isFalse()
        assertThat(errorCode).isEqualTo(BiometricPrompt.ERROR_CANCELED)
        assertThat(viewModel.isAwaitingResult).isFalse()
        activityController.destroy()
    }

    @Test
    fun fragmentRegisterForAuthenticationResult_beforeOnAttach_doesNotThrow() {
        class UnattachedFragment : Fragment() {
            val launcher: AuthenticationResultLauncher = registerForAuthenticationResult {}
        }

        val fragment = UnattachedFragment()
        assertThat(fragment.launcher).isNotNull()
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
