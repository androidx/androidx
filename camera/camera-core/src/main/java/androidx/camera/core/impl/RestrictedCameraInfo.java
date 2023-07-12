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
import android.util.Rational;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ExposureState;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.TorchState;
import androidx.camera.core.ZoomState;
import androidx.camera.core.internal.ImmutableZoomState;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * A {@link CameraInfoInternal} that returns disabled state if the corresponding operation in the
 * given {@link RestrictedCameraControl} is disabled.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class RestrictedCameraInfo extends ForwardingCameraInfo {
    private final CameraInfoInternal mCameraInfo;
    private final RestrictedCameraControl mRestrictedCameraControl;

    public RestrictedCameraInfo(@NonNull CameraInfoInternal cameraInfo,
            @NonNull RestrictedCameraControl restrictedCameraControl) {
        super(cameraInfo);
        mCameraInfo = cameraInfo;
        mRestrictedCameraControl = restrictedCameraControl;
    }

    @NonNull
    @Override
    public CameraInfoInternal getImplementation() {
        return mCameraInfo;
    }

    @Override
    public boolean hasFlashUnit() {
        if (!mRestrictedCameraControl.isOperationSupported(RestrictedCameraControl.FLASH)) {
            return false;
        }

        return mCameraInfo.hasFlashUnit();
    }

    @NonNull
    @Override
    public LiveData<Integer> getTorchState() {
        if (!mRestrictedCameraControl.isOperationSupported(RestrictedCameraControl.TORCH)) {
            return new MutableLiveData<>(TorchState.OFF);
        }

        return mCameraInfo.getTorchState();
    }

    @NonNull
    @Override
    public LiveData<ZoomState> getZoomState() {
        if (!mRestrictedCameraControl.isOperationSupported(RestrictedCameraControl.ZOOM)) {
            return new MutableLiveData<>(ImmutableZoomState.create(
                    /* zoomRatio */1f, /* maxZoomRatio */ 1f,
                    /* minZoomRatio */ 1f, /* linearZoom*/ 0f));
        }
        return mCameraInfo.getZoomState();
    }

    @NonNull
    @Override
    public ExposureState getExposureState() {
        if (!mRestrictedCameraControl.isOperationSupported(
                RestrictedCameraControl.EXPOSURE_COMPENSATION)) {
            return new ExposureState() {
                @Override
                public int getExposureCompensationIndex() {
                    return 0;
                }

                @NonNull
                @Override
                public Range<Integer> getExposureCompensationRange() {
                    return new Range<>(0, 0);
                }

                @NonNull
                @Override
                public Rational getExposureCompensationStep() {
                    return Rational.ZERO;
                }

                @Override
                public boolean isExposureCompensationSupported() {
                    return false;
                }
            };
        }
        return mCameraInfo.getExposureState();
    }

    @Override
    public boolean isFocusMeteringSupported(@NonNull FocusMeteringAction action) {
        if (mRestrictedCameraControl.getModifiedFocusMeteringAction(action) == null) {
            return false;
        }
        return mCameraInfo.isFocusMeteringSupported(action);
    }
}
