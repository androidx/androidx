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

package androidx.camera.core;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.LifecycleOwner;

import java.util.List;

/**
 * Concurrent camera is a new feature introduced from Android 11, which supports simultaneous
 * streaming of camera devices, for example, it allows a device to have both the front and back
 * cameras operating at the same time.
 *
 * <p>A concurrent camera object is returned after binding concurrent cameras to
 * {@link LifecycleOwner}. It includes a list of {@link Camera}s which are operating at the same
 * time. Before binding to {@link LifecycleOwner}, check
 * {@link PackageManager#FEATURE_CAMERA_CONCURRENT} to see whether this device is supporting
 * concurrent camera or not.
 *
 * <p>CameraX currently only supports dual concurrent camera, which allows two cameras
 * operating at the same time, with at most two {@link UseCase}s bound for each. The max
 * resolution is 720p or 1440p, more details in the following link, see
 * <a href="https://developer.android.com/reference/android/hardware/camera2/CameraManager#getConcurrentCameraIds()">concurrent camera streaming</a>
 *
 */
public class ConcurrentCamera {

    @NonNull
    private List<Camera> mCameras;

    /**
     * Constructor of concurrent cameras.
     * @param cameras list of {@link Camera}.
     */
    public ConcurrentCamera(@NonNull List<Camera> cameras) {
        mCameras = cameras;
    }

    /**
     * Gets the list of cameras.
     */
    @NonNull
    public List<Camera> getCameras() {
        return mCameras;
    }

    /**
     * Configuration for a single camera in concurrent camera mode, including
     * {@link CameraSelector}, {@link LifecycleOwner} and {@link UseCaseGroup}.
     *
     * <p>The configuration is used to bring up a lifecycle-aware camera with {@link UseCase}
     * bound. This class can used to bind concurrent cameras to {@link LifecycleOwner}, each
     * {@link SingleCameraConfig} represents a single camera.
     */
    public static final class SingleCameraConfig {

        @NonNull
        private CameraSelector mCameraSelector;
        @NonNull
        private LifecycleOwner mLifecycleOwner;
        @NonNull
        private UseCaseGroup mUseCaseGroup;
        @NonNull
        private CompositionSettings mCompositionSettings;

        /**
         * Constructor of a {@link SingleCameraConfig} for concurrent cameras.
         *
         * @param cameraSelector {@link CameraSelector}.
         * @param useCaseGroup {@link UseCaseGroup}.
         * @param lifecycleOwner {@link LifecycleOwner}.
         */
        public SingleCameraConfig(
                @NonNull CameraSelector cameraSelector,
                @NonNull UseCaseGroup useCaseGroup,
                @NonNull LifecycleOwner lifecycleOwner) {
            this(cameraSelector, useCaseGroup, CompositionSettings.DEFAULT, lifecycleOwner);
        }

        /**
         * Constructor of a {@link SingleCameraConfig} for concurrent cameras.
         *
         * @param cameraSelector {@link CameraSelector}.
         * @param useCaseGroup {@link UseCaseGroup}.
         * @param compositionSettings {@link CompositionSettings}.
         * @param lifecycleOwner {@link LifecycleOwner}.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public SingleCameraConfig(
                @NonNull CameraSelector cameraSelector,
                @NonNull UseCaseGroup useCaseGroup,
                @NonNull CompositionSettings compositionSettings,
                @NonNull LifecycleOwner lifecycleOwner) {
            this.mCameraSelector = cameraSelector;
            this.mUseCaseGroup = useCaseGroup;
            this.mCompositionSettings = compositionSettings;
            this.mLifecycleOwner = lifecycleOwner;
        }

        /**
         * Returns {@link CameraSelector}.
         * @return {@link CameraSelector} instance.
         */
        @NonNull
        public CameraSelector getCameraSelector() {
            return mCameraSelector;
        }

        /**
         * Returns {@link LifecycleOwner}.
         * @return {@link LifecycleOwner} instance.
         */
        @NonNull
        public LifecycleOwner getLifecycleOwner() {
            return mLifecycleOwner;
        }

        /**
         * Returns {@link UseCaseGroup}.
         * @return {@link UseCaseGroup} instance.
         */
        @NonNull
        public UseCaseGroup getUseCaseGroup() {
            return mUseCaseGroup;
        }

        /**
         * Returns {@link CompositionSettings}.
         * @return {@link CompositionSettings} instance.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public CompositionSettings getCompositionSettings() {
            return mCompositionSettings;
        }
    }
}
