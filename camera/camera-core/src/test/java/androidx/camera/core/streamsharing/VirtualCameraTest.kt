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

package androidx.camera.core.streamsharing

import android.os.Build
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.UseCase
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [VirtualCamera]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class VirtualCameraTest {

    private val cameraInfo = FakeCameraInfoInternal(90, CameraSelector.LENS_FACING_BACK)

    private val parentCamera = FakeCamera(null, cameraInfo)

    private val useCaseStateCallback =
        object : UseCase.StateChangeCallback {

            override fun onUseCaseActive(useCase: UseCase) {}

            override fun onUseCaseInactive(useCase: UseCase) {}

            override fun onUseCaseUpdated(useCase: UseCase) {}

            override fun onUseCaseReset(useCase: UseCase) {}
        }

    private val streamSharingControl =
        StreamSharing.Control { _, _ -> Futures.immediateFuture(null) }

    private val virtualCamera =
        VirtualCamera(parentCamera, useCaseStateCallback, streamSharingControl)

    @Test
    fun getCameraId_returnsVirtualCameraId() {
        assertThat(virtualCamera.cameraInfoInternal.cameraId)
            .startsWith("virtual-" + parentCamera.cameraInfoInternal.cameraId)
    }

    @Test
    fun getCameraState_returnsParentState() {
        assertThat(virtualCamera.cameraState).isEqualTo(parentCamera.cameraState)
    }

    @Test
    fun setRotationDegrees_offsetsParentRotationDegrees() {
        assertThat(parentCamera.cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_0))
            .isEqualTo(90)
        virtualCamera.setRotationDegrees(180)
        assertThat(virtualCamera.cameraInfoInternal.getSensorRotationDegrees()).isEqualTo(270)
        assertThat(virtualCamera.cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_0))
            .isEqualTo(270)
    }
}
