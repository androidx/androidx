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
import androidx.annotation.RestrictTo.Scope;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A set of requirements and priorities used to select a camera.
 */
public final class CameraSelector {
    /** A static {@link CameraSelector} that selects the default front facing camera. */
    public static final CameraSelector DEFAULT_FRONT_CAMERA =
            new CameraSelector.Builder().requireLensFacing(LensFacing.FRONT).build();
    /** A static {@link CameraSelector} that selects the default back facing camera. */
    public static final CameraSelector DEFAULT_BACK_CAMERA =
            new CameraSelector.Builder().requireLensFacing(LensFacing.BACK).build();

    private LinkedHashSet<CameraIdFilter> mCameraFilterSet;

    CameraSelector(LinkedHashSet<CameraIdFilter> cameraFilterSet) {
        mCameraFilterSet = cameraFilterSet;
    }

    /**
     * Selects the first camera that filtered by the {@link CameraIdFilter} assigned to the
     * selector.
     *
     * <p>The camera ids filtered must be contained in the input set. Otherwise it will throw an
     * exception.
     *
     * @param cameraIds The camera id set being filtered.
     * @return The first camera filtered.
     * @throws IllegalArgumentException If there's no available camera after being filtered or
     *                                  the filtered camera ids aren't contained in the input set.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public String select(@NonNull Set<String> cameraIds) {
        Set<String> resultCameraSet = new LinkedHashSet<>();
        for (CameraIdFilter filter : mCameraFilterSet) {
            resultCameraSet = filter.filter(cameraIds);
            // If the result is empty or has extra camera id that isn't contained in the
            // input, throws an exception.
            if (resultCameraSet.isEmpty()) {
                throw new IllegalArgumentException("No available camera can be found.");
            } else if (!cameraIds.containsAll(resultCameraSet)) {
                throw new IllegalArgumentException("The output isn't contained in the input.");
            }
            cameraIds = resultCameraSet;
        }
        return resultCameraSet.iterator().next();
    }

    /**
     * Gets the set of {@link CameraIdFilter} assigned to this camera
     * selector.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public LinkedHashSet<CameraIdFilter> getCameraFilterSet() {
        return mCameraFilterSet;
    }

    /**
     * Returns a single lens facing from this camera selector, or null if lens facing has not
     * been set.
     *
     * @return The lens facing.
     * @throws IllegalStateException if a single lens facing cannot be resolved, such as if
     *                               multiple conflicting lens facing requirements exist in this
     *                               camera selector.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public LensFacing getLensFacing() {
        LensFacing currentLensFacing = null;
        for (CameraIdFilter filter : mCameraFilterSet) {
            if (filter instanceof LensFacingCameraIdFilter) {
                LensFacing newLensFacing = ((LensFacingCameraIdFilter) filter).getLensFacing();
                if (currentLensFacing == null) {
                    currentLensFacing = newLensFacing;
                } else if (newLensFacing != currentLensFacing) {
                    // TODO(b/122975195): Now we assume the lens facing of a camera is either
                    //  FRONT or BACK, so if there's conflicting lens facings set, throws an
                    //  exception. It needs to be revisited if we have a third lens facing enum
                    //  in the future.
                    throw new IllegalStateException(
                            "Multiple conflicting lens facing requirements exist.");
                }
            }
        }

        return currentLensFacing;
    }

    /** Builder for a {@link CameraSelector}. */
    public static final class Builder {
        private final LinkedHashSet<CameraIdFilter> mCameraFilterSet;

        public Builder() {
            mCameraFilterSet = new LinkedHashSet<>();
        }

        private Builder(@NonNull LinkedHashSet<CameraIdFilter> cameraFilterSet) {
            mCameraFilterSet = cameraFilterSet;
        }

        /**
         * Requires a camera with the specified lens facing.
         *
         * <p>If lens facing is already set, this will add extra requirement for lens facing
         * instead of replacing the previous setting.
         */
        @NonNull
        public Builder requireLensFacing(@NonNull LensFacing lensFacing) {
            CameraIdFilter cameraFilter = CameraX.getCameraFactory().getLensFacingCameraIdFilter(
                    lensFacing);
            mCameraFilterSet.add(cameraFilter);
            return this;
        }

        /**
         * Appends a CameraIdFilter to the current set of filters.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder appendFilter(@NonNull CameraIdFilter cameraFilter) {
            mCameraFilterSet.add(cameraFilter);
            return this;
        }

        /**
         * Generates a Builder from another CameraSelector object.
         *
         * @param cameraSelector An existing CameraSelector.
         * @return The new Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Builder fromSelector(@NonNull CameraSelector cameraSelector) {
            CameraSelector.Builder builder = new CameraSelector.Builder(
                    cameraSelector.getCameraFilterSet());
            return builder;
        }

        /** Builds the {@link CameraSelector}. */
        @NonNull
        public CameraSelector build() {
            return new CameraSelector(mCameraFilterSet);
        }
    }
}
