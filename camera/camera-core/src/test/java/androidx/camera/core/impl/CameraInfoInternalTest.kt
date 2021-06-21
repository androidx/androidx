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
package androidx.camera.core.impl

import android.os.Build
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val CAMERA_ID = "2"

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class CameraInfoInternalTest {

    @Test
    fun selector_findsMatchingCamera() {
        val cameraInfo = FakeCameraInfoInternal(CAMERA_ID)
        val cameras = createCamerasWithIds(arrayOf(1, CAMERA_ID.toInt(), 3, 4))
        val filteredCameras = cameraInfo.cameraSelector.filter(LinkedHashSet(cameras))

        assertThat(filteredCameras).hasSize(1)
        assertThat(filteredCameras.first().cameraInfoInternal.cameraId).isEqualTo(CAMERA_ID)
    }

    @Test(expected = IllegalStateException::class)
    fun selector_doesNotFindMatchingCamera() {
        val cameraInfo = FakeCameraInfoInternal(CAMERA_ID)
        val cameras = createCamerasWithIds(arrayOf(1, 3, 4))
        cameraInfo.cameraSelector.filter(LinkedHashSet(cameras))
    }

    private fun createCamerasWithIds(ids: Array<Int>): List<CameraInternal> {
        return ids.map { FakeCamera(it.toString()) }
    }
}