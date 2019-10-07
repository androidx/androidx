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
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Implementation for HDR preview use case.
 *
 * <p>This class should be implemented by OEM and deployed to the target devices. 3P developers
 * don't need to implement this, unless this is used for related testing usage.
 *
 * @since 1.0
 */
public final class HdrPreviewExtenderImpl implements PreviewExtenderImpl {
    private static final int DEFAULT_STAGE_ID = 0;

    GLImage2SurfaceRenderer mRenderer;

    public HdrPreviewExtenderImpl() { }

    @Override
    public void init(String cameraId, CameraCharacteristics cameraCharacteristics) {
    }

    @Override
    public boolean isExtensionAvailable(@NonNull String cameraId,
            @Nullable CameraCharacteristics cameraCharacteristics) {
        // Implement the logic to check whether the extension function is supported or not.
        return true;
    }

    @Override
    public CaptureStageImpl getCaptureStage() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(DEFAULT_STAGE_ID);

        return captureStage;
    }

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.PROCESSOR_TYPE_IMAGE_PROCESSOR;
    }

    @Override
    public ProcessorImpl getProcessor() {
        return mProcessor;
    }

    @Override
    public List<Pair<Integer, Size[]>> getSupportedResolutions() {
        return null;
    }

    private PreviewImageProcessorImpl mProcessor = new PreviewImageProcessorImpl() {
        Surface mSurface;
        Size mSize;


        private void setWindowSurface() {
            if (mSurface != null && mSize != null) {
                mRenderer.setWindowSurface(mSurface, mSize.getWidth(), mSize.getHeight());
            }
        }

        @Override
        public void onOutputSurface(Surface surface, int imageFormat) {
            mSurface = surface;
            setWindowSurface();
        }

        @Override
        public void process(Image image, TotalCaptureResult result) {
            mRenderer.renderTexture(image);
        }

        @Override
        public void onResolutionUpdate(Size size) {
            mSize = size;
            setWindowSurface();
            mRenderer.setInput(size);
        }

        @Override
        public void onImageFormatUpdate(int imageFormat) {

        }
    };

    @Override
    public void onInit(String cameraId, CameraCharacteristics cameraCharacteristics,
            Context context) {
        mRenderer = new GLImage2SurfaceRenderer();
    }

    @Override
    public void onDeInit() {
        mRenderer.close();
        mRenderer = null;
    }

    @Override
    public CaptureStageImpl onPresetSession() {
        return null;
    }

    @Override
    public CaptureStageImpl onEnableSession() {
        return null;
    }

    @Override
    public CaptureStageImpl onDisableSession() {
        return null;
    }
}
