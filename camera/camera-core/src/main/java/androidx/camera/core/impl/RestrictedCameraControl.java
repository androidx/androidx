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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.impl.utils.SessionProcessorUtil;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * A {@link CameraControlInternal} whose capabilities can be restricted by the associated
 * {@link SessionProcessor}. Only the camera operations that can be retrieved from
 * {@link SessionProcessor#getSupportedCameraOperations()} can be supported by the
 * RestrictedCameraControl.
 */
public class RestrictedCameraControl extends ForwardingCameraControl {
    private final CameraControlInternal mCameraControl;
    @Nullable
    private final SessionProcessor mSessionProcessor;

    /**
     * Creates the restricted version of the given {@link CameraControlInternal}.
     */
    public RestrictedCameraControl(@NonNull CameraControlInternal cameraControl,
            @Nullable SessionProcessor sessionProcessor) {
        super(cameraControl);
        mCameraControl = cameraControl;
        mSessionProcessor = sessionProcessor;
    }

    /**
     * Returns implementation instance.
     */
    @NonNull
    @Override
    public CameraControlInternal getImplementation() {
        return mCameraControl;
    }

    /**
     * Returns the {@link SessionProcessor} associated with the RestrictedCameraControl.
     */
    @Nullable
    public SessionProcessor getSessionProcessor() {
        return mSessionProcessor;
    }

    @NonNull
    @Override
    public ListenableFuture<Void> enableTorch(boolean torch) {
        if (!SessionProcessorUtil.isOperationSupported(mSessionProcessor,
                RestrictedCameraInfo.CAMERA_OPERATION_TORCH)) {
            return Futures.immediateFailedFuture(
                    new IllegalStateException("Torch is not supported"));
        }
        return mCameraControl.enableTorch(torch);
    }

    @NonNull
    @Override
    public ListenableFuture<FocusMeteringResult> startFocusAndMetering(
            @NonNull FocusMeteringAction action) {
        FocusMeteringAction modifiedAction =
                SessionProcessorUtil.getModifiedFocusMeteringAction(mSessionProcessor, action);
        if (modifiedAction == null) {
            return Futures.immediateFailedFuture(
                    new IllegalStateException("FocusMetering is not supported"));
        }

        return mCameraControl.startFocusAndMetering(modifiedAction);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> cancelFocusAndMetering() {
        return mCameraControl.cancelFocusAndMetering();
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setZoomRatio(float ratio) {
        if (!SessionProcessorUtil.isOperationSupported(mSessionProcessor,
                RestrictedCameraInfo.CAMERA_OPERATION_ZOOM)) {
            return Futures.immediateFailedFuture(
                    new IllegalStateException("Zoom is not supported"));
        }
        return mCameraControl.setZoomRatio(ratio);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setLinearZoom(float linearZoom) {
        if (!SessionProcessorUtil.isOperationSupported(mSessionProcessor,
                RestrictedCameraInfo.CAMERA_OPERATION_ZOOM)) {
            return Futures.immediateFailedFuture(
                    new IllegalStateException("Zoom is not supported"));
        }
        return mCameraControl.setLinearZoom(linearZoom);
    }

    @NonNull
    @Override
    public ListenableFuture<Integer> setExposureCompensationIndex(int value) {
        if (!SessionProcessorUtil.isOperationSupported(mSessionProcessor,
                RestrictedCameraInfo.CAMERA_OPERATION_EXPOSURE_COMPENSATION)) {
            return Futures.immediateFailedFuture(
                    new IllegalStateException("ExposureCompensation is not supported"));
        }
        return mCameraControl.setExposureCompensationIndex(value);
    }
}
