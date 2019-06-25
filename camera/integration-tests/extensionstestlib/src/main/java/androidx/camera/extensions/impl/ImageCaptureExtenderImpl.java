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

package androidx.camera.extensions.impl;

import android.hardware.camera2.CameraCharacteristics;

import java.util.List;

/**
 * Provides abstract methods that the OEM needs to implement to enable extensions for image capture.
 */
public interface ImageCaptureExtenderImpl extends ExtenderStateListener {
    /**
     * Indicates whether the extension is supported on the device.
     *
     * @param cameraId The camera2 id string of the camera.
     * @param cameraCharacteristics The {@link CameraCharacteristics} of the camera.
     * @return true if the extension is supported, otherwise false
     */
    boolean isExtensionAvailable(String cameraId, CameraCharacteristics cameraCharacteristics);

    /**
     * Initializes the extender to be used with the specified camera.
     *
     * <p>This should be called before any other method on the extender. The exception is {@link
     * #isExtensionAvailable(String, CameraCharacteristics)}.
     *
     * @param cameraId The camera2 id string of the camera.
     * @param cameraCharacteristics The {@link CameraCharacteristics} of the camera.
     */
    void init(String cameraId, CameraCharacteristics cameraCharacteristics);

    /**
     * The processing that will be done on a set of captures to create and image with the effect.
     */
    CaptureProcessorImpl getCaptureProcessor();

    /** The set of captures that are needed to create an image with the effect. */
    List<CaptureStageImpl> getCaptureStages();

    /**
     * Returns the maximum size of the list returned by {@link #getCaptureStages()}.
     * @return the maximum count.
     */
    int getMaxCaptureStage();
}

