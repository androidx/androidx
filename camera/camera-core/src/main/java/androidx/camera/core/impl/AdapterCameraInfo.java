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

import static android.hardware.camera2.CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON;
import static android.hardware.camera2.CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION;

import android.util.Range;
import android.util.Rational;

import androidx.annotation.IntDef;
import androidx.camera.core.ExposureState;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.TorchState;
import androidx.camera.core.UseCase;
import androidx.camera.core.ZoomState;
import androidx.camera.core.impl.utils.LiveDataUtil;
import androidx.camera.core.impl.utils.SessionProcessorUtil;
import androidx.camera.core.internal.ImmutableZoomState;
import androidx.core.math.MathUtils;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * A {@link CameraInfoInternal} that returns disabled state if the corresponding operation in the
 * given {@link AdapterCameraControl} is disabled.
 */
public class AdapterCameraInfo extends ForwardingCameraInfo {
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
    private final @Nullable SessionProcessor mSessionProcessor;
    private boolean mIsPostviewSupported = false;
    private boolean mIsCaptureProcessProgressSupported = false;
    private final @NonNull CameraConfig mCameraConfig;
    private @Nullable LiveData<ZoomState> mExtensionZoomStateLiveData = null;

    public AdapterCameraInfo(@NonNull CameraInfoInternal cameraInfo,
            @NonNull CameraConfig cameraConfig) {
        super(cameraInfo);
        mCameraInfo = cameraInfo;
        mCameraConfig = cameraConfig;
        mSessionProcessor = cameraConfig.getSessionProcessor(null);

        setPostviewSupported(cameraConfig.isPostviewSupported());
        setCaptureProcessProgressSupported(cameraConfig.isCaptureProcessProgressSupported());
    }

    public @NonNull CameraConfig getCameraConfig() {
        return mCameraConfig;
    }

    @Override
    public @NonNull CameraInfoInternal getImplementation() {
        return mCameraInfo;
    }

    /**
     * Returns the session processor associated with the AdapterCameraInfo.
     */
    public @Nullable SessionProcessor getSessionProcessor() {
        return mSessionProcessor;
    }

    @Override
    public boolean hasFlashUnit() {
        if (!SessionProcessorUtil.isOperationSupported(mSessionProcessor, CAMERA_OPERATION_FLASH)) {
            return false;
        }

        return mCameraInfo.hasFlashUnit();
    }

    @Override
    public @NonNull LiveData<Integer> getTorchState() {
        if (!SessionProcessorUtil.isOperationSupported(mSessionProcessor, CAMERA_OPERATION_TORCH)) {
            return new MutableLiveData<>(TorchState.OFF);
        }

        return mCameraInfo.getTorchState();
    }

    /**
     * Return the zoom ratio calculated by the linear zoom (percentage)
     */
    public static float getZoomRatioByPercentage(float percentage,
            float minZoomRatio, float maxZoomRatio) {
        // Make sure 1.0f and 0.0 return exactly the same max/min ratio.
        if (percentage == 1.0f) {
            return maxZoomRatio;
        } else if (percentage == 0f) {
            return minZoomRatio;
        }
        // This crop width is proportional to the real crop width.
        // The real crop with = sensorWidth/ zoomRatio,  but we need the ratio only so we can
        // assume sensorWidth as 1.0f.
        double cropWidthInMaxZoom = 1.0f / maxZoomRatio;
        double cropWidthInMinZoom = 1.0f / minZoomRatio;

        double cropWidth = cropWidthInMinZoom + (cropWidthInMaxZoom - cropWidthInMinZoom)
                * percentage;

        double ratio = 1.0 / cropWidth;

        return (float) MathUtils.clamp(ratio, minZoomRatio, maxZoomRatio);
    }

    /**
     * Return the linear zoom (percentage) calculated by the zoom ratio.
     */
    public static float getPercentageByRatio(float ratio, float minZoomRatio, float maxZoomRatio) {
        // if zoom is not supported, return 0
        if (maxZoomRatio == minZoomRatio) {
            return 0f;
        }

        // To make the min/max same value when doing conversion between ratio / percentage.
        // We return the max/min value directly.
        if (ratio == maxZoomRatio) {
            return 1f;
        } else if (ratio == minZoomRatio) {
            return 0f;
        }

        float cropWidth = 1.0f / ratio;
        float cropWidthInMaxZoom = 1.0f / maxZoomRatio;
        float cropWidthInMinZoom = 1.0f / minZoomRatio;

        return (cropWidth - cropWidthInMinZoom) / (cropWidthInMaxZoom - cropWidthInMinZoom);
    }

    @Override
    public @NonNull LiveData<ZoomState> getZoomState() {
        if (!SessionProcessorUtil.isOperationSupported(mSessionProcessor, CAMERA_OPERATION_ZOOM)) {
            return new MutableLiveData<>(ImmutableZoomState.create(
                    /* zoomRatio */1f, /* maxZoomRatio */ 1f,
                    /* minZoomRatio */ 1f, /* linearZoom*/ 0f));
        }

        if (mSessionProcessor != null) {
            ZoomState zoomState = mCameraInfo.getZoomState().getValue();
            Range<Float> extensionsZoomRange = mSessionProcessor.getExtensionZoomRange();
            if (extensionsZoomRange != null
                    && (extensionsZoomRange.getLower() != zoomState.getMinZoomRatio()
                    || extensionsZoomRange.getUpper() != zoomState.getMaxZoomRatio())) {
                if (mExtensionZoomStateLiveData == null) {
                    // Transform the zoomState to have adjusted maxzoom, minzoom and linear zoom
                    mExtensionZoomStateLiveData = LiveDataUtil.map(mCameraInfo.getZoomState(),
                            (state) -> {
                                return ImmutableZoomState.create(
                                        state.getZoomRatio(),
                                        extensionsZoomRange.getUpper(),
                                        extensionsZoomRange.getLower(),
                                        getPercentageByRatio(state.getZoomRatio(),
                                                extensionsZoomRange.getLower(),
                                                extensionsZoomRange.getUpper())
                                );
                            });
                }
                return mExtensionZoomStateLiveData;
            }
        }
        return mCameraInfo.getZoomState();
    }

    @Override
    public @NonNull ExposureState getExposureState() {
        if (!SessionProcessorUtil.isOperationSupported(mSessionProcessor,
                CAMERA_OPERATION_EXPOSURE_COMPENSATION)) {
            return new ExposureState() {
                @Override
                public int getExposureCompensationIndex() {
                    return 0;
                }

                @Override
                public @NonNull Range<Integer> getExposureCompensationRange() {
                    return new Range<>(0, 0);
                }

                @Override
                public @NonNull Rational getExposureCompensationStep() {
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

    @Override
    public boolean isVideoStabilizationSupported() {
        if (mSessionProcessor != null) {
            int[] stabilizationModes = mSessionProcessor.getExtensionAvailableStabilizationModes();
            if (stabilizationModes != null) {
                for (int mode : stabilizationModes) {
                    if (mode == CONTROL_VIDEO_STABILIZATION_MODE_ON) {
                        return true;
                    }
                }
                return false;
            }
        }
        return super.isVideoStabilizationSupported();
    }

    @Override
    public boolean isPreviewStabilizationSupported() {
        if (mSessionProcessor != null) {
            int[] stabilizationModes = mSessionProcessor.getExtensionAvailableStabilizationModes();
            if (stabilizationModes != null) {
                for (int mode : stabilizationModes) {
                    if (mode == CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION) {
                        return true;
                    }
                }
                return false;
            }
        }
        return super.isPreviewStabilizationSupported();
    }

    @Override
    public boolean isUseCaseCombinationSupported(@NonNull List<@NonNull UseCase> useCases,
            int cameraMode, boolean isFeatureComboInvocation) {
        return mCameraInfo.isUseCaseCombinationSupported(useCases, cameraMode,
                isFeatureComboInvocation, mCameraConfig);
    }
}
