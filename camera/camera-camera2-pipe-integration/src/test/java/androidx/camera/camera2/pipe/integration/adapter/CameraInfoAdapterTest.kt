/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.integration.testing.FakeCameraInfoAdapterCreator.createCameraInfoAdapter
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.impl.ImageFormatConstants
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraInfoAdapterTest {
    private val cameraInfoAdapter = createCameraInfoAdapter()

    @Test
    fun getSupportedResolutions() {
        // Act.
        val resolutions: List<Size> = cameraInfoAdapter.getSupportedResolutions(
            ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
        )

        // Assert.
        assertThat(resolutions).containsExactly(
            Size(1920, 1080),
            Size(1280, 720),
            Size(640, 480)
        )
    }

    @Test
    fun canReturnIsFocusMeteringSupported() {
        val factory = SurfaceOrientedMeteringPointFactory(1.0f, 1.0f)
        val action = FocusMeteringAction.Builder(
            factory.createPoint(0.5f, 0.5f)
        ).build()

        assertWithMessage("isFocusMeteringSupported() method did not return successfully")
            .that(cameraInfoAdapter.isFocusMeteringSupported(action))
            .isAnyOf(true, false)
    }
}
