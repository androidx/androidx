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

package androidx.camera.camera2.pipe.testing

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class RobolectricCamerasTest {
    private val context = ApplicationProvider.getApplicationContext() as Context
    private val mainLooper = shadowOf(Looper.getMainLooper())

    @Test
    fun fakeCamerasCanBeOpened() {
        val fakeCameraId = RobolectricCameras.create(
            mapOf(CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK)
        )
        val fakeCamera = RobolectricCameras.open(fakeCameraId)

        assertThat(fakeCamera).isNotNull()
        assertThat(fakeCamera.cameraId).isEqualTo(fakeCameraId)
        assertThat(fakeCamera.cameraDevice).isNotNull()
        assertThat(fakeCamera.characteristics).isNotNull()
        assertThat(fakeCamera.characteristics[CameraCharacteristics.LENS_FACING]).isNotNull()
        assertThat(fakeCamera.metadata).isNotNull()
        assertThat(fakeCamera.metadata[CameraCharacteristics.LENS_FACING]).isNotNull()
    }

    @After
    fun teardown() {
        mainLooper.idle()
        RobolectricCameras.clear()
    }
}