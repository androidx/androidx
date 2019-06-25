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
import android.hardware.camera2.TotalCaptureResult;
import android.util.Size;
import android.view.Surface;

/**
 * Implementation for bokeh preview use case.
 *
 * <p>This class should be implemented by OEM and deployed to the target devices. 3P developers
 * don't need to implement this, unless this is used for related testing usage.
 */
public final class BokehPreviewExtenderImpl implements PreviewExtenderImpl {
    private static final int DEFAULT_STAGE_ID = 0;
    private static final int SESSION_STAGE_ID = 101;

    SettableCaptureStage mCaptureStage;
    public BokehPreviewExtenderImpl() {}

    @Override
    public void init(String cameraId, CameraCharacteristics cameraCharacteristics) {
        mCaptureStage = new SettableCaptureStage(DEFAULT_STAGE_ID);
        mCaptureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_OFF);
    }

    @Override
    public boolean isExtensionAvailable(String cameraId,
            CameraCharacteristics cameraCharacteristics) {
        // Implement the logic to check whether the extension function is supported or not.
        return true;
    }

    @Override
    public CaptureStageImpl getCaptureStage() {
        return mCaptureStage;
    }

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.PROCESSOR_TYPE_REQUEST_UPDATE_ONLY;
    }

    // Switches effect every 90 frames
    private RequestUpdateProcessorImpl mRequestUpdateProcessor = new RequestUpdateProcessorImpl() {
        private int mFrameCount = 0;
        private Integer mEffectMode = CaptureRequest.CONTROL_EFFECT_MODE_OFF;

        @Override
        public CaptureStageImpl process(TotalCaptureResult result) {
            mFrameCount++;
            if (mFrameCount % 90 == 0) {
                mCaptureStage = new SettableCaptureStage(DEFAULT_STAGE_ID);
                switch (mEffectMode) {
                    case CaptureRequest.CONTROL_EFFECT_MODE_OFF:
                        mEffectMode = CaptureRequest.CONTROL_EFFECT_MODE_SEPIA;
                        break;
                    case CaptureRequest.CONTROL_EFFECT_MODE_SEPIA:
                    default:
                }
                mCaptureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE,
                        mEffectMode);
                mFrameCount = 0;

                return mCaptureStage;
            }

            return null;
        }

        @Override
        public void onOutputSurface(Surface surface, int imageFormat) {}

        @Override
        public void onResolutionUpdate(Size size) {}

        @Override
        public void onImageFormatUpdate(int imageFormat) {}
    };


    @Override
    public ProcessorImpl getProcessor() {
        return mRequestUpdateProcessor;
    }

    @Override
    public void onInit(String cameraId, CameraCharacteristics cameraCharacteristics,
            Context context) {

    }

    @Override
    public void onDeInit() {

    }

    @Override
    public CaptureStageImpl onPresetSession() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_SEPIA);

        return captureStage;
    }

    @Override
    public CaptureStageImpl onEnableSession() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_SEPIA);

        return captureStage;
    }

    @Override
    public CaptureStageImpl onDisableSession() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_SEPIA);

        return captureStage;
    }
}
