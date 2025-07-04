/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.biometric.integration.testapp

import android.content.pm.ActivityInfo
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import leakcanary.DetectLeaksAfterTestSuccess
import org.hamcrest.Matchers.containsString
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BiometricPromptEnrolledTest {
    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    // TODO(b/391721281): Find a better alternative to [device.pressBack()]
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(hasEnrolledBiometric(context))
        assumeFalse(isDeviceLocked(context))
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun testBiometricOnlyAuth_SendsError_WhenBackPressed() {
        ActivityScenario.launch(BiometricPromptTestActivity::class.java).use { _ ->
            sendsError_WhenBackPressed()
        }
    }

    @Test
    fun testBiometricOrCredentialAuth_SendsError_WhenBackPressed() {
        ActivityScenario.launch(BiometricPromptTestActivity::class.java).use { _ ->
            onView(withId(R.id.allow_device_credential_checkbox)).perform(click())
            sendsError_WhenBackPressed()
        }
    }

    private fun sendsError_WhenBackPressed() {
        onView(withId(R.id.authenticate_button)).perform(click())
        device.pressBack()
        onView(withId(R.id.log_text_view))
            .check(matches(withText(containsString("onAuthenticationError"))))
    }

    @Test
    fun testBiometricOnlyAuth_SendsError_WhenBackPressedAfterRotation() {
        ActivityScenario.launch(BiometricPromptTestActivity::class.java).use { scenario ->
            sendsError_WhenBackPressedAfterRotation(scenario)
        }
    }

    @Test
    fun testBiometricOrCredentialAuth_SendsError_WhenBackPressedAfterRotation() {
        ActivityScenario.launch(BiometricPromptTestActivity::class.java).use { scenario ->
            onView(withId(R.id.allow_device_credential_checkbox)).perform(click())
            sendsError_WhenBackPressedAfterRotation(scenario)
        }
    }

    private fun sendsError_WhenBackPressedAfterRotation(scenario: ActivityScenario<*>) {
        onView(withId(R.id.authenticate_button)).perform(click())
        scenario.rotateDevice(toLandscape = true)
        device.pressBack()
        onView(withId(R.id.log_text_view))
            .check(matches(withText(containsString("onAuthenticationError"))))
    }

    @Test
    fun testBiometricOnlyAuth_SendsError_WhenBackPressedAfterRepeatedRotation() {
        ActivityScenario.launch(BiometricPromptTestActivity::class.java).use { scenario ->
            sendsError_WhenBackPressedAfterRepeatedRotation(scenario)
        }
    }

    @Test
    fun testBiometricOrCredentialAuth_SendsError_WhenBackPressedAfterRepeatedRotation() {
        ActivityScenario.launch(BiometricPromptTestActivity::class.java).use { scenario ->
            onView(withId(R.id.allow_device_credential_checkbox)).perform(click())
            sendsError_WhenBackPressedAfterRepeatedRotation(scenario)
        }
    }

    private fun sendsError_WhenBackPressedAfterRepeatedRotation(scenario: ActivityScenario<*>) {
        onView(withId(R.id.authenticate_button)).perform(click())
        for (i in 1..3) {
            scenario.rotateDevice(toLandscape = true)
            scenario.rotateDevice(toLandscape = false)
        }
        device.pressBack()
        onView(withId(R.id.log_text_view))
            .check(matches(withText(containsString("onAuthenticationError"))))
    }

    @Test
    fun testBiometricOnlyAuth_SendsError_WhenCanceledOnConfigurationChange() {
        ActivityScenario.launch(BiometricPromptTestActivity::class.java).use { scenario ->
            sendsError_WhenCanceledOnConfigurationChange(scenario)
        }
    }

    @Test
    fun testBiometricOrCredentialAuth_SendsError_WhenCanceledOnConfigurationChange() {
        // Prompt isn't canceled on configuration change for some devices on API 29 (b/202975762).
        assumeFalse(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q)

        ActivityScenario.launch(BiometricPromptTestActivity::class.java).use { scenario ->
            onView(withId(R.id.allow_device_credential_checkbox)).perform(click())
            sendsError_WhenCanceledOnConfigurationChange(scenario)
        }
    }

    private fun sendsError_WhenCanceledOnConfigurationChange(scenario: ActivityScenario<*>) {
        onView(withId(R.id.cancel_config_change_checkbox)).perform(click())
        onView(withId(R.id.authenticate_button)).perform(click())
        scenario.rotateDevice(toLandscape = true)
        onView(withId(R.id.log_text_view))
            .check(matches(withText(containsString("onAuthenticationError"))))
    }

    @Test
    fun testBiometricOnlyAuth_SendsError_WhenActivityBackgrounded() {
        ActivityScenario.launch(BiometricPromptTestActivity::class.java).use { scenario ->
            sendsError_WhenActivityBackgrounded(scenario)
        }
    }

    @Test
    fun testBiometricOrCredentialAuth_SendsError_WhenActivityBackgrounded() {
        // Prompt is not dismissed when backgrounded for Pixel devices on API 29 (b/162022588).
        assumeFalse(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q)

        ActivityScenario.launch(BiometricPromptTestActivity::class.java).use { scenario ->
            onView(withId(R.id.allow_device_credential_checkbox)).perform(click())
            sendsError_WhenActivityBackgrounded(scenario)
        }
    }

    private fun sendsError_WhenActivityBackgrounded(scenario: ActivityScenario<*>) {
        onView(withId(R.id.authenticate_button)).perform(click())
        // This actually stops the activity. Use this to bring the activity to background.
        scenario.moveToState(Lifecycle.State.CREATED)
        // Bring activity to foreground to check the text
        scenario.moveToState(Lifecycle.State.RESUMED)
        onView(withId(R.id.log_text_view))
            .check(matches(withText(containsString("onAuthenticationError"))))
    }

    // TODO(b/391721281): Use [ScreenOrientationRule] instead. Somehow
    // DeviceControllerOperationException happens with [onDevice().setScreenOrientation()]
    private fun ActivityScenario<*>.rotateDevice(toLandscape: Boolean) {
        val orientation =
            if (toLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

        onActivity { activity -> activity.requestedOrientation = orientation }

        // Wait for the rotation to complete
        device.waitForIdle()
    }
}
