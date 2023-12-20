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

import android.os.Build
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [CameraFilters].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraFiltersTest {
    private val mCameraInfos: ArrayList<CameraInfo> = arrayListOf()

    @Before
    fun setUp() {
        val fakeCameraInfo0 = FakeCameraInfoInternal("0", 0, CameraSelector.LENS_FACING_BACK)
        mCameraInfos.add(fakeCameraInfo0)

        val fakeCameraInfo1 = FakeCameraInfoInternal("1", 180, CameraSelector.LENS_FACING_FRONT)
        mCameraInfos.add(fakeCameraInfo1)
    }

    @Test
    fun filterAny() {
        val resultCameras = CameraFilters.ANY.filter(mCameraInfos)
        assertThat(resultCameras).isEqualTo(mCameraInfos)
    }

    @Test
    fun filterNone() {
        val resultCameras = CameraFilters.NONE.filter(mCameraInfos)
        assertThat(resultCameras).isEmpty()
    }
}
