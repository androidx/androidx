/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.Set;

/**
 * A filter selects camera id with specified lens facing from a camera id set.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LensFacingCameraIdFilter implements CameraIdFilter {
    @CameraSelector.LensFacing
    private int mLensFacing;

    public LensFacingCameraIdFilter(@CameraSelector.LensFacing int lensFacing) {
        mLensFacing = lensFacing;
    }

    @Override
    @NonNull
    public Set<String> filter(@NonNull Set<String> cameraIds) {
        LensFacingCameraIdFilter lensFacingCameraIdFilter =
                CameraX.getCameraFactory().getLensFacingCameraIdFilter(mLensFacing);
        return lensFacingCameraIdFilter.filter(cameraIds);
    }

    /** Returns the lens facing associated with this lens facing camera id filter. */
    @CameraSelector.LensFacing
    public int getLensFacing() {
        return mLensFacing;
    }
}
