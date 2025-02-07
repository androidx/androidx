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

import androidx.annotation.IntRange;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.imagecapture.CameraCapturePipeline;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A {@link CameraControlInternal} that forwards all the calls into the given
 * {@link CameraControlInternal}.
 */
public class ForwardingCameraControl implements CameraControlInternal {
    private final CameraControlInternal mCameraControlInternal;

    /**
     * Create an instance that will forward all calls to the supplied {@link CameraControlInternal}
     * instance.
     */
    public ForwardingCameraControl(@NonNull CameraControlInternal cameraControlInternal) {
        mCameraControlInternal = cameraControlInternal;
    }

    @Override
    public @NonNull ListenableFuture<Void> enableTorch(boolean torch) {
        return mCameraControlInternal.enableTorch(torch);
    }

    @Override
    public @NonNull ListenableFuture<Void> enableLowLightBoostAsync(boolean lowLightBoost) {
        return mCameraControlInternal.enableLowLightBoostAsync(lowLightBoost);
    }

    @Override
    public @NonNull ListenableFuture<FocusMeteringResult> startFocusAndMetering(
            @NonNull FocusMeteringAction action) {
        return mCameraControlInternal.startFocusAndMetering(action);
    }

    @Override
    public @NonNull ListenableFuture<Void> cancelFocusAndMetering() {
        return mCameraControlInternal.cancelFocusAndMetering();
    }

    @Override
    public @NonNull ListenableFuture<Void> setZoomRatio(float ratio) {
        return mCameraControlInternal.setZoomRatio(ratio);
    }

    @Override
    public @NonNull ListenableFuture<Void> setLinearZoom(float linearZoom) {
        return mCameraControlInternal.setLinearZoom(linearZoom);
    }

    @Override
    public @NonNull ListenableFuture<Integer> setExposureCompensationIndex(int value) {
        return mCameraControlInternal.setExposureCompensationIndex(value);
    }

    @Override
    public @NonNull ListenableFuture<Void> setTorchStrengthLevel(
            @IntRange(from = 1) int torchStrengthLevel) {
        return mCameraControlInternal.setTorchStrengthLevel(torchStrengthLevel);
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
    public void setScreenFlash(ImageCapture.@Nullable ScreenFlash screenFlash) {
        mCameraControlInternal.setScreenFlash(screenFlash);
    }

    @Override
    public void addZslConfig(SessionConfig.@NonNull Builder sessionConfigBuilder) {
        mCameraControlInternal.addZslConfig(sessionConfigBuilder);
    }

    @Override
    public void clearZslConfig() {
        mCameraControlInternal.clearZslConfig();
    }

    @Override
    public void setZslDisabledByUserCaseConfig(boolean disabled) {
        mCameraControlInternal.setZslDisabledByUserCaseConfig(disabled);
    }

    @Override
    public boolean isZslDisabledByByUserCaseConfig() {
        return mCameraControlInternal.isZslDisabledByByUserCaseConfig();
    }

    @Override
    public @NonNull ListenableFuture<List<Void>> submitStillCaptureRequests(
            @NonNull List<CaptureConfig> captureConfigs,
            @ImageCapture.CaptureMode int captureMode,
            @ImageCapture.FlashType int flashType) {
        return mCameraControlInternal.submitStillCaptureRequests(
                captureConfigs,
                captureMode,
                flashType);
    }

    @Override
    public @NonNull ListenableFuture<CameraCapturePipeline> getCameraCapturePipelineAsync(
            @ImageCapture.CaptureMode int captureMode, @ImageCapture.FlashType int flashType) {
        return mCameraControlInternal.getCameraCapturePipelineAsync(captureMode, flashType);
    }

    @Override
    public @NonNull SessionConfig getSessionConfig() {
        return mCameraControlInternal.getSessionConfig();
    }

    @Override
    public @NonNull Rect getSensorRect() {
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

    @Override
    public @NonNull Config getInteropConfig() {
        return mCameraControlInternal.getInteropConfig();
    }

    @Override
    public @NonNull CameraControlInternal getImplementation() {
        return mCameraControlInternal.getImplementation();
    }

    @Override
    public void incrementVideoUsage() {
        mCameraControlInternal.incrementVideoUsage();
    }

    @Override
    public void decrementVideoUsage() {
        mCameraControlInternal.decrementVideoUsage();
    }

    @VisibleForTesting
    @Override
    public boolean isInVideoUsage() {
        return mCameraControlInternal.isInVideoUsage();
    }
}
