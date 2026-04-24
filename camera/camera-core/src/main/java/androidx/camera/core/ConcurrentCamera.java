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

import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;

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
 * <p>Concurrent camera also supports composition mode, where multiple camera streams are
 * composited into a single stream. This can be used for Picture-in-Picture or other custom
 * layouts. Use {@link CompositionSettings} to configure the layout of each camera stream.
 *
 * <p>CameraX currently only supports dual concurrent camera, which allows two cameras
 * operating at the same time, with at most two {@link UseCase}s bound for each. The max
 * resolution is 720p or 1440p, more details in the following link, see
 * <a href="https://developer.android.com/reference/android/hardware/camera2/CameraManager#getConcurrentCameraIds()">concurrent camera streaming</a>
 *
 */
public class ConcurrentCamera {

    private @NonNull List<Camera> mCameras;

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
    public @NonNull List<Camera> getCameras() {
        return mCameras;
    }

    /**
     * Sets the composition settings for concurrent camera.
     *
     * <p>This method can be used to dynamically update the composition settings of the concurrent
     * cameras, for example, to change the position or size of a Picture-in-Picture window.
     *
     * <p>The composition settings will be applied to the cameras in the order they were bound.
     * The first composition setting is for the primary camera, and the second is for the
     * secondary camera.
     *
     * <p>Currently only 2 cameras are supported, so the size of the list must be 2.
     *
     * <p>The following code snippet demonstrates how to swap the primary and secondary camera
     * positions:
     * <pre>
     * {@code
     * // Primary becomes PiP, Secondary becomes full screen
     * CompositionSettings primary = new CompositionSettings.Builder()
     *         .setOffset(0.5f, 0.5f)
     *         .setScale(0.3f, 0.3f)
     *         .setZOrder(1) // Display on top
     *         .build();
     * CompositionSettings secondary = new CompositionSettings.Builder()
     *         .setZOrder(0)
     *         .build();
     * concurrentCamera.setCompositionSettings(Arrays.asList(primary, secondary));
     * }
     * </pre>
     *
     * @param compositionSettings A list of {@link CompositionSettings} for the concurrent
     *                            cameras.
     * @throws IllegalStateException if the camera is not in concurrent camera composition mode.
     * @throws IllegalArgumentException if the size of the composition settings list is not 2.
     */
    public void setCompositionSettings(
            @NonNull List<CompositionSettings> compositionSettings) {
        if (!mCameras.isEmpty()) {
            // ConcurrentCamera only has one camera in composition mode
            Camera camera = mCameras.get(0);
            camera.setCompositionSettings(compositionSettings);
        }
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

        private @NonNull CameraSelector mCameraSelector;
        private @NonNull LifecycleOwner mLifecycleOwner;
        private @NonNull UseCaseGroup mUseCaseGroup;
        private @NonNull CompositionSettings mCompositionSettings;

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
        public @NonNull CameraSelector getCameraSelector() {
            return mCameraSelector;
        }

        /**
         * Returns {@link LifecycleOwner}.
         * @return {@link LifecycleOwner} instance.
         */
        public @NonNull LifecycleOwner getLifecycleOwner() {
            return mLifecycleOwner;
        }

        /**
         * Returns {@link UseCaseGroup}.
         * @return {@link UseCaseGroup} instance.
         */
        public @NonNull UseCaseGroup getUseCaseGroup() {
            return mUseCaseGroup;
        }

        /**
         * Returns {@link CompositionSettings}.
         * @return {@link CompositionSettings} instance.
         */
        public @NonNull CompositionSettings getCompositionSettings() {
            return mCompositionSettings;
        }
    }
}
