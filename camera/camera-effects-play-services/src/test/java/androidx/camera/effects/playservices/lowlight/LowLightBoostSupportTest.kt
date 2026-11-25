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
import java.util.concurrent.CancellationException
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
    fun unsupportedResolution_hasUnsupportedStreamReason() {
        // Act.
        val reason = LowLightBoostSupport.unsupportedReasonOrNull(Size(3840, 2160), "0")

        // Assert.
        val error = requireNotNull(reason).error
        assertThat(error.errorCode).isEqualTo(LowLightBoostException.ERROR_UNSUPPORTED_STREAM)
    }

    @Test
    fun cachedUnsupportedDevice_hasDeviceReason() {
        // Arrange.
        PlayServicesLowLightBoostAvailability.setDeviceSupported(false)

        // Act.
        val reason = LowLightBoostSupport.unsupportedReasonOrNull(Size(1920, 1080), "0")

        // Assert.
        val error = requireNotNull(reason).error
        assertThat(error.errorCode).isEqualTo(LowLightBoostException.ERROR_NOT_SUPPORTED)
        assertThat(error.message).isEqualTo("Low Light Boost is not supported on this device.")
    }

    @Test
    fun cachedUnsupportedCamera_hasCameraReason() {
        // Arrange.
        PlayServicesLowLightBoostAvailability.setCameraSupported("0", false)

        // Act.
        val reason = LowLightBoostSupport.cachedUnsupportedReasonOrNull("0")

        // Assert.
        val error = requireNotNull(reason).error
        assertThat(error.errorCode).isEqualTo(LowLightBoostException.ERROR_NOT_SUPPORTED)
        assertThat(error.message).isEqualTo("Low Light Boost is not supported on camera 0.")
    }

    @Test
    fun lowLightBoostException_constructorsAndIdentityEquality() {
        val cause = RuntimeException("Root cause")
        val twoArg = LowLightBoostException(LowLightBoostException.ERROR_SESSION_FAILED, "Failed")
        val threeArg =
            LowLightBoostException(LowLightBoostException.ERROR_SESSION_FAILED, "Failed", cause)

        assertThat(twoArg.errorCode).isEqualTo(LowLightBoostException.ERROR_SESSION_FAILED)
        assertThat(twoArg.message).isEqualTo("Failed")
        assertThat(twoArg.cause).isNull()
        assertThat(threeArg.errorCode).isEqualTo(LowLightBoostException.ERROR_SESSION_FAILED)
        assertThat(threeArg.message).isEqualTo("Failed")
        assertThat(threeArg.cause).isSameInstanceAs(cause)
        assertThat(twoArg).isEqualTo(twoArg)
        assertThat(twoArg).isNotEqualTo(threeArg)
    }

    @Test
    fun toPublicError_preservesLowLightBoostExceptionInstances() {
        val existing =
            LowLightBoostException(LowLightBoostException.ERROR_NOT_SUPPORTED, "Unsupported")

        assertThat(LowLightBoostSupport.toPublicError(existing)).isSameInstanceAs(existing)
    }

    @Test
    fun toPublicError_wrapsInternalExceptionsAndPreservesCause() {
        val unsupported = UnsupportedOperationException("Internal unsupported")
        val illegalArg = IllegalArgumentException("Internal arg")
        val illegalState = IllegalStateException("Internal state")
        val cancellation = CancellationException("Cancelled")
        val securityException = SecurityException("Denied")

        listOf(unsupported, illegalArg, illegalState, cancellation, securityException).forEach { raw
            ->
            val wrapped = LowLightBoostSupport.toPublicError(raw)
            assertThat(wrapped.errorCode).isEqualTo(LowLightBoostException.ERROR_UNKNOWN)
            assertThat(wrapped.cause).isSameInstanceAs(raw)
        }
    }

    @Test
    fun toPublicError_normalizesApiExceptionsAndPreservesCause() {
        val cameraUnsupported = ApiException(Status(LowLightBoostStatusCodes.CAMERA_NOT_SUPPORTED))
        val missingChars =
            ApiException(Status(LowLightBoostStatusCodes.MISSING_CAMERA_CHARACTERISTICS))
        val badDataspace = ApiException(Status(LowLightBoostStatusCodes.UNSUPPORTED_DATASPACE))
        val internalError = ApiException(Status(CommonStatusCodes.INTERNAL_ERROR))
        val apiNotConnected = ApiException(Status(CommonStatusCodes.API_NOT_CONNECTED))

        val normalizedCamera = LowLightBoostSupport.toPublicError(cameraUnsupported)
        assertThat(normalizedCamera.errorCode).isEqualTo(LowLightBoostException.ERROR_NOT_SUPPORTED)
        assertThat(normalizedCamera.cause).isSameInstanceAs(cameraUnsupported)

        val normalizedChars = LowLightBoostSupport.toPublicError(missingChars)
        assertThat(normalizedChars.errorCode).isEqualTo(LowLightBoostException.ERROR_NOT_SUPPORTED)
        assertThat(normalizedChars.cause).isSameInstanceAs(missingChars)

        val normalizedDataspace = LowLightBoostSupport.toPublicError(badDataspace)
        assertThat(normalizedDataspace.errorCode)
            .isEqualTo(LowLightBoostException.ERROR_UNSUPPORTED_STREAM)
        assertThat(normalizedDataspace.cause).isSameInstanceAs(badDataspace)

        val normalizedInternal = LowLightBoostSupport.toPublicError(internalError)
        assertThat(normalizedInternal.errorCode)
            .isEqualTo(LowLightBoostException.ERROR_SESSION_FAILED)
        assertThat(normalizedInternal.cause).isSameInstanceAs(internalError)

        val normalizedTransientDisconnect = LowLightBoostSupport.toPublicError(apiNotConnected)
        assertThat(normalizedTransientDisconnect.errorCode)
            .isEqualTo(LowLightBoostException.ERROR_SESSION_FAILED)
        assertThat(normalizedTransientDisconnect.cause).isSameInstanceAs(apiNotConnected)

        PlayServicesLowLightBoostAvailability.setDeviceSupported(false)
        val normalizedUnsupportedDisconnect = LowLightBoostSupport.toPublicError(apiNotConnected)
        assertThat(normalizedUnsupportedDisconnect.errorCode)
            .isEqualTo(LowLightBoostException.ERROR_NOT_SUPPORTED)
        assertThat(normalizedUnsupportedDisconnect.cause).isSameInstanceAs(apiNotConnected)
    }
}
