/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo

/**
 * Interface for a SceneCore camera view ActivityPose. This is the position of a user's camera.
 *
 * <p>The camera's field of view can be retrieved from this CameraViewActivityPose.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface CameraViewActivityPose : ActivityPose {
    /** Returns the type of camera that this space represents. */
    @CameraType public val cameraType: Int

    /** Returns the field of view for this camera. */
    public val fov: Fov

    /** Returns the resolution of this camera view in pixels. */
    public val displayResolutionInPixels: PixelDimensions

    /**
     * The angles (in radians) representing the sides of the view frustum. These are not expected to
     * change over the lifetime of the session but in rare cases may change due to updated camera
     * settings
     */
    // TODO: b/419311998 Replace usages of this type with androidx.xr.runtime.FieldOfView
    public class Fov(
        public val angleLeft: Float,
        public val angleRight: Float,
        public val angleUp: Float,
        public val angleDown: Float,
    ) {}

    public annotation class CameraType {
        public companion object {
            public const val CAMERA_TYPE_UNKNOWN: Int = 0
            public const val CAMERA_TYPE_LEFT_EYE: Int = 1
            public const val CAMERA_TYPE_RIGHT_EYE: Int = 2
        }
    }
}
