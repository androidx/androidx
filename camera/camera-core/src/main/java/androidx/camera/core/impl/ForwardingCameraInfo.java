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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraState;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ExperimentalZeroShutterLag;
import androidx.camera.core.ExposureState;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ZoomState;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * A {@link CameraInfoInternal} that forwards all the calls into the given
 * {@link CameraInfoInternal}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
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

    @NonNull
    @Override
    public LiveData<Integer> getTorchState() {
        return mCameraInfoInternal.getTorchState();
    }

    @NonNull
    @Override
    public LiveData<ZoomState> getZoomState() {
        return mCameraInfoInternal.getZoomState();
    }

    @NonNull
    @Override
    public ExposureState getExposureState() {
        return mCameraInfoInternal.getExposureState();
    }

    @NonNull
    @Override
    public LiveData<CameraState> getCameraState() {
        return mCameraInfoInternal.getCameraState();
    }

    @NonNull
    @Override
    public String getImplementationType() {
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

    @NonNull
    @Override
    public Set<Range<Integer>> getSupportedFrameRateRanges() {
        return mCameraInfoInternal.getSupportedFrameRateRanges();
    }

    @Override
    public boolean isPrivateReprocessingSupported() {
        return mCameraInfoInternal.isPrivateReprocessingSupported();
    }

    @NonNull
    @Override
    public String getCameraId() {
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

    @NonNull
    @Override
    public Quirks getCameraQuirks() {
        return mCameraInfoInternal.getCameraQuirks();
    }

    @NonNull
    @Override
    public EncoderProfilesProvider getEncoderProfilesProvider() {
        return mCameraInfoInternal.getEncoderProfilesProvider();
    }

    @NonNull
    @Override
    public Timebase getTimebase() {
        return mCameraInfoInternal.getTimebase();
    }

    @NonNull
    @Override
    public List<Size> getSupportedResolutions(int format) {
        return mCameraInfoInternal.getSupportedResolutions(format);
    }

    @NonNull
    @Override
    public List<Size> getSupportedHighResolutions(int format) {
        return mCameraInfoInternal.getSupportedHighResolutions(format);
    }

    @NonNull
    @Override
    public Set<DynamicRange> getSupportedDynamicRanges() {
        return mCameraInfoInternal.getSupportedDynamicRanges();
    }

    @NonNull
    @Override
    public CameraInfoInternal getImplementation() {
        return mCameraInfoInternal.getImplementation();
    }

    @NonNull
    @Override
    public CameraSelector getCameraSelector() {
        return mCameraInfoInternal.getCameraSelector();
    }
}
