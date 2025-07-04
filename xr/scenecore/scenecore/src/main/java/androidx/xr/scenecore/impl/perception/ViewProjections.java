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

/** Contains the view projections for both eyes */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class ViewProjections {
    private final ViewProjection mLeftEye;
    private final ViewProjection mRightEye;

    public ViewProjections(@NonNull ViewProjection leftEye, @NonNull ViewProjection rightEye) {
        mLeftEye = leftEye;
        mRightEye = rightEye;
    }

    // Returns the left eye view projection.
    public @NonNull ViewProjection getLeftEye() {
        return mLeftEye;
    }

    // Returns the right eye view projection.
    public @NonNull ViewProjection getRightEye() {
        return mRightEye;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ViewProjections) {
            ViewProjections that = (ViewProjections) object;
            return mLeftEye.equals(that.mLeftEye) && mRightEye.equals(that.mRightEye);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mLeftEye.hashCode() + mRightEye.hashCode();
    }
}
