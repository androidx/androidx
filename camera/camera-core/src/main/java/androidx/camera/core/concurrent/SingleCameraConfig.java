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
import androidx.camera.core.CameraSelector;
import androidx.camera.core.UseCaseGroup;
import androidx.lifecycle.LifecycleOwner;

/**
 * Single Camera Configuration.
 */
@RequiresApi(21)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SingleCameraConfig {

    @NonNull
    private CameraSelector mCameraSelector;
    @NonNull
    private LifecycleOwner mLifecycleOwner;
    @NonNull
    private UseCaseGroup mUseCaseGroup;

    SingleCameraConfig(
            @NonNull CameraSelector cameraSelector,
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull UseCaseGroup useCaseGroup) {
        this.mCameraSelector = cameraSelector;
        this.mLifecycleOwner = lifecycleOwner;
        this.mUseCaseGroup = useCaseGroup;
    }

    @NonNull
    public CameraSelector getCameraSelector() {
        return mCameraSelector;
    }

    @NonNull
    public LifecycleOwner getLifecycleOwner() {
        return mLifecycleOwner;
    }

    @NonNull
    public UseCaseGroup getUseCaseGroup() {
        return mUseCaseGroup;
    }

    /**
     * Build for single camera config.
     */
    public static class Builder {
        @NonNull
        private CameraSelector mCameraSelector;
        @NonNull
        private LifecycleOwner mLifecycleOwner;
        @NonNull
        private UseCaseGroup mUseCaseGroup;

        public Builder() {}

        /**
         * Sets {@link CameraSelector}.
         * @param cameraSelector
         * @return {@link Builder}.
         */
        @NonNull
        public Builder setCameraSelector(@NonNull CameraSelector cameraSelector) {
            mCameraSelector = cameraSelector;
            return this;
        }

        /**
         * Sets {@link LifecycleOwner}.
         * @param lifecycleOwner
         * @return {@link Builder}.
         */
        @NonNull
        public Builder setLifecycleOwner(@NonNull LifecycleOwner lifecycleOwner) {
            mLifecycleOwner = lifecycleOwner;
            return this;
        }

        /**
         * Sets {@link UseCaseGroup}.
         * @param useCaseGroup
         * @return {@link Builder}.
         */
        @NonNull
        public Builder setUseCaseGroup(@NonNull UseCaseGroup useCaseGroup) {
            mUseCaseGroup = useCaseGroup;
            return this;
        }

        /**
         * Builds the {@link SingleCameraConfig}.
         * @return {@link SingleCameraConfig}.
         */
        @NonNull
        public SingleCameraConfig build() {
            return new SingleCameraConfig(mCameraSelector, mLifecycleOwner, mUseCaseGroup);
        }
    }
}
