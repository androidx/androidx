/*
 * Copyright 2020 The Android Open Source Project
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

import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Range;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.params.CaptureRequestParameterCompat;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraControl;
import androidx.camera.core.impl.Config;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

@RequiresApi(30)
final class AndroidRZoomImpl implements ZoomControl.ZoomImpl {
    public static final float DEFAULT_ZOOM_RATIO = 1.0f;
    private final CameraCharacteristicsCompat mCameraCharacteristics;
    private final Range<Float> mZoomRatioRange;
    private float mCurrentZoomRatio = DEFAULT_ZOOM_RATIO;
    private CallbackToFutureAdapter.Completer<Void> mPendingZoomRatioCompleter;
    private float mPendingZoomRatio = 1.0f;
    private boolean mShouldOverrideZoom = false;

    AndroidRZoomImpl(@NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        mCameraCharacteristics = cameraCharacteristics;
        mZoomRatioRange = mCameraCharacteristics
                .get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE);
        mShouldOverrideZoom = mCameraCharacteristics.isZoomOverrideAvailable();
    }

    @Override
    public float getMinZoom() {
        return mZoomRatioRange.getLower();
    }

    @Override
    public float getMaxZoom() {
        return mZoomRatioRange.getUpper();
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    @Override
    public void addRequestOption(@NonNull Camera2ImplConfig.Builder builder) {
        builder.setCaptureRequestOptionWithPriority(CaptureRequest.CONTROL_ZOOM_RATIO,
                mCurrentZoomRatio, Config.OptionPriority.REQUIRED);
        if (mShouldOverrideZoom) {
            CaptureRequestParameterCompat.setSettingsOverrideZoom(builder,
                    Config.OptionPriority.REQUIRED);
        }
    }

    @Override
    public void resetZoom() {
        mCurrentZoomRatio = DEFAULT_ZOOM_RATIO;

        // Fails the pending ListenableFuture.
        if (mPendingZoomRatioCompleter != null) {
            mPendingZoomRatioCompleter
                    .setException(
                            new CameraControl.OperationCanceledException("Camera is not active."));
            mPendingZoomRatioCompleter = null;
        }
    }

    @Override
    public void setZoomRatio(float zoomRatio,
            @NonNull CallbackToFutureAdapter.Completer<Void> completer) {
        mCurrentZoomRatio = zoomRatio;

        if (mPendingZoomRatioCompleter != null) {
            mPendingZoomRatioCompleter.setException(
                    new CameraControl.OperationCanceledException(
                            "There is a new zoomRatio being set"));
        }

        // Wait for zoom ratio action to complete.
        mPendingZoomRatio = mCurrentZoomRatio;
        mPendingZoomRatioCompleter = completer;
    }

    @Override
    public void onCaptureResult(@NonNull TotalCaptureResult captureResult) {
        if (mPendingZoomRatioCompleter != null) {
            CaptureRequest request = captureResult.getRequest();
            Float zoomRatioFloat = (request == null) ? null :
                    request.get(CaptureRequest.CONTROL_ZOOM_RATIO);

            if (zoomRatioFloat == null) {
                return;
            }

            float zoomRatio = zoomRatioFloat;
            if (mPendingZoomRatio == zoomRatio) {
                mPendingZoomRatioCompleter.set(null);
                mPendingZoomRatioCompleter = null;
            }
        }
    }

    @NonNull
    @Override
    public Rect getCropSensorRegion() {
        return Preconditions.checkNotNull(
                mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE));
    }
}
