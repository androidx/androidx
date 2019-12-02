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

package androidx.camera.core.impl.utils;

import static androidx.camera.core.CameraDeviceConfig.OPTION_CAMERA_ID_FILTER;
import static androidx.camera.core.CameraDeviceConfig.OPTION_LENS_FACING;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraDeviceConfig;
import androidx.camera.core.CameraIdFilter;
import androidx.camera.core.CameraIdFilterSet;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Config;
import androidx.camera.core.MutableOptionsBundle;
import androidx.camera.core.OptionsBundle;

import java.util.Set;

/**
 * Utility helper methods for CameraSelector.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class CameraSelectorUtil {

    /**
     * Converts a {@link CameraSelector} to a {@link CameraDeviceConfig}.
     * TODO(142840814): This will no longer be needed once CameraDeviceConfig is removed.
     */
    @NonNull
    public static CameraDeviceConfig toCameraDeviceConfig(@NonNull CameraSelector cameraSelector) {
        MutableOptionsBundle mutableOptionsBundle = MutableOptionsBundle.create();
        Integer lensFacing = cameraSelector.getLensFacing();
        if (lensFacing != null) {
            mutableOptionsBundle.insertOption(OPTION_LENS_FACING, lensFacing);
        }

        Set<CameraIdFilter> idFilters = cameraSelector.getCameraFilterSet();
        if (!idFilters.isEmpty()) {
            CameraIdFilter combinedFilter = idFilters.iterator().next();
            if (idFilters.size() > 1) {
                CameraIdFilterSet filterSet = new CameraIdFilterSet();
                for (CameraIdFilter filter : idFilters) {
                    filterSet.addCameraIdFilter(filter);
                }
                combinedFilter = filterSet;
            }
            mutableOptionsBundle.insertOption(OPTION_CAMERA_ID_FILTER, combinedFilter);
        }

        return new CameraSelectorDeviceConfig(OptionsBundle.from(mutableOptionsBundle));
    }

    // TODO(142840814): This will no longer be needed once CameraDeviceConfig is removed.
    private static class CameraSelectorDeviceConfig implements CameraDeviceConfig, Config {

        private final OptionsBundle mConfig;

        CameraSelectorDeviceConfig(OptionsBundle options) {
            mConfig = options;
        }

        // Implementations of CameraDeviceConfig default methods

        /**
         * Returns the lens-facing direction of the camera being configured.
         *
         * @param valueIfMissing The value to return if this configuration option has not been set.
         * @return The stored value or <code>valueIfMissing</code> if the value does not exist in
         * this
         * configuration.
         */
        @Override
        @Nullable
        public Integer getLensFacing(@Nullable Integer valueIfMissing) {
            try {
                return getLensFacing();
            } catch (IllegalArgumentException e) {
                return valueIfMissing;
            }
        }

        /**
         * Retrieves the lens facing direction for the primary camera to be configured.
         *
         * @return The stored value, if it exists in this configuration.
         * @throws IllegalArgumentException if the option does not exist in this configuration.
         */
        @Override
        @CameraSelector.LensFacing
        public int getLensFacing() {
            return retrieveOption(OPTION_LENS_FACING);
        }

        /**
         * Returns the set of {@link CameraIdFilter} that filter out unavailable camera id.
         *
         * @param valueIfMissing The value to return if this configuration option has not been set.
         * @return The stored value or <code>ValueIfMissing</code> if the value does not exist in
         * this
         * configuration.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @Nullable
        public CameraIdFilter getCameraIdFilter(@Nullable CameraIdFilter valueIfMissing) {
            return retrieveOption(OPTION_CAMERA_ID_FILTER, valueIfMissing);
        }

        /**
         * Returns the set of {@link CameraIdFilter} that filter out unavailable camera id.
         *
         * @return The stored value, if it exists in the configuration.
         * @throws IllegalArgumentException if the option does not exist in this configuration.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public CameraIdFilter getCameraIdFilter() {
            return retrieveOption(OPTION_CAMERA_ID_FILTER);
        }

        // Start of the default implementation of Config
        // *****************************************************************************************

        // Implementations of Config default methods

        @Override
        public boolean containsOption(@NonNull Option<?> id) {
            return mConfig.containsOption(id);
        }


        @Override
        @Nullable
        public <ValueT> ValueT retrieveOption(@NonNull Option<ValueT> id) {
            return mConfig.retrieveOption(id);
        }

        @Override
        @Nullable
        public <ValueT> ValueT retrieveOption(@NonNull Option<ValueT> id,
                @Nullable ValueT valueIfMissing) {
            return mConfig.retrieveOption(id, valueIfMissing);
        }

        @Override
        public void findOptions(@NonNull String idStem, @NonNull OptionMatcher matcher) {
            mConfig.findOptions(idStem, matcher);
        }

        @Override
        @NonNull
        public Set<Option<?>> listOptions() {
            return mConfig.listOptions();
        }
    }

    private CameraSelectorUtil() {
    }
}
