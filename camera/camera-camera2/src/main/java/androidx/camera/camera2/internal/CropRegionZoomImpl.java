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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.CameraControl;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class CropRegionZoomImpl implements ZoomControl.ZoomImpl {
    public static final float MIN_DIGITAL_ZOOM = 1.0f;

    private final CameraCharacteristicsCompat mCameraCharacteristics;
    private Rect mCurrentCropRect = null;
    private CallbackToFutureAdapter.Completer<Void> mPendingZoomRatioCompleter;
    private Rect mPendingZoomCropRegion = null;

    CropRegionZoomImpl(@NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        mCameraCharacteristics = cameraCharacteristics;
    }

    @Override
    public float getMinZoom() {
        return MIN_DIGITAL_ZOOM;
    }

    @Override
    public float getMaxZoom() {
        Float maxZoom = mCameraCharacteristics.get(
                CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        if (maxZoom == null) {
            return MIN_DIGITAL_ZOOM;
        }

        if (maxZoom < getMinZoom()) {
            return getMinZoom();
        }
        return maxZoom;
    }

    @Override
    public void addRequestOption(@NonNull Camera2ImplConfig.Builder builder) {
        if (mCurrentCropRect != null) {
            builder.setCaptureRequestOption(CaptureRequest.SCALER_CROP_REGION,
                    mCurrentCropRect);
        }
    }

    @Override
    public void resetZoom() {
        mPendingZoomCropRegion = null;
        mCurrentCropRect = null;

        // Fails the pending ListenableFuture.
        if (mPendingZoomRatioCompleter != null) {
            mPendingZoomRatioCompleter
                    .setException(
                            new CameraControl.OperationCanceledException("Camera is not active."));
            mPendingZoomRatioCompleter = null;
        }
    }

    private Rect getSensorRect() {
        return Preconditions.checkNotNull(
                mCameraCharacteristics.get(
                        CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE));
    }

    @NonNull
    private static Rect getCropRectByRatio(@NonNull Rect sensorRect, float ratio) {
        float cropWidth = (sensorRect.width() / ratio);
        float cropHeight = (sensorRect.height() / ratio);
        float left = ((sensorRect.width() - cropWidth) / 2.0f);
        float top = ((sensorRect.height() - cropHeight) / 2.0f);
        return new Rect((int) left, (int) top, (int) (left + cropWidth),
                (int) (top + cropHeight));
    }

    @Override
    public void setZoomRatio(float zoomRatio,
            @NonNull CallbackToFutureAdapter.Completer<Void> completer) {
        Rect sensorRect = getSensorRect();
        mCurrentCropRect = getCropRectByRatio(sensorRect, zoomRatio);

        if (mPendingZoomRatioCompleter != null) {
            mPendingZoomRatioCompleter.setException(
                    new CameraControl.OperationCanceledException(
                            "There is a new zoomRatio being set"));
        }

        // Wait for zoom ratio action to complete.
        mPendingZoomCropRegion = mCurrentCropRect;
        mPendingZoomRatioCompleter = completer;
    }

    @Override
    public void onCaptureResult(@NonNull TotalCaptureResult captureResult) {
        // Compare the requested crop region, not the result's crop region because HAL
        // could modify the requested crop region.
        if (mPendingZoomRatioCompleter != null) {
            CaptureRequest request = captureResult.getRequest();
            Rect cropRect = (request == null) ? null :
                    request.get(CaptureRequest.SCALER_CROP_REGION);

            if (mPendingZoomCropRegion != null
                    && mPendingZoomCropRegion.equals(cropRect)) {
                mPendingZoomRatioCompleter.set(null);
                mPendingZoomRatioCompleter = null;
                mPendingZoomCropRegion = null;
            }
        }
    }

    @NonNull
    @Override
    public Rect getCropSensorRegion() {
        return mCurrentCropRect != null ? mCurrentCropRect : getSensorRect();
    }
}
