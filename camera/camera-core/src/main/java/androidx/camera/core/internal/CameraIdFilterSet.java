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

package androidx.camera.core.internal;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.CameraIdFilter;

import java.util.HashSet;
import java.util.Set;

/**
 * A {@link CameraIdFilter} which holds multiple {@link CameraIdFilter} and filters camera ids
 * with all of them.
 */
public final class CameraIdFilterSet implements CameraIdFilter {
    private Set<CameraIdFilter> mCameraIdFilterSet = new HashSet<>();

    @Override
    @NonNull
    public Set<String> filter(@NonNull Set<String> cameraIds) {
        for (CameraIdFilter filter : mCameraIdFilterSet) {
            cameraIds = filter.filter(cameraIds);
        }

        return cameraIds;
    }

    /** Gets a set of {@link CameraIdFilter}. */
    @NonNull
    public Set<CameraIdFilter> getCameraIdFilters() {
        return mCameraIdFilterSet;
    }

    /** Adds a {@link CameraIdFilter} to the set. */
    public void addCameraIdFilter(@NonNull CameraIdFilter cameraIdFilter) {
        if (cameraIdFilter instanceof CameraIdFilterSet) {
            // If the additional CameraIdFilter is a CameraIdFilterSet, add all of the single
            // CameraIdFilter one by one.
            mCameraIdFilterSet.addAll(((CameraIdFilterSet) cameraIdFilter).getCameraIdFilters());
        } else {
            mCameraIdFilterSet.add(cameraIdFilter);
        }
    }
}
