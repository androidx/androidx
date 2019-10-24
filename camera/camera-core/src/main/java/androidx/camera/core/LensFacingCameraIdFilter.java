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

import java.util.Set;
import java.util.TreeSet;

/**
 * A filter selects camera id with specified lens facing from a camera id set.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class LensFacingCameraIdFilter implements CameraIdFilter {
    /** Returns the lens facing associated with this lens facing camera id filter. */
    @NonNull
    public abstract LensFacing getLensFacing();

    /** Creates a lens facing camera id filter with a given set of ids for a LensFacing. */
    @NonNull
    public static LensFacingCameraIdFilter createLensFacingCameraIdFilterWithIdSet(
            @NonNull LensFacing lensFacing, @Nullable Set<String> ids) {
        return new SettableLensFacingCameraIdFilter(lensFacing, ids);
    }

    /** Creates a lens facing camera id filter. */
    @NonNull
    public static LensFacingCameraIdFilter createLensFacingCameraIdFilter(
            @NonNull LensFacing lensFacing) {
        if (CameraX.isInitialized()) {
            return CameraX.getCameraFactory().getLensFacingCameraIdFilter(lensFacing);
        }
        // Returns a no ops camera id filter if CameraX hasn't been initialized.
        return createLensFacingCameraIdFilterWithIdSet(lensFacing, null);
    }

    private static final class SettableLensFacingCameraIdFilter extends LensFacingCameraIdFilter {
        private final LensFacing mLensFacing;
        @Nullable
        private final Set<String> mIds;

        SettableLensFacingCameraIdFilter(LensFacing lensFacing, @Nullable Set<String> ids) {
            mLensFacing = lensFacing;
            mIds = ids;
        }

        @Override
        @NonNull
        public Set<String> filter(@NonNull Set<String> cameraIds) {
            if (mIds == null) {
                return cameraIds;
            }

            // Use a TreeSet to maintain lexical order of ids
            Set<String> resultCameraIdSet = new TreeSet<>(cameraIds);
            resultCameraIdSet.retainAll(mIds);
            return resultCameraIdSet;
        }

        @Override
        @NonNull
        public LensFacing getLensFacing() {
            return mLensFacing;
        }
    }
}
