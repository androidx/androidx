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

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/**
 * A {@link CameraControlInternal} that forwards all the calls into the given
 * {@link CameraControlInternal}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ForwardingCameraControl implements CameraControlInternal {
    private final CameraControlInternal mCameraControlInternal;

    /**
     * Create an instance that will forward all calls to the supplied {@link CameraControlInternal}
     * instance.
     */
    public ForwardingCameraControl(@NonNull CameraControlInternal cameraControlInternal) {
        mCameraControlInternal = cameraControlInternal;
    }

    @NonNull
    @Override
    public ListenableFuture<Void> enableTorch(boolean torch) {
        return mCameraControlInternal.enableTorch(torch);
    }

    @NonNull
    @Override
    public ListenableFuture<FocusMeteringResult> startFocusAndMetering(
            @NonNull FocusMeteringAction action) {
        return mCameraControlInternal.startFocusAndMetering(action);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> cancelFocusAndMetering() {
        return mCameraControlInternal.cancelFocusAndMetering();
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setZoomRatio(float ratio) {
        return mCameraControlInternal.setZoomRatio(ratio);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setLinearZoom(float linearZoom) {
        return mCameraControlInternal.setLinearZoom(linearZoom);
    }

    @NonNull
    @Override
    public ListenableFuture<Integer> setExposureCompensationIndex(int value) {
        return mCameraControlInternal.setExposureCompensationIndex(value);
    }

    @Override
    @ImageCapture.FlashMode
    public int getFlashMode() {
        return mCameraControlInternal.getFlashMode();
    }

    @Override
    public void setFlashMode(@ImageCapture.FlashMode int flashMode) {
        mCameraControlInternal.setFlashMode(flashMode);
    }

    @Override
    public void addZslConfig(@NonNull SessionConfig.Builder sessionConfigBuilder) {
        mCameraControlInternal.addZslConfig(sessionConfigBuilder);
    }

    @Override
    public void setZslDisabledByUserCaseConfig(boolean disabled) {
        mCameraControlInternal.setZslDisabledByUserCaseConfig(disabled);
    }

    @Override
    public boolean isZslDisabledByByUserCaseConfig() {
        return mCameraControlInternal.isZslDisabledByByUserCaseConfig();
    }

    @NonNull
    @Override
    public ListenableFuture<List<Void>> submitStillCaptureRequests(
            @NonNull List<CaptureConfig> captureConfigs,
            @ImageCapture.CaptureMode int captureMode,
            @ImageCapture.FlashType int flashType) {
        return mCameraControlInternal.submitStillCaptureRequests(
                captureConfigs,
                captureMode,
                flashType);
    }

    @NonNull
    @Override
    public SessionConfig getSessionConfig() {
        return mCameraControlInternal.getSessionConfig();
    }

    @NonNull
    @Override
    public Rect getSensorRect() {
        return mCameraControlInternal.getSensorRect();
    }

    @Override
    public void addInteropConfig(@NonNull Config config) {
        mCameraControlInternal.addInteropConfig(config);
    }

    @Override
    public void clearInteropConfig() {
        mCameraControlInternal.clearInteropConfig();
    }

    @NonNull
    @Override
    public Config getInteropConfig() {
        return mCameraControlInternal.getInteropConfig();
    }

    @NonNull
    @Override
    public CameraControlInternal getImplementation() {
        return mCameraControlInternal.getImplementation();
    }
}
