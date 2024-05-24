/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.extensions.internal

import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraManager

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 28, instrumentedPackages = arrayOf("androidx.camera.extensions.internal"))
class ExtensionsUtilsTest {
    companion object {
        val ACTIVE_ARRAY_0 = Rect(0, 0, 1920, 1080)
        val ACTIVE_ARRAY_2 = Rect(0, 0, 640, 480)
        val ACTIVE_ARRAY_3 = Rect(0, 0, 320, 240)
    }

    @Test
    fun canReturnCameraCharacteriticsMap() {
        registerCameraCharacteristics("0", ACTIVE_ARRAY_0, physicalCameraId = setOf("0", "2", "3"))
        registerCameraCharacteristics("2", ACTIVE_ARRAY_2)
        registerCameraCharacteristics("3", ACTIVE_ARRAY_3)

        val cameraInfo = FakeCameraInfoInternal("0", ApplicationProvider.getApplicationContext())

        val characteristicsMap = ExtensionsUtils.getCameraCharacteristicsMap(cameraInfo)
        assertThat(characteristicsMap.size).isEqualTo(3)
        assertThat(
                characteristicsMap
                    .get("0")!!
                    .get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            )
            .isSameInstanceAs(ACTIVE_ARRAY_0)
        assertThat(
                characteristicsMap
                    .get("2")!!
                    .get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            )
            .isSameInstanceAs(ACTIVE_ARRAY_2)
        assertThat(
                characteristicsMap
                    .get("3")!!
                    .get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            )
            .isSameInstanceAs(ACTIVE_ARRAY_3)
    }

    private fun registerCameraCharacteristics(
        cameraId: String,
        activeArray: Rect,
        physicalCameraId: Set<String>? = null
    ) {
        val characteristics0 = Mockito.mock(CameraCharacteristics::class.java)
        physicalCameraId?.let {
            Mockito.`when`(characteristics0.physicalCameraIds).thenReturn(physicalCameraId)
        }
        Mockito.`when`(characteristics0.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE))
            .thenReturn(activeArray)

        // Add the camera to the camera service
        val shadowCameraManager =
            Shadow.extract<Any>(
                ApplicationProvider.getApplicationContext<Context>()
                    .getSystemService(Context.CAMERA_SERVICE)
            ) as ShadowCameraManager

        shadowCameraManager.addCamera(cameraId, characteristics0)
    }
}
