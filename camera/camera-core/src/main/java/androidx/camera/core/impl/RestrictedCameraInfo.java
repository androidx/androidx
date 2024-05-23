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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ExposureState;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.TorchState;
import androidx.camera.core.ZoomState;
import androidx.camera.core.impl.utils.SessionProcessorUtil;
import androidx.camera.core.internal.ImmutableZoomState;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A {@link CameraInfoInternal} that returns disabled state if the corresponding operation in the
 * given {@link RestrictedCameraControl} is disabled.
 */
public class RestrictedCameraInfo extends ForwardingCameraInfo {
    /**
     * Defines the list of supported camera operations.
     */
    public static final int CAMERA_OPERATION_ZOOM = 0;
    public static final int CAMERA_OPERATION_AUTO_FOCUS = 1;
    public static final int CAMERA_OPERATION_AF_REGION = 2;
    public static final int CAMERA_OPERATION_AE_REGION = 3;
    public static final int CAMERA_OPERATION_AWB_REGION = 4;
    public static final int CAMERA_OPERATION_FLASH = 5;
    public static final int CAMERA_OPERATION_TORCH = 6;
    public static final int CAMERA_OPERATION_EXPOSURE_COMPENSATION = 7;
    public static final int CAMERA_OPERATION_EXTENSION_STRENGTH = 8;

    @IntDef({CAMERA_OPERATION_ZOOM, CAMERA_OPERATION_AUTO_FOCUS, CAMERA_OPERATION_AF_REGION,
            CAMERA_OPERATION_AE_REGION, CAMERA_OPERATION_AWB_REGION, CAMERA_OPERATION_FLASH,
            CAMERA_OPERATION_TORCH, CAMERA_OPERATION_EXPOSURE_COMPENSATION,
            CAMERA_OPERATION_EXTENSION_STRENGTH})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CameraOperation {
    }

    private final CameraInfoInternal mCameraInfo;
    @Nullable
    private final SessionProcessor mSessionProcessor;
    private boolean mIsPostviewSupported = false;
    private boolean mIsCaptureProcessProgressSupported = false;
    @NonNull
    private final CameraConfig mCameraConfig;

    public RestrictedCameraInfo(@NonNull CameraInfoInternal cameraInfo,
            @NonNull CameraConfig cameraConfig) {
        super(cameraInfo);
        mCameraInfo = cameraInfo;
        mCameraConfig = cameraConfig;
        mSessionProcessor = cameraConfig.getSessionProcessor(null);

        setPostviewSupported(cameraConfig.isPostviewSupported());
        setCaptureProcessProgressSupported(cameraConfig.isCaptureProcessProgressSupported());
    }

    @NonNull
    public CameraConfig getCameraConfig() {
        return mCameraConfig;
    }

    @NonNull
    @Override
    public CameraInfoInternal getImplementation() {
        return mCameraInfo;
    }

    /**
     * Returns the session processor associated with the RestrictedCameraInfo.
     */
    @Nullable
    public SessionProcessor getSessionProcessor() {
        return mSessionProcessor;
    }

    @Override
    public boolean hasFlashUnit() {
        if (!SessionProcessorUtil.isOperationSupported(mSessionProcessor, CAMERA_OPERATION_FLASH)) {
            return false;
        }

        return mCameraInfo.hasFlashUnit();
    }

    @NonNull
    @Override
    public LiveData<Integer> getTorchState() {
        if (!SessionProcessorUtil.isOperationSupported(mSessionProcessor, CAMERA_OPERATION_TORCH)) {
            return new MutableLiveData<>(TorchState.OFF);
        }

        return mCameraInfo.getTorchState();
    }

    @NonNull
    @Override
    public LiveData<ZoomState> getZoomState() {
        if (!SessionProcessorUtil.isOperationSupported(mSessionProcessor, CAMERA_OPERATION_ZOOM)) {
            return new MutableLiveData<>(ImmutableZoomState.create(
                    /* zoomRatio */1f, /* maxZoomRatio */ 1f,
                    /* minZoomRatio */ 1f, /* linearZoom*/ 0f));
        }
        return mCameraInfo.getZoomState();
    }

    @NonNull
    @Override
    public ExposureState getExposureState() {
        if (!SessionProcessorUtil.isOperationSupported(mSessionProcessor,
                CAMERA_OPERATION_EXPOSURE_COMPENSATION)) {
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
        FocusMeteringAction modifiedAction =
                SessionProcessorUtil.getModifiedFocusMeteringAction(mSessionProcessor, action);
        if (modifiedAction == null) {
            return false;
        }
        return mCameraInfo.isFocusMeteringSupported(modifiedAction);
    }

    /**
     * Sets if postview is supported or not.
     */
    public void setPostviewSupported(boolean isPostviewSupported) {
        mIsPostviewSupported = isPostviewSupported;
    }

    /**
     * Sets if capture process progress is supported or not.
     */
    public void setCaptureProcessProgressSupported(boolean isCaptureProcessProgressSupported) {
        mIsCaptureProcessProgressSupported = isCaptureProcessProgressSupported;
    }

    /**
     * Returns if postview is supported.
     */
    @Override
    public boolean isPostviewSupported() {
        return mIsPostviewSupported;
    }

    @Override
    public boolean isCaptureProcessProgressSupported() {
        return mIsCaptureProcessProgressSupported;
    }
}
