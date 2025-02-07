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
import android.util.Size;

import androidx.annotation.IntRange;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraState;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ExperimentalZeroShutterLag;
import androidx.camera.core.ExposureState;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ZoomState;
import androidx.lifecycle.LiveData;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * A {@link CameraInfoInternal} that forwards all the calls into the given
 * {@link CameraInfoInternal}.
 */
public class ForwardingCameraInfo implements CameraInfoInternal {

    private final CameraInfoInternal mCameraInfoInternal;

    /**
     * Create an instance that will forward all calls to the supplied {@link CameraInfoInternal}
     * instance.
     */
    public ForwardingCameraInfo(@NonNull CameraInfoInternal cameraInfoInternal) {
        mCameraInfoInternal = cameraInfoInternal;
    }

    @Override
    public int getSensorRotationDegrees() {
        return mCameraInfoInternal.getSensorRotationDegrees();
    }

    @Override
    public int getSensorRotationDegrees(int relativeRotation) {
        return mCameraInfoInternal.getSensorRotationDegrees(relativeRotation);
    }

    @Override
    public boolean hasFlashUnit() {
        return mCameraInfoInternal.hasFlashUnit();
    }

    @Override
    public @NonNull LiveData<Integer> getTorchState() {
        return mCameraInfoInternal.getTorchState();
    }

    @Override
    public @NonNull LiveData<Integer> getTorchStrengthLevel() {
        return mCameraInfoInternal.getTorchStrengthLevel();
    }

    @Override
    @IntRange(from = 0)
    public int getMaxTorchStrengthLevel() {
        return mCameraInfoInternal.getMaxTorchStrengthLevel();
    }

    @Override
    public boolean isTorchStrengthSupported() {
        return mCameraInfoInternal.isTorchStrengthSupported();
    }

    @Override
    public boolean isLowLightBoostSupported() {
        return mCameraInfoInternal.isLowLightBoostSupported();
    }

    @Override
    public @NonNull LiveData<Integer> getLowLightBoostState() {
        return mCameraInfoInternal.getLowLightBoostState();
    }

    @Override
    public @NonNull LiveData<ZoomState> getZoomState() {
        return mCameraInfoInternal.getZoomState();
    }

    @Override
    public @NonNull ExposureState getExposureState() {
        return mCameraInfoInternal.getExposureState();
    }

    @Override
    public @NonNull LiveData<CameraState> getCameraState() {
        return mCameraInfoInternal.getCameraState();
    }

    @Override
    public @NonNull String getImplementationType() {
        return mCameraInfoInternal.getImplementationType();
    }

    @Override
    public int getLensFacing() {
        return mCameraInfoInternal.getLensFacing();
    }

    @Override
    public float getIntrinsicZoomRatio() {
        return mCameraInfoInternal.getIntrinsicZoomRatio();
    }

    @Override
    public boolean isFocusMeteringSupported(@NonNull FocusMeteringAction action) {
        return mCameraInfoInternal.isFocusMeteringSupported(action);
    }

    @Override
    @ExperimentalZeroShutterLag
    public boolean isZslSupported() {
        return mCameraInfoInternal.isZslSupported();
    }

    @Override
    public @NonNull Set<Range<Integer>> getSupportedFrameRateRanges() {
        return mCameraInfoInternal.getSupportedFrameRateRanges();
    }

    @Override
    public boolean isPrivateReprocessingSupported() {
        return mCameraInfoInternal.isPrivateReprocessingSupported();
    }

    @Override
    public boolean isLogicalMultiCameraSupported() {
        return mCameraInfoInternal.isLogicalMultiCameraSupported();
    }

    @Override
    public @NonNull String getCameraId() {
        return mCameraInfoInternal.getCameraId();
    }

    @Override
    public void addSessionCaptureCallback(@NonNull Executor executor,
            @NonNull CameraCaptureCallback callback) {
        mCameraInfoInternal.addSessionCaptureCallback(executor, callback);
    }

    @Override
    public void removeSessionCaptureCallback(@NonNull CameraCaptureCallback callback) {
        mCameraInfoInternal.removeSessionCaptureCallback(callback);
    }

    @Override
    public @NonNull Quirks getCameraQuirks() {
        return mCameraInfoInternal.getCameraQuirks();
    }

    @Override
    public @NonNull EncoderProfilesProvider getEncoderProfilesProvider() {
        return mCameraInfoInternal.getEncoderProfilesProvider();
    }

    @Override
    public @NonNull Timebase getTimebase() {
        return mCameraInfoInternal.getTimebase();
    }

    @Override
    public @NonNull Set<Integer> getSupportedOutputFormats() {
        return mCameraInfoInternal.getSupportedOutputFormats();
    }

    @Override
    public @NonNull List<Size> getSupportedResolutions(int format) {
        return mCameraInfoInternal.getSupportedResolutions(format);
    }

    @Override
    public @NonNull List<Size> getSupportedHighResolutions(int format) {
        return mCameraInfoInternal.getSupportedHighResolutions(format);
    }

    @Override
    public @NonNull Set<DynamicRange> getSupportedDynamicRanges() {
        return mCameraInfoInternal.getSupportedDynamicRanges();
    }

    @Override
    public boolean isHighSpeedSupported() {
        return mCameraInfoInternal.isHighSpeedSupported();
    }

    @Override
    public @NonNull Set<Range<Integer>> getSupportedHighSpeedFrameRateRanges() {
        return mCameraInfoInternal.getSupportedHighSpeedFrameRateRanges();
    }

    @Override
    public @NonNull Set<Range<Integer>> getSupportedHighSpeedFrameRateRangesFor(@NonNull Size size) {
        return mCameraInfoInternal.getSupportedHighSpeedFrameRateRangesFor(size);
    }

    @Override
    public @NonNull List<Size> getSupportedHighSpeedResolutions() {
        return mCameraInfoInternal.getSupportedHighSpeedResolutions();
    }

    @Override
    public @NonNull List<Size> getSupportedHighSpeedResolutionsFor(@NonNull Range<Integer> fpsRange) {
        return mCameraInfoInternal.getSupportedHighSpeedResolutionsFor(fpsRange);
    }

    @Override
    public @NonNull Set<DynamicRange> querySupportedDynamicRanges(
            @NonNull Set<DynamicRange> candidateDynamicRanges) {
        return mCameraInfoInternal.querySupportedDynamicRanges(candidateDynamicRanges);
    }

    @Override
    public @NonNull CameraInfoInternal getImplementation() {
        return mCameraInfoInternal.getImplementation();
    }

    @Override
    public @NonNull CameraSelector getCameraSelector() {
        return mCameraInfoInternal.getCameraSelector();
    }

    @Override
    public boolean isPreviewStabilizationSupported() {
        return mCameraInfoInternal.isPreviewStabilizationSupported();
    }

    @Override
    public boolean isVideoStabilizationSupported() {
        return mCameraInfoInternal.isVideoStabilizationSupported();
    }

    @Override
    public @NonNull Object getCameraCharacteristics() {
        return mCameraInfoInternal.getCameraCharacteristics();
    }

    @Override
    public @Nullable Object getPhysicalCameraCharacteristics(@NonNull String physicalCameraId) {
        return mCameraInfoInternal.getPhysicalCameraCharacteristics(physicalCameraId);
    }

    @Override
    public @NonNull Set<CameraInfo> getPhysicalCameraInfos() {
        return mCameraInfoInternal.getPhysicalCameraInfos();
    }
}
