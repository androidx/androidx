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

package androidx.xr.scenecore.impl.perception;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/**
 * Contains the view projection state. <a
 * href="https://registry.khronos.org/OpenXR/specs/1.0/man/html/XrView.html">...</a>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ViewProjection {
    private final Pose mPose;
    private final Fov mFov;

    public ViewProjection(@NonNull Pose pose, @NonNull Fov fov) {
        mPose = pose;
        mFov = fov;
    }

    /** Returns the location and orientation of the camera/eye pose. */
    public @NonNull Pose getPose() {
        return mPose;
    }

    /** Returns the four sides of the projection / view frustum. */
    public @NonNull Fov getFov() {
        return mFov;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ViewProjection) {
            ViewProjection that = (ViewProjection) object;
            return mPose.equals(that.mPose) && mFov.equals(that.mFov);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mPose.hashCode() + mFov.hashCode();
    }
}
