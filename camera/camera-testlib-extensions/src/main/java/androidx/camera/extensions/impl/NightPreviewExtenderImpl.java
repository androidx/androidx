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

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.SessionConfiguration;
import android.util.Pair;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * A simple pass-through implementation for night preview use case.
 *
 * <p>This is only for testing camera-extensions and should not be used as a sample OEM
 * implementation.
 *
 * @since 1.0
 */
public final class NightPreviewExtenderImpl implements PreviewExtenderImpl {
    private static final int DEFAULT_STAGE_ID = 0;
    private static final int SESSION_STAGE_ID = 101;
    private static final int EV_INDEX = 2;

    public NightPreviewExtenderImpl() {
    }

    @Override
    public void init(@NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics) {
    }

    @Override
    public boolean isExtensionAvailable(@NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics) {
        Range<Integer> compensationRange = cameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);

        return compensationRange != null && compensationRange.contains(EV_INDEX);
    }

    @NonNull
    @Override
    public CaptureStageImpl getCaptureStage() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(DEFAULT_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                EV_INDEX);
        return captureStage;
    }

    @NonNull
    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.PROCESSOR_TYPE_REQUEST_UPDATE_ONLY;
    }

    @Nullable
    @Override
    public ProcessorImpl getProcessor() {
        return RequestUpdateProcessorImpls.noUpdateProcessor();
    }

    @Nullable
    @Override
    public List<Pair<Integer, Size[]>> getSupportedResolutions() {
        return null;
    }

    @Override
    public void onInit(@NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics,
            @NonNull Context context) {
    }

    @Override
    public void onDeInit() {

    }

    @Nullable
    @Override
    public CaptureStageImpl onPresetSession() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        return captureStage;
    }

    @Nullable
    @Override
    public CaptureStageImpl onEnableSession() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        return captureStage;
    }

    @Nullable
    @Override
    public CaptureStageImpl onDisableSession() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        return captureStage;
    }

    @Override
    public int onSessionType() {
        return SessionConfiguration.SESSION_REGULAR;
    }

    /**
     * This method is used to check if test lib is running. If OEM implementation exists, invoking
     * this method will throw {@link NoSuchMethodError}. This can be used to determine if OEM
     * implementation is used or not.
     */
    public static void checkTestlibRunning() {}
}
