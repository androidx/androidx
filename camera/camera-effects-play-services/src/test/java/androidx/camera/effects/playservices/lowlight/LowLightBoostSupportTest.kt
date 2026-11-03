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
package androidx.camera.effects.playservices.lowlight

import android.util.Size
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class LowLightBoostSupportTest {

    @Before
    fun setUp() {
        PlayServicesLowLightBoostAvailability.reset()
    }

    @After
    fun tearDown() {
        PlayServicesLowLightBoostAvailability.reset()
    }

    @Test
    fun supportedResolutionAndUnknownSupport_haveNoReason() {
        assertThat(LowLightBoostSupport.unsupportedReasonOrNull(Size(1920, 1080), "0")).isNull()
    }

    @Test
    fun unsupportedResolution_hasIllegalArgumentReason() {
        // Act.
        val reason = LowLightBoostSupport.unsupportedReasonOrNull(Size(3840, 2160), "0")

        // Assert.
        assertThat(requireNotNull(reason).error).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun cachedUnsupportedDevice_hasDeviceReason() {
        // Arrange.
        PlayServicesLowLightBoostAvailability.setDeviceSupported(false)

        // Act.
        val reason = LowLightBoostSupport.unsupportedReasonOrNull(Size(1920, 1080), "0")

        // Assert.
        assertThat(requireNotNull(reason).error.message)
            .isEqualTo("Low Light Boost is not supported on this device.")
    }

    @Test
    fun cachedUnsupportedCamera_hasCameraReason() {
        // Arrange.
        PlayServicesLowLightBoostAvailability.setCameraSupported("0", false)

        // Act.
        val reason = LowLightBoostSupport.cachedUnsupportedReasonOrNull("0")

        // Assert.
        assertThat(requireNotNull(reason).error.message)
            .isEqualTo("Low Light Boost is not supported on camera 0.")
    }
}
