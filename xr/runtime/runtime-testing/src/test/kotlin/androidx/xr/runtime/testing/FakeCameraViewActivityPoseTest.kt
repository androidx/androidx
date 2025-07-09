/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.runtime.testing

import androidx.xr.runtime.internal.CameraViewActivityPose.CameraType
import androidx.xr.runtime.internal.PixelDimensions
import org.junit.Test

class FakeCameraViewActivityPoseTest {
    private lateinit var underTest: FakeCameraViewActivityPose

    @Test
    fun getInitialValues_returnsDefaultValues() {
        // Arrange
        underTest = FakeCameraViewActivityPose()

        // Assert
        check(underTest.cameraType == CameraType.CAMERA_TYPE_UNKNOWN)
        // Default fov is (0, 0, 0, 0)
        check(underTest.fov.angleLeft == 0f)
        check(underTest.fov.angleRight == 0f)
        check(underTest.fov.angleUp == 0f)
        check(underTest.fov.angleDown == 0f)
        // Default resolution is (640, 480)
        check(underTest.displayResolutionInPixels == PixelDimensions(640, 480))
    }
}
