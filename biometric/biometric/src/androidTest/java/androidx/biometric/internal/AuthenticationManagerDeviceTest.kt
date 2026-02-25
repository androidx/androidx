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

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.TestActivity
import androidx.biometric.internal.data.CanceledFrom
import androidx.biometric.internal.data.FakeAuthenticationStateRepository
import androidx.biometric.internal.data.FakePromptConfigRepository
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.biometric.utils.AuthenticatorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.lifecycle.LifecycleOwnerUtils.waitUntilState
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AuthenticationManagerDeviceTest {
    @get:Rule val activityRule = ActivityScenarioRule(TestActivity::class.java)

    private val promptId = android.R.id.primary
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val promptRepository = FakePromptConfigRepository()
    private val authRepository = FakeAuthenticationStateRepository()
    private val viewModel: AuthenticationViewModel =
        AuthenticationViewModel(promptRepository, authRepository)
    private val clientExecutor: Executor = Executor { it.run() }
    private val clientAuthenticationCallback = object : BiometricPrompt.AuthenticationCallback() {}

    @Test
    fun onStop_whenActivityNotChangingConfigurations_cancelAuthentication() {
        activityRule.scenario.onActivity { activity ->
            val manager = getAuthenticationManager(activity)
            manager.initialize()
            manager.authenticate(getPromptInfo(), null) { activity.showPrompt() }
            // Force isChangingConfigurations to false so isPermanentlyRemoved is true
            activity.setChangingConfigurationsOverride(false)
        }
        onView(withId(promptId)).check(matches(isDisplayed()))

        // Trigger ON_STOP
        activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        // Bring back to foreground to verify UI recovery
        activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        onView(withId(promptId)).check(doesNotExist())
    }

    @Test
    fun onStop_whenActivityChangingConfigurations_doesNotCancelAuthentication() {
        activityRule.scenario.onActivity { activity ->
            val manager = getAuthenticationManager(activity)
            manager.initialize()
            manager.authenticate(getPromptInfo(), null) { activity.showPrompt() }

            // Force isChangingConfigurations to true so isPermanentlyRemoved is false
            activity.setChangingConfigurationsOverride(true)
        }
        onView(withId(promptId)).check(matches(isDisplayed()))

        // Trigger ON_STOP
        activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        // Bring back to foreground to verify UI recovery
        activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        onView(withId(promptId)).check(matches(isDisplayed()))
    }

    @Test
    fun onDestroy_whenActivityFinishing_cancelsAuthentication() {
        lateinit var activityFromScenario: TestActivity
        val cancellationSignal = viewModel.cancellationSignalProvider.biometricCancellationSignal
        activityRule.scenario.onActivity { activity ->
            activityFromScenario = activity
            val manager = getAuthenticationManager(activity)
            manager.initialize()
            manager.authenticate(getPromptInfo(), null) { activity.showPrompt() }
        }
        onView(withId(promptId)).check(matches(isDisplayed()))

        activityRule.scenario.onActivity { activity -> activity.finish() }
        waitUntilState(activityFromScenario, Lifecycle.State.DESTROYED)

        assertThat(viewModel.canceledFrom).isEqualTo(CanceledFrom.INTERNAL)
        assertThat(cancellationSignal.isCanceled).isTrue()
    }

    @Test
    fun onDestroy_whenFragmentRemoving_cancelsAuthentication() {
        val fragment = Fragment()
        activityRule.scenario.onActivity { activity ->
            activity.supportFragmentManager.beginTransaction().add(fragment, "test").commitNow()

            val manager = getAuthenticationManager(fragment)
            manager.initialize()
            manager.authenticate(getPromptInfo(), null) { activity.showPrompt() }
        }
        onView(withId(promptId)).check(matches(isDisplayed()))

        activityRule.scenario.onActivity { activity ->
            activity.supportFragmentManager.beginTransaction().remove(fragment).commit()
            activity.supportFragmentManager.executePendingTransactions()
        }

        onView(withId(promptId)).check(doesNotExist())
    }

    @Test
    fun initialize_multipleTimes_isNoOp() {
        activityRule.scenario.onActivity { activity ->
            val manager = getAuthenticationManager(activity)
            manager.initialize()

            val firstContainer = manager.lifecycleContainer

            manager.initialize() // Second call

            // Should not re-initialize or create new containers
            assertThat(manager.lifecycleContainer).isSameInstanceAs(firstContainer)
        }
    }

    private fun getAuthenticationManager(lifecycleOwner: LifecycleOwner): AuthenticationManager =
        AuthenticationManager(
            context = context,
            lifecycleOwner = lifecycleOwner,
            viewModel = viewModel,
            confirmCredentialActivityLauncher = {},
            clientExecutor = clientExecutor,
            clientAuthenticationCallback = clientAuthenticationCallback,
        )

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

    private fun TestActivity.showPrompt() {
        showTestPrompt(promptId, viewModel.cancellationSignalProvider.biometricCancellationSignal)
    }
}
