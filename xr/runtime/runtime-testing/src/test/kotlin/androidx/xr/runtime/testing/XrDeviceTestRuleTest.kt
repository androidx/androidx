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

package androidx.xr.runtime.testing

import androidx.activity.ComponentActivity
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.DisplayBlendMode
import androidx.xr.runtime.XrDevice
import androidx.xr.runtime.testing.internal.FakeSpatialApiVersionProvider
import androidx.xr.runtime.testing.internal.FakeXrDeviceCapabilityProviderFactory
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class XrDeviceTestRuleTest {

    @Rule @JvmField val underTest: XrDeviceTestRule = XrDeviceTestRule()

    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        val activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
    }

    @Test
    fun before_registersSelfWithProviders() {
        assertThat(FakeXrDeviceCapabilityProviderFactory.xrDeviceTestRule).isEqualTo(underTest)
        assertThat(FakeSpatialApiVersionProvider.xrDeviceTestRule).isEqualTo(underTest)
    }

    @Test
    fun preferredDisplayBlendMode_returnedByDevice() {
        val device = XrDevice.getCurrentDevice(activity)
        underTest.preferredDisplayBlendMode = DisplayBlendMode.NO_DISPLAY

        assertThat(device.getPreferredDisplayBlendMode()).isEqualTo(DisplayBlendMode.NO_DISPLAY)

        underTest.preferredDisplayBlendMode = DisplayBlendMode.ALPHA_BLEND

        assertThat(device.getPreferredDisplayBlendMode()).isEqualTo(DisplayBlendMode.ALPHA_BLEND)
    }

    @Test
    fun isProjectedServiceAvailable_enabledByDefault() {
        assertThat(XrDevice.isProjectedServiceAvailable(activity)).isTrue()
    }

    @Test
    fun isProjectedServiceAvailable_controlsReturnValue() {
        underTest.isProjectedServiceAvailable = false

        assertThat(XrDevice.isProjectedServiceAvailable(activity)).isFalse()

        underTest.isProjectedServiceAvailable = true

        assertThat(XrDevice.isProjectedServiceAvailable(activity)).isTrue()
    }
}
