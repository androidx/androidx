/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.internal;

import android.annotation.SuppressLint;
import android.util.Range;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraState;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ExperimentalZeroShutterLag;
import androidx.camera.core.ExposureState;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ZoomState;
import androidx.lifecycle.LiveData;

import java.util.Set;

@ExperimentalCamera2Interop
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class Camera2PhysicalCameraInfoImpl implements CameraInfo {

    @NonNull private final String mCameraId;
    @NonNull private final CameraCharacteristicsCompat mCameraCharacteristicsCompat;
    @NonNull private final Camera2CameraInfo mCamera2CameraInfo;

    public Camera2PhysicalCameraInfoImpl(@NonNull String cameraId,
            @NonNull CameraManagerCompat cameraManager) throws CameraAccessExceptionCompat {
        mCameraId = cameraId;
        mCameraCharacteristicsCompat = cameraManager.getCameraCharacteristicsCompat(mCameraId);
        mCamera2CameraInfo = new Camera2CameraInfo(this);
    }

    /**
     * Gets the implementation of {@link Camera2CameraInfo}.
     */
    @NonNull
    public Camera2CameraInfo getCamera2CameraInfo() {
        return mCamera2CameraInfo;
    }

    @NonNull
    public String getCameraId() {
        return mCameraId;
    }

    @NonNull
    public CameraCharacteristicsCompat getCameraCharacteristicsCompat() {
        return mCameraCharacteristicsCompat;
    }

    @Override
    public int getSensorRotationDegrees() {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @Override
    public int getSensorRotationDegrees(int relativeRotation) {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @Override
    public boolean hasFlashUnit() {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @NonNull
    @Override
    public LiveData<Integer> getTorchState() {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @NonNull
    @Override
    public LiveData<ZoomState> getZoomState() {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @NonNull
    @Override
    public ExposureState getExposureState() {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @NonNull
    @Override
    public LiveData<CameraState> getCameraState() {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @NonNull
    @Override
    public String getImplementationType() {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @NonNull
    @Override
    public CameraSelector getCameraSelector() {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @Override
    public int getLensFacing() {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @Override
    public float getIntrinsicZoomRatio() {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @Override
    public boolean isFocusMeteringSupported(@NonNull FocusMeteringAction action) {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @SuppressLint("NullAnnotationGroup")
    @ExperimentalZeroShutterLag
    @Override
    public boolean isZslSupported() {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @NonNull
    @Override
    public Set<Range<Integer>> getSupportedFrameRateRanges() {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @Override
    public boolean isLogicalMultiCameraSupported() {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @Override
    public boolean isPrivateReprocessingSupported() {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @NonNull
    @Override
    public Set<DynamicRange> querySupportedDynamicRanges(
            @NonNull Set<DynamicRange> candidateDynamicRanges) {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }

    @NonNull
    @Override
    public Set<CameraInfo> getPhysicalCameraInfos() {
        throw new UnsupportedOperationException("Physical camera doesn't support this function");
    }
}
