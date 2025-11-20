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

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.CameraViewScenePose
import androidx.xr.scenecore.runtime.CameraViewScenePose.CameraType
import androidx.xr.scenecore.runtime.CameraViewScenePose.Fov
import androidx.xr.scenecore.runtime.PixelDimensions

/**
 * A fake ActivityPose representing a user's camera. This can be used to determine the location and
 * field of view of the camera.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class FakeCameraViewScenePose(@CameraType override val cameraType: Int = 0) :
    CameraViewScenePose, FakeScenePose() {

    /** Returns the field of view for this camera. */
    override var fov: Fov = Fov(0.0f, 0.0f, 0.0f, 0.0f)

    /** Returns the resolution of this camera view in pixels. */
    override var displayResolutionInPixels: PixelDimensions = PixelDimensions(640, 480)
}
