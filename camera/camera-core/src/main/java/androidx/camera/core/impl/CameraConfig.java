/*
 * Copyright 2020 The Android Open Source Project
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

/**
 * Configuration for a {@link androidx.camera.core.Camera}.
 */
public interface CameraConfig extends ReadableConfig {

    // Option Declarations:
    // *********************************************************************************************

    /**
     * Option: camerax.core.camera.cameraFilter
     */
    Option<CameraFilter> OPTION_CAMERA_FILTER =
            Option.create("camerax.core.camera.cameraFilter", CameraFilter.class);

    Option<UseCaseConfigFactory> OPTION_USECASE_CONFIG_FACTORY =
            Option.create("camerax.core.camera.useCaseConfigFactory",
                    UseCaseConfigFactory.class);

    /**
     * Retrieves the camera filter from this configuration.
     *
     * <p> This filter is used to filter out additional cameras.
     *
     * @return The stored value, if it exists in this configuration. Otherwise a filter that does
     * not filter out any cameras.
     */
    @NonNull
    default CameraFilter getCameraFilter() {
        return retrieveOption(OPTION_CAMERA_FILTER, CameraFilters.ANY);
    }

    /**
     * Retrieves the use case config factory instance.
     */
    @NonNull
    UseCaseConfigFactory getUseCaseConfigFactory();

    /**
     * Builder for creating a {@link CameraConfig}.
     * @param <B> the top level builder type for which this builder is composed with.
     */
    interface Builder<B> {
        /**
         * Sets the {@link CameraFilter} to apply.
         *
         * <p> The filter which will be additionally applied to
         * {@link androidx.camera.core.CameraSelector} when selecting a camera.
         *
         * @param cameraFilter the {@link CameraFilter} to apply.
         * @return The current Builder.
         */
        @NonNull
        B setCameraFilter(@NonNull CameraFilter cameraFilter);

        @NonNull
        B setUseCaseConfigFactory(@NonNull UseCaseConfigFactory factory);
    }
}
