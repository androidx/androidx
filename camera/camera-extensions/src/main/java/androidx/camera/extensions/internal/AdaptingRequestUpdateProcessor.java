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

package androidx.camera.extensions.internal;

import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.impl.Camera2CameraCaptureResultConverter;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraCaptureResults;
import androidx.camera.core.impl.CaptureStage;
import androidx.camera.core.impl.ImageInfoProcessor;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
import androidx.camera.extensions.impl.RequestUpdateProcessorImpl;
import androidx.core.util.Preconditions;

/**
 * A {@link ImageInfoProcessor} that calls a vendor provided preview processing implementation.
 */
public final class AdaptingRequestUpdateProcessor implements ImageInfoProcessor,
        CloseableProcessor {
    private final PreviewExtenderImpl mPreviewExtenderImpl;
    private final RequestUpdateProcessorImpl mProcessorImpl;
    private BlockingCloseAccessCounter mAccessCounter = new BlockingCloseAccessCounter();

    public AdaptingRequestUpdateProcessor(@NonNull PreviewExtenderImpl previewExtenderImpl) {
        Preconditions.checkArgument(previewExtenderImpl.getProcessorType()
                        == PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_REQUEST_UPDATE_ONLY,
                "AdaptingRequestUpdateProcess can only adapt extender with "
                        + "PROCESSOR_TYPE_REQUEST_UPDATE_ONLY ProcessorType.");
        mPreviewExtenderImpl = previewExtenderImpl;
        mProcessorImpl = (RequestUpdateProcessorImpl) mPreviewExtenderImpl.getProcessor();
    }

    @Override
    @Nullable
    public CaptureStage getCaptureStage() {
        if (!mAccessCounter.tryIncrement()) {
            return null;
        }

        try {
            return new AdaptingCaptureStage(mPreviewExtenderImpl.getCaptureStage());
        } finally {
            mAccessCounter.decrement();
        }

    }

    @Override
    public boolean process(@NonNull ImageInfo imageInfo) {
        if (!mAccessCounter.tryIncrement()) {
            return false;
        }

        try {
            boolean processResult = false;

            CameraCaptureResult result = CameraCaptureResults.retrieveCameraCaptureResult(
                    imageInfo);
            CaptureResult captureResult = Camera2CameraCaptureResultConverter.getCaptureResult(
                    result);

            if (captureResult instanceof TotalCaptureResult) {

                CaptureStageImpl captureStageImpl =
                        mProcessorImpl.process((TotalCaptureResult) captureResult);
                processResult = captureStageImpl != null;
            }
            return processResult;
        } finally {
            mAccessCounter.decrement();
        }
    }

    @Override
    public void close() {
        mAccessCounter.destroyAndWaitForZeroAccess();
    }
}
