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

package androidx.camera.core.impl

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [CameraFilters].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
class CameraFiltersTest {
    private val mCameras: LinkedHashSet<Camera> = linkedSetOf()

    @Before
    fun setUp() {
        val fakeCamera0 = FakeCamera(
            "0",
            null,
            FakeCameraInfoInternal("0", 0, CameraSelector.LENS_FACING_BACK)
        )
        mCameras.add(fakeCamera0)

        val fakeCamera1 = FakeCamera(
            "1",
            null,
            FakeCameraInfoInternal("1", 180, CameraSelector.LENS_FACING_FRONT)
        )
        mCameras.add(fakeCamera1)
    }

    @Test
    fun filterAny() {
        val resultCameras = CameraFilters.ANY.filter(mCameras)
        assertThat(resultCameras).isEqualTo(mCameras)
    }

    @Test
    fun filterNone() {
        val resultCameras = CameraFilters.NONE.filter(mCameras)
        assertThat(resultCameras).isEmpty()
    }
}