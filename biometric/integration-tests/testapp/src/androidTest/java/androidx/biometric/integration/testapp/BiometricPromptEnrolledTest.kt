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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BiometricPromptEnrolledTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule(BiometricTestActivity::class.java)

    private lateinit var context: Context
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        assumeTrue(TestUtils.hasEnrolledBiometric(context))
        assumeFalse(TestUtils.isDeviceLocked(context))
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @After
    fun tearDown() {
        if (::device.isInitialized) {
            TestUtils.changeOrientation(device, landscape = false)
            device.pressHome()
        }
    }

    @Test
    fun testBiometricOnlyAuth_SendsError_WhenBackPressed() {
        onView(withId(R.id.button_authenticate)).perform(click())
        device.pressBack()
        onView(withId(R.id.text_view_log)).check(
            matches(withText(containsString("onAuthenticationError")))
        )
    }

    @Test
    fun testBiometricOnlyAuth_SendsError_WhenBackPressedAfterRotation() {
        onView(withId(R.id.button_authenticate)).perform(click())
        TestUtils.changeOrientation(device, landscape = true)
        device.pressBack()
        onView(withId(R.id.text_view_log)).check(
            matches(withText(containsString("onAuthenticationError")))
        )
    }

    @Test
    fun testBiometricOnlyAuth_SendsError_WhenBackPressedAfterRepeatedRotation() {
        onView(withId(R.id.button_authenticate)).perform(click())
        for (i in 1..3) {
            TestUtils.changeOrientation(device, landscape = true)
            TestUtils.changeOrientation(device, landscape = false)
        }
        device.pressBack()
        onView(withId(R.id.text_view_log)).check(
            matches(withText(containsString("onAuthenticationError")))
        )
    }

    @Test
    fun testBiometricOrCredentialAuth_SendsError_WhenBackPressed() {
        onView(withId(R.id.checkbox_allow_device_credential)).perform(click())
        onView(withId(R.id.button_authenticate)).perform(click())
        device.pressBack()
        onView(withId(R.id.text_view_log)).check(
            matches(withText(containsString("onAuthenticationError")))
        )
    }

    @Test
    fun testBiometricOrCredentialAuth_SendsError_WhenBackPressedAfterRotation() {
        onView(withId(R.id.checkbox_allow_device_credential)).perform(click())
        onView(withId(R.id.button_authenticate)).perform(click())
        TestUtils.changeOrientation(device, landscape = true)
        device.pressBack()
        onView(withId(R.id.text_view_log)).check(
            matches(withText(containsString("onAuthenticationError")))
        )
    }

    @Test
    fun testBiometricOrCredentialAuth_SendsError_WhenBackPressedAfterRepeatedRotation() {
        onView(withId(R.id.checkbox_allow_device_credential)).perform(click())
        onView(withId(R.id.button_authenticate)).perform(click())
        for (i in 1..3) {
            TestUtils.changeOrientation(device, landscape = true)
            TestUtils.changeOrientation(device, landscape = false)
        }
        device.pressBack()
        onView(withId(R.id.text_view_log)).check(
            matches(withText(containsString("onAuthenticationError")))
        )
    }

    @Test
    fun testCancelOnConfigurationChange_SendsError_WhenDeviceRotates() {
        onView(withId(R.id.checkbox_cancel_config_change)).perform(click())
        onView(withId(R.id.button_authenticate)).perform(click())
        TestUtils.changeOrientation(device, landscape = true)
        onView(withId(R.id.text_view_log)).check(
            matches(withText(containsString("onAuthenticationError")))
        )
    }
}