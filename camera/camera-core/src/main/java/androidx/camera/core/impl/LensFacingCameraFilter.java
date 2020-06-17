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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.annotation.experimental.UseExperimental;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraFilter;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalCameraFilter;
import androidx.core.util.Preconditions;

import java.util.LinkedHashSet;

/**
 * A filter that filters camera based on lens facing.
 */
@UseExperimental(markerClass = ExperimentalCameraFilter.class)
public class LensFacingCameraFilter implements CameraFilter {
    @CameraSelector.LensFacing
    private int mLensFacing;

    public LensFacingCameraFilter(@CameraSelector.LensFacing int lensFacing) {
        mLensFacing = lensFacing;
    }

    @NonNull
    @Override
    public LinkedHashSet<Camera> filter(@NonNull LinkedHashSet<Camera> cameras) {
        LinkedHashSet<Camera> resultCameras = new LinkedHashSet<>();
        for (Camera camera : cameras) {
            Preconditions.checkState(camera instanceof CameraInternal,
                    "The camera doesn't contain internal implementation.");
            Integer lensFacing = ((CameraInternal) camera).getCameraInfoInternal().getLensFacing();
            if (lensFacing != null && lensFacing == mLensFacing) {
                resultCameras.add(camera);
            }
        }

        return resultCameras;
    }

    /** Returns the lens facing associated with this lens facing camera id filter. */
    @CameraSelector.LensFacing
    public int getLensFacing() {
        return mLensFacing;
    }
}
