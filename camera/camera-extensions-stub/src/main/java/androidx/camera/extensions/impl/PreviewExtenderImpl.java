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
import android.hardware.camera2.TotalCaptureResult;

/**
 * Provides abstract methods that the OEM needs to implement to enable extensions in the preview.
 */
public interface PreviewExtenderImpl extends ExtenderStateListener {
    /** The different types of the preview processing. */
    enum ProcessorType {
        /** Processor which only updates the {@link CaptureStageImpl}. */
        PROCESSOR_TYPE_REQUEST_UPDATE_ONLY,
        /** Processor which updates the received {@link android.media.Image}. */
        PROCESSOR_TYPE_IMAGE_PROCESSOR,
        /** No processor, only a {@link CaptureStageImpl} is defined. */
        PROCESSOR_TYPE_NONE
    }

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
     * The set of parameters required to produce the effect on the preview stream.
     *
     * <p> This will be the initial set of parameters used for the preview
     * {@link android.hardware.camera2.CaptureRequest}. If the {@link ProcessorType} is defined as
     * {@link ProcessorType#PROCESSOR_TYPE_REQUEST_UPDATE_ONLY} then this will be updated when
     * the {@link RequestUpdateProcessorImpl#process(TotalCaptureResult)} from {@link
     * #getProcessor()} has been called, this should be updated to reflect the new {@link
     * CaptureStageImpl}. If the processing step returns a {@code null}, meaning the required
     * parameters has not changed, then calling this will return the previous non-null value.
     */
    CaptureStageImpl getCaptureStage();

    /** The type of preview processing to use. */
    ProcessorType getProcessorType();

    /**
     * Returns a processor which only updates the {@link CaptureStageImpl}.
     *
     * <p>The type of processor is dependent on the return of {@link #getProcessorType()}. If it
     *
     */
    ProcessorImpl getProcessor();
}
