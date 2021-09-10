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
import androidx.camera.core.CameraFilter;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * A filter that filters camera based on lens facing.
 */
public class LensFacingCameraFilter implements CameraFilter {
    @CameraSelector.LensFacing
    private int mLensFacing;

    public LensFacingCameraFilter(@CameraSelector.LensFacing int lensFacing) {
        mLensFacing = lensFacing;
    }

    @NonNull
    @Override
    public List<CameraInfo> filter(@NonNull List<CameraInfo> cameraInfos) {
        List<CameraInfo> result = new ArrayList<>();
        for (CameraInfo cameraInfo : cameraInfos) {
            Preconditions.checkArgument(cameraInfo instanceof CameraInfoInternal,
                    "The camera info doesn't contain internal implementation.");
            Integer lensFacing = ((CameraInfoInternal) cameraInfo).getLensFacing();
            if (lensFacing != null && lensFacing == mLensFacing) {
                result.add(cameraInfo);
            }
        }

        return result;
    }

    /** Returns the lens facing associated with this lens facing camera id filter. */
    @CameraSelector.LensFacing
    public int getLensFacing() {
        return mLensFacing;
    }
}
