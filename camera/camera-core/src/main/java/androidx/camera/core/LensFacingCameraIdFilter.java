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
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A filter selects camera id with specified lens facing from a camera id set.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class LensFacingCameraIdFilter implements CameraIdFilter {
    /** Returns the lens facing associated with this lens facing camera id filter. */
    @NonNull
    public abstract CameraX.LensFacing getLensFacing();

    /** Creates a lens facing camera id filter with a predefined camera map. */
    @NonNull
    public static LensFacingCameraIdFilter createLensFacingCameraIdFilterWithCameraMap(
            @NonNull CameraX.LensFacing lensFacing, @Nullable Map<String, BaseCamera> cameraMap) {
        return new SettableLensFacingCameraIdFilter(lensFacing, cameraMap);
    }

    /** Creates a lens facing camera id filter. */
    @NonNull
    public static LensFacingCameraIdFilter createLensFacingCameraIdFilter(
            @NonNull CameraX.LensFacing lensFacing) {
        if (CameraX.isInitialized()) {
            return CameraX.getCameraFactory().getLensFacingCameraIdFilter(lensFacing);
        }
        // Returns a no ops camera id filter if CameraX hasn't been initialized.
        return createLensFacingCameraIdFilterWithCameraMap(lensFacing, null);
    }

    private static final class SettableLensFacingCameraIdFilter extends LensFacingCameraIdFilter {
        private CameraX.LensFacing mLensFacing;
        private Map<String, BaseCamera> mCameraMap;

        SettableLensFacingCameraIdFilter(CameraX.LensFacing lensFacing,
                @Nullable Map<String, BaseCamera> cameraMap) {
            mLensFacing = lensFacing;
            mCameraMap = cameraMap;
        }

        @Override
        @NonNull
        public Set<String> filter(@NonNull Set<String> cameraIds) {
            if (mCameraMap == null) {
                return cameraIds;
            }

            Set<String> resultCameraIdSet = new LinkedHashSet<>();

            for (String cameraId : cameraIds) {
                if (mCameraMap.containsKey(cameraId)) {
                    try {
                        if (mCameraMap.get(cameraId).getCameraInfo().getLensFacing()
                                == mLensFacing) {
                            resultCameraIdSet.add(cameraId);
                        }
                    } catch (CameraInfoUnavailableException e) {
                        throw new IllegalArgumentException("Unable to get camera info.", e);
                    }
                }
            }

            return resultCameraIdSet;
        }

        @Override
        @NonNull
        public CameraX.LensFacing getLensFacing() {
            return mLensFacing;
        }
    }
}
