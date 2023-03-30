/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.concurrent;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.util.List;

/**
 * Concurrent Camera Configuration.
 */
@RequiresApi(21)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ConcurrentCameraConfig {
    @NonNull
    private List<SingleCameraConfig> mSingleCameraConfigs;

    ConcurrentCameraConfig(@NonNull List<SingleCameraConfig> singleCameraConfigs) {
        mSingleCameraConfigs = singleCameraConfigs;
    }

    /**
     * Returns single camera configs.
     * @return list of single camera configs.
     */
    @NonNull
    public List<SingleCameraConfig> getSingleCameraConfigs() {
        return mSingleCameraConfigs;
    }

    /**
     * Builder for concurrent camera config.
     */
    public static class Builder {

        @NonNull
        private List<SingleCameraConfig> mSingleCameraConfigs;

        public Builder() {}

        /**
         * Sets the list of single camera configs.
         * @param singleCameraConfigs list of single camera configs.
         * @return {@link Builder}.
         */
        @NonNull
        public Builder setCameraConfigs(@NonNull List<SingleCameraConfig> singleCameraConfigs) {
            mSingleCameraConfigs = singleCameraConfigs;
            return this;
        }

        /**
         * Builds the {@link ConcurrentCameraConfig}.
         * @return {@link ConcurrentCameraConfig}.
         */
        @NonNull
        public ConcurrentCameraConfig build() {
            return new ConcurrentCameraConfig(mSingleCameraConfigs);
        }

    }
}
