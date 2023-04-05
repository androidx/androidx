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
package androidx.camera.core.streamsharing;

import static androidx.core.util.Preconditions.checkArgument;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import android.graphics.Rect;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/**
 * A {@link CameraControlInternal} that is used to control the virtual camera.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class VirtualCameraControl implements CameraControlInternal {

    private static final int DEFAULT_JPEG_QUALITY = 100;

    private final CameraControlInternal mParent;
    private final StreamSharing.Control mStreamSharingControl;

    VirtualCameraControl(@NonNull CameraControlInternal parent,
            @NonNull StreamSharing.Control streamSharingControl) {
        mParent = parent;
        mStreamSharingControl = streamSharingControl;
    }

    @NonNull
    @Override
    public ListenableFuture<Void> enableTorch(boolean torch) {
        return mParent.enableTorch(torch);
    }

    @NonNull
    @Override
    public ListenableFuture<FocusMeteringResult> startFocusAndMetering(
            @NonNull FocusMeteringAction action) {
        return mParent.startFocusAndMetering(action);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> cancelFocusAndMetering() {
        return mParent.cancelFocusAndMetering();
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setZoomRatio(float ratio) {
        return mParent.setZoomRatio(ratio);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setLinearZoom(float linearZoom) {
        return mParent.setLinearZoom(linearZoom);
    }

    @NonNull
    @Override
    public ListenableFuture<Integer> setExposureCompensationIndex(int value) {
        return mParent.setExposureCompensationIndex(value);
    }

    @Override
    public int getFlashMode() {
        return mParent.getFlashMode();
    }

    @Override
    public void setFlashMode(int flashMode) {
        mParent.setFlashMode(flashMode);
    }

    @Override
    public void addZslConfig(@NonNull SessionConfig.Builder sessionConfigBuilder) {
        mParent.addZslConfig(sessionConfigBuilder);
    }

    @Override
    public void setZslDisabledByUserCaseConfig(boolean disabled) {
        mParent.setZslDisabledByUserCaseConfig(disabled);
    }

    @Override
    public boolean isZslDisabledByByUserCaseConfig() {
        return mParent.isZslDisabledByByUserCaseConfig();
    }

    @NonNull
    @Override
    public ListenableFuture<List<Void>> submitStillCaptureRequests(
            @NonNull List<CaptureConfig> captureConfigs,
            @ImageCapture.CaptureMode int captureMode,
            @ImageCapture.FlashType int flashType) {
        checkArgument(captureConfigs.size() == 1, "Only support one capture config.");
        int jpegQuality = getJpegQuality(captureConfigs.get(0));
        return Futures.allAsList(singletonList(mStreamSharingControl.jpegSnapshot(jpegQuality)));
    }

    private int getJpegQuality(@NonNull CaptureConfig captureConfig) {
        return requireNonNull(captureConfig.getImplementationOptions().retrieveOption(
                CaptureConfig.OPTION_JPEG_QUALITY, DEFAULT_JPEG_QUALITY));
    }

    @NonNull
    @Override
    public SessionConfig getSessionConfig() {
        return mParent.getSessionConfig();
    }

    @NonNull
    @Override
    public Rect getSensorRect() {
        return mParent.getSensorRect();
    }

    @Override
    public void addInteropConfig(@NonNull Config config) {
        mParent.addInteropConfig(config);
    }

    @Override
    public void clearInteropConfig() {
        mParent.clearInteropConfig();
    }

    @NonNull
    @Override
    public Config getInteropConfig() {
        return mParent.getInteropConfig();
    }
}
