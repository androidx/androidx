/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.hardware.camera2.CameraCharacteristics.LENS_FACING
import android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
import android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT
import android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
import android.hardware.camera2.CameraMetadata.LENS_FACING_EXTERNAL
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.CamcorderProfile.QUALITY_1080P
import android.media.CamcorderProfile.QUALITY_480P
import android.media.CamcorderProfile.QUALITY_720P
import android.media.CamcorderProfile.QUALITY_HIGH
import android.media.CamcorderProfile.QUALITY_LOW
import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.testing.FakeCameraDevices
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.Quirks
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_480P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_720P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_480P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_720P
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val CAMERA_ID_0 = "0"
private const val CAMERA_ID_1 = "1"
private const val CAMERA_ID_EXTERNAL_0 = "100"
private const val CAMERA_ID_EXTERNAL_1 = "101"

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class EncoderProfilesProviderFallbackTest {

    private lateinit var cameraDevices: CameraDevices
    private val quirks = Quirks(emptyList())

    private val profilesProvider0: EncoderProfilesProvider =
        FakeEncoderProfilesProvider.Builder()
            .add(QUALITY_HIGH, PROFILES_720P)
            .add(QUALITY_720P, PROFILES_720P)
            .add(QUALITY_480P, PROFILES_480P)
            .add(QUALITY_LOW, PROFILES_480P)
            .build()

    private val profilesProvider1: EncoderProfilesProvider =
        FakeEncoderProfilesProvider.Builder()
            .add(QUALITY_HIGH, PROFILES_1080P)
            .add(QUALITY_1080P, PROFILES_1080P)
            .add(QUALITY_720P, PROFILES_720P)
            .add(QUALITY_480P, PROFILES_480P)
            .add(QUALITY_LOW, PROFILES_480P)
            .build()

    private val profilesProviderExternal0: EncoderProfilesProvider =
        FakeEncoderProfilesProvider.Builder()
            .add(QUALITY_HIGH, PROFILES_480P)
            .add(QUALITY_480P, PROFILES_480P)
            .add(QUALITY_LOW, PROFILES_480P)
            .build()

    private val profilesProviderExternal1: EncoderProfilesProvider =
        FakeEncoderProfilesProvider.Builder().build()

    private val providerFactory: (cameraId: String, quirks: Quirks) -> EncoderProfilesProvider =
        { cameraId, _ ->
            when (cameraId) {
                CAMERA_ID_0 -> profilesProvider0
                CAMERA_ID_1 -> profilesProvider1
                CAMERA_ID_EXTERNAL_0 -> profilesProviderExternal0
                CAMERA_ID_EXTERNAL_1 -> profilesProviderExternal1
                else -> throw IllegalArgumentException("Unknown camera id: $cameraId")
            }
        }

    @Before
    fun setup() {
        initCameras()
    }

    @Test
    fun resolveProvider_isInternalCamera_noFallback() {
        // Arrange.
        val encoderProfilesProviderFallback = EncoderProfilesProviderFallback(providerFactory)

        // Act.
        val result =
            encoderProfilesProviderFallback.resolveProvider(CAMERA_ID_0, quirks, cameraDevices)

        // Assert.
        assertThat(result).isSameInstanceAs(profilesProvider0)
    }

    @Test
    fun resolveProvider_isExternalCameraWithProfile_noFallback() {
        // Arrange.
        val encoderProfilesProviderFallback = EncoderProfilesProviderFallback(providerFactory)

        // Act.
        val result =
            encoderProfilesProviderFallback.resolveProvider(
                CAMERA_ID_EXTERNAL_0,
                quirks,
                cameraDevices,
            )

        assertThat(result).isSameInstanceAs(profilesProviderExternal0)
    }

    @Test
    fun resolveProvider_isExternalCamera_hasFallback() {
        // Arrange.
        val encoderProfilesProviderFallback = EncoderProfilesProviderFallback(providerFactory)

        // Act.
        val result =
            encoderProfilesProviderFallback.resolveProvider(
                CAMERA_ID_EXTERNAL_1,
                quirks,
                cameraDevices,
            )

        // Assert.
        // The result provider should be from the provider1 and filtered by the supported sizes of
        // external camera.
        // Provider1 contains [1080P, 720P, 480P] profiles.
        // External camera supports [1080P, 720P] sizes.
        // Result provider should contain [1080P, 720P] profiles.
        assertThat(result).isNotSameInstanceAs(profilesProviderExternal1)

        assertThat(result.hasProfile(QUALITY_HIGH)).isTrue()
        assertThat(result.getAll(QUALITY_HIGH)).isSameInstanceAs(PROFILES_1080P)
        assertThat(result.hasProfile(QUALITY_LOW)).isTrue()
        assertThat(result.getAll(QUALITY_LOW)).isSameInstanceAs(PROFILES_720P)

        assertThat(result.hasProfile(QUALITY_1080P)).isTrue()
        assertThat(result.getAll(QUALITY_1080P)).isSameInstanceAs(PROFILES_1080P)
        assertThat(result.hasProfile(QUALITY_720P)).isTrue()
        assertThat(result.getAll(QUALITY_720P)).isSameInstanceAs(PROFILES_720P)
        assertThat(result.hasProfile(QUALITY_480P)).isFalse()
        assertThat(result.getAll(QUALITY_480P)).isNull()
    }

    private fun initCameras() {
        val metadata0 =
            createCameraMetadata(
                cameraId = CameraId(CAMERA_ID_0),
                lensFacing = LENS_FACING_BACK,
                supportedSizes = arrayOf(RESOLUTION_720P, RESOLUTION_480P),
            )

        val metadata1 =
            createCameraMetadata(
                cameraId = CameraId(CAMERA_ID_1),
                lensFacing = LENS_FACING_FRONT,
                supportedSizes = arrayOf(RESOLUTION_1080P, RESOLUTION_720P, RESOLUTION_480P),
            )

        val metadataExternal0 =
            createCameraMetadata(
                cameraId = CameraId(CAMERA_ID_EXTERNAL_0),
                lensFacing = LENS_FACING_EXTERNAL,
                supportedSizes = arrayOf(RESOLUTION_480P),
            )

        val metadataExternal1 =
            createCameraMetadata(
                cameraId = CameraId(CAMERA_ID_EXTERNAL_1),
                lensFacing = LENS_FACING_EXTERNAL,
                supportedSizes = arrayOf(RESOLUTION_1080P, RESOLUTION_720P),
            )

        val backendId = CameraBackendId(CAMERA_ID_0)

        cameraDevices =
            FakeCameraDevices(
                defaultCameraBackendId = backendId,
                concurrentCameraBackendIds = emptySet(),
                cameraMetadataMap =
                    mapOf(
                        backendId to
                            listOf(metadata0, metadata1, metadataExternal0, metadataExternal1)
                    ),
            )
    }

    private fun createCameraMetadata(
        cameraId: CameraId,
        lensFacing: Int,
        supportedSizes: Array<Size>,
    ): FakeCameraMetadata {
        val mockMap =
            Mockito.mock(StreamConfigurationMap::class.java).also { map ->
                `when`(map.getOutputSizes(anyInt())).thenReturn(supportedSizes)
                `when`(map.getOutputSizes(any(Class::class.java))).thenReturn(supportedSizes)
            }

        val characteristics =
            mapOf(LENS_FACING to lensFacing, SCALER_STREAM_CONFIGURATION_MAP to mockMap)
        return FakeCameraMetadata(cameraId = cameraId, characteristics = characteristics)
    }
}
