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
import com.google.android.gms.cameralowlight.LowLightBoostStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
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

    @Test
    fun toPublicError_preservesStandardExceptionInstances() {
        val unsupported = UnsupportedOperationException("Unsupported")
        val illegalArg = IllegalArgumentException("Bad arg")
        val illegalState = IllegalStateException("Bad state")

        assertThat(LowLightBoostSupport.toPublicError(unsupported)).isSameInstanceAs(unsupported)
        assertThat(LowLightBoostSupport.toPublicError(illegalArg)).isSameInstanceAs(illegalArg)
        assertThat(LowLightBoostSupport.toPublicError(illegalState)).isSameInstanceAs(illegalState)
    }

    @Test
    fun toPublicError_normalizesApiExceptionsAndWrapsOtherErrorsWithCause() {
        val cameraUnsupported = ApiException(Status(LowLightBoostStatusCodes.CAMERA_NOT_SUPPORTED))
        val missingChars =
            ApiException(Status(LowLightBoostStatusCodes.MISSING_CAMERA_CHARACTERISTICS))
        val badDataspace = ApiException(Status(LowLightBoostStatusCodes.UNSUPPORTED_DATASPACE))
        val internalError = ApiException(Status(CommonStatusCodes.INTERNAL_ERROR))
        val apiNotConnected = ApiException(Status(CommonStatusCodes.API_NOT_CONNECTED))
        val securityException = SecurityException("Denied")

        val normalizedCamera = LowLightBoostSupport.toPublicError(cameraUnsupported)
        assertThat(normalizedCamera).isInstanceOf(UnsupportedOperationException::class.java)
        assertThat(normalizedCamera.cause).isSameInstanceAs(cameraUnsupported)

        val normalizedChars = LowLightBoostSupport.toPublicError(missingChars)
        assertThat(normalizedChars).isInstanceOf(UnsupportedOperationException::class.java)
        assertThat(normalizedChars.cause).isSameInstanceAs(missingChars)

        val normalizedDataspace = LowLightBoostSupport.toPublicError(badDataspace)
        assertThat(normalizedDataspace).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(normalizedDataspace.cause).isSameInstanceAs(badDataspace)

        val normalizedInternal = LowLightBoostSupport.toPublicError(internalError)
        assertThat(normalizedInternal).isInstanceOf(IllegalStateException::class.java)
        assertThat(normalizedInternal.cause).isSameInstanceAs(internalError)

        val normalizedTransientDisconnect = LowLightBoostSupport.toPublicError(apiNotConnected)
        assertThat(normalizedTransientDisconnect).isInstanceOf(IllegalStateException::class.java)
        assertThat(normalizedTransientDisconnect.cause).isSameInstanceAs(apiNotConnected)

        PlayServicesLowLightBoostAvailability.setDeviceSupported(false)
        val normalizedUnsupportedDisconnect = LowLightBoostSupport.toPublicError(apiNotConnected)
        assertThat(normalizedUnsupportedDisconnect)
            .isInstanceOf(UnsupportedOperationException::class.java)
        assertThat(normalizedUnsupportedDisconnect.cause).isSameInstanceAs(apiNotConnected)

        val normalizedSecurity = LowLightBoostSupport.toPublicError(securityException)
        assertThat(normalizedSecurity).isInstanceOf(IllegalStateException::class.java)
        assertThat(normalizedSecurity.cause).isSameInstanceAs(securityException)
    }
}
