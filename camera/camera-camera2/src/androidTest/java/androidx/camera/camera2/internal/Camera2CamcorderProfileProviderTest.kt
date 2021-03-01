/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.internal

import android.media.CamcorderProfile
import androidx.camera.core.CameraSelector
import androidx.camera.testing.CameraUtil
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SmallTest
public class Camera2CamcorderProfileProviderTest(private val quality: Int) {
    public companion object {
        @JvmStatic
        @Parameterized.Parameters
        public fun data(): Array<Array<Int>> = arrayOf(
            arrayOf(CamcorderProfile.QUALITY_LOW),
            arrayOf(CamcorderProfile.QUALITY_HIGH),
            arrayOf(CamcorderProfile.QUALITY_QCIF),
            arrayOf(CamcorderProfile.QUALITY_CIF),
            arrayOf(CamcorderProfile.QUALITY_480P),
            arrayOf(CamcorderProfile.QUALITY_720P),
            arrayOf(CamcorderProfile.QUALITY_1080P),
            arrayOf(CamcorderProfile.QUALITY_QVGA),
            arrayOf(CamcorderProfile.QUALITY_2160P),
            arrayOf(CamcorderProfile.QUALITY_VGA),
            arrayOf(CamcorderProfile.QUALITY_4KDCI),
            arrayOf(CamcorderProfile.QUALITY_QHD),
            arrayOf(CamcorderProfile.QUALITY_2K)
        )
    }

    private lateinit var camcorderProfileProvider: Camera2CamcorderProfileProvider
    private var intCameraId = -1

    @Before
    public fun setup() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        val cameraId = CameraUtil.getCameraIdWithLensFacing(CameraSelector.LENS_FACING_BACK)!!
        intCameraId = cameraId.toInt()

        camcorderProfileProvider = Camera2CamcorderProfileProvider(cameraId)
    }

    @Test
    public fun hasProfile_returnSameResult() {
        assertThat(camcorderProfileProvider.hasProfile(quality))
            .isEqualTo(CamcorderProfile.hasProfile(intCameraId, quality))
    }

    @Test
    public fun notHasProfile_getReturnNull() {
        assumeTrue(!CamcorderProfile.hasProfile(intCameraId, quality))

        assertThat(camcorderProfileProvider.get(quality)).isNull()
    }

    @Test
    public fun hasProfile_getReturnSameQualityProfile() {
        assumeTrue(CamcorderProfile.hasProfile(intCameraId, quality))

        val profileProxy = camcorderProfileProvider.get(quality)!!
        val profile = CamcorderProfile.get(intCameraId, quality)
        assertThat(profileProxy.quality).isEqualTo(profile.quality)
    }
}
