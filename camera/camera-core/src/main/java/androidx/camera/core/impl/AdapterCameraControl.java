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

package androidx.camera.core.impl;

import android.util.Range;

import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.utils.SessionProcessorUtil;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A {@link CameraControlInternal} whose capabilities can be restricted by the associated
 * {@link SessionProcessor}. Only the camera operations that can be retrieved from
 * {@link SessionProcessor#getSupportedCameraOperations()} can be supported by the
 * AdapterCameraControl.
 */
public class AdapterCameraControl extends ForwardingCameraControl {
    private final CameraControlInternal mCameraControl;
    private final @Nullable SessionProcessor mSessionProcessor;
    private volatile @Nullable EnabledUseCasesHandler mEnabledUseCasesHandler;

    /**
     * Handler for session-scoped dynamic {@link UseCase} switching and capture request gating.
     */
    public interface EnabledUseCasesHandler {
        /**
         * Validates and applies the active subset of {@link UseCase}s for the bound session.
         */
        @NonNull ListenableFuture<Void> setEnabledUseCasesAsync(@NonNull List<UseCase> useCases);

        /**
         * Returns whether the given still capture requests target currently enabled
         * {@link UseCase}s.
         */
        boolean areCaptureRequestsEnabled(@NonNull List<CaptureConfig> captureConfigs);
    }

    /**
     * Creates the restricted version of the given {@link CameraControlInternal}.
     */
    public AdapterCameraControl(@NonNull CameraControlInternal cameraControl,
            @Nullable SessionProcessor sessionProcessor) {
        super(cameraControl);
        mCameraControl = cameraControl;
        mSessionProcessor = sessionProcessor;
    }

    /**
     * Returns implementation instance.
     */
    @Override
    public @NonNull CameraControlInternal getImplementation() {
        return mCameraControl;
    }

    /**
     * Returns the {@link SessionProcessor} associated with the AdapterCameraControl.
     */
    public @Nullable SessionProcessor getSessionProcessor() {
        return mSessionProcessor;
    }

    @Override
    public @NonNull ListenableFuture<Void> enableTorch(boolean torch) {
        if (!SessionProcessorUtil.isOperationSupported(mSessionProcessor,
                AdapterCameraInfo.CAMERA_OPERATION_TORCH)) {
            return Futures.immediateFailedFuture(
                    new IllegalStateException("Torch is not supported"));
        }
        return mCameraControl.enableTorch(torch);
    }

    @Override
    public @NonNull ListenableFuture<FocusMeteringResult> startFocusAndMetering(
            @NonNull FocusMeteringAction action) {
        FocusMeteringAction modifiedAction =
                SessionProcessorUtil.getModifiedFocusMeteringAction(mSessionProcessor, action);
        if (modifiedAction == null) {
            return Futures.immediateFailedFuture(
                    new IllegalStateException("FocusMetering is not supported"));
        }

        return mCameraControl.startFocusAndMetering(modifiedAction);
    }

    @Override
    public @NonNull ListenableFuture<Void> cancelFocusAndMetering() {
        return mCameraControl.cancelFocusAndMetering();
    }

    @Override
    public @NonNull ListenableFuture<Void> setZoomRatio(float ratio) {
        if (!SessionProcessorUtil.isOperationSupported(mSessionProcessor,
                AdapterCameraInfo.CAMERA_OPERATION_ZOOM)) {
            return Futures.immediateFailedFuture(
                    new IllegalStateException("Zoom is not supported"));
        }

        if (mSessionProcessor != null) {
            Range<Float> extensionZoomRange = mSessionProcessor.getExtensionZoomRange();
            if (extensionZoomRange != null
                    && (ratio < extensionZoomRange.getLower()
                    || ratio > extensionZoomRange.getUpper())) {
                String outOfRangeDesc = "Requested zoomRatio " + ratio + " is not within valid "
                        + "range [" + extensionZoomRange.getLower() + " , "
                        + extensionZoomRange.getUpper() + "]";
                return Futures.immediateFailedFuture(new IllegalArgumentException(outOfRangeDesc));
            }
        }
        return mCameraControl.setZoomRatio(ratio);
    }

    @Override
    public @NonNull ListenableFuture<Void> setLinearZoom(float linearZoom) {
        if (!SessionProcessorUtil.isOperationSupported(mSessionProcessor,
                AdapterCameraInfo.CAMERA_OPERATION_ZOOM)) {
            return Futures.immediateFailedFuture(
                    new IllegalStateException("Zoom is not supported"));
        }

        if (mSessionProcessor != null) {
            Range<Float> extensionZoomRange = mSessionProcessor.getExtensionZoomRange();
            if (extensionZoomRange == null) {
                return mCameraControl.setLinearZoom(linearZoom);
            }

            if (linearZoom > 1.0f || linearZoom < 0f) {
                String outOfRangeDesc = "Requested linearZoom " + linearZoom + " is not within"
                        + " valid range [0..1]";
                return Futures.immediateFailedFuture(new IllegalArgumentException(outOfRangeDesc));
            }
            float zoomRatio = AdapterCameraInfo.getZoomRatioByPercentage(linearZoom,
                    extensionZoomRange.getLower(), extensionZoomRange.getUpper());
            return mCameraControl.setZoomRatio(zoomRatio);
        }

        return mCameraControl.setLinearZoom(linearZoom);
    }

    @Override
    public @NonNull ListenableFuture<Integer> setExposureCompensationIndex(int value) {
        if (!SessionProcessorUtil.isOperationSupported(mSessionProcessor,
                AdapterCameraInfo.CAMERA_OPERATION_EXPOSURE_COMPENSATION)) {
            return Futures.immediateFailedFuture(
                    new IllegalStateException("ExposureCompensation is not supported"));
        }
        return mCameraControl.setExposureCompensationIndex(value);
    }

    /**
     * Sets the handler used for session-scoped dynamic {@link UseCase} switching and capture
     * request gating.
     */
    public void setEnabledUseCasesHandler(@Nullable EnabledUseCasesHandler handler) {
        mEnabledUseCasesHandler = handler;
    }

    @Override
    public @NonNull ListenableFuture<Void> setEnabledUseCasesAsync(
            @NonNull List<UseCase> useCases) {
        EnabledUseCasesHandler handler = mEnabledUseCasesHandler;
        if (handler != null) {
            return handler.setEnabledUseCasesAsync(useCases);
        }
        return mCameraControl.setEnabledUseCasesAsync(useCases);
    }

    /**
     * Returns whether all {@code captureConfigs} target surfaces of {@link UseCase}s that are
     * currently enabled.
     */
    public boolean areCaptureRequestsEnabled(@NonNull List<CaptureConfig> captureConfigs) {
        EnabledUseCasesHandler handler = mEnabledUseCasesHandler;
        return handler == null || handler.areCaptureRequestsEnabled(captureConfigs);
    }

    @Override
    public @NonNull ListenableFuture<List<Void>> submitStillCaptureRequests(
            @NonNull List<CaptureConfig> captureConfigs,
            @ImageCapture.CaptureMode int captureMode,
            @ImageCapture.FlashType int flashType) {
        if (!areCaptureRequestsEnabled(captureConfigs)) {
            return Futures.immediateFailedFuture(
                    new ImageCaptureException(
                            ImageCapture.ERROR_INVALID_CAMERA,
                            "UseCase is not enabled in the current SessionConfig",
                            null));
        }
        return mCameraControl.submitStillCaptureRequests(captureConfigs, captureMode, flashType);
    }
}
