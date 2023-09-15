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

package androidx.camera.extensions.internal;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.Camera2CameraInfoImpl;
import androidx.camera.camera2.pipe.integration.adapter.CameraInfoAdapter;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.impl.CameraInfoInternal;

import java.util.Map;


@OptIn(markerClass = {androidx.camera.camera2.interop.ExperimentalCamera2Interop.class,
        androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop.class})
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public final class Camera2CameraInfoWrapper {
    private androidx.camera.camera2.interop.Camera2CameraInfo mCamera2CameraInfo;
    private androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo
            mCameraPipeCameraInfo;

    Camera2CameraInfoWrapper(@NonNull Camera2CameraInfoImpl camera2CameraInfo) {
        mCamera2CameraInfo =
                androidx.camera.camera2.interop.Camera2CameraInfo.from(camera2CameraInfo);
    }

    Camera2CameraInfoWrapper(@NonNull CameraInfoAdapter cameraInfoAdapter) {
        mCameraPipeCameraInfo =
                androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo.from(
                        cameraInfoAdapter);
    }

    @NonNull
    public static Camera2CameraInfoWrapper from(@NonNull CameraInfo cameraInfo) {
        CameraInfoInternal cameraInfoInternal =
                ((CameraInfoInternal) cameraInfo).getImplementation();
        if (cameraInfoInternal instanceof Camera2CameraInfoImpl) {
            return new Camera2CameraInfoWrapper((Camera2CameraInfoImpl) cameraInfoInternal);
        } else if (cameraInfoInternal instanceof CameraInfoAdapter) {
            return new Camera2CameraInfoWrapper((CameraInfoAdapter) cameraInfoInternal);
        } else {
            throw new IllegalArgumentException("Not a Camera2 implementation!");
        }
    }

    @NonNull
    public String getCameraId() {
        if (mCamera2CameraInfo != null) {
            return mCamera2CameraInfo.getCameraId();
        } else {
            return mCameraPipeCameraInfo.getCameraId();
        }
    }

    @NonNull
    public <T> T getCameraCharacteristic(@NonNull CameraCharacteristics.Key<T> key) {
        if (mCamera2CameraInfo != null) {
            return mCamera2CameraInfo.getCameraCharacteristic(key);
        } else {
            return mCameraPipeCameraInfo.getCameraCharacteristic(key);
        }
    }

    @NonNull
    public static CameraCharacteristics extractCameraCharacteristics(
            @NonNull CameraInfo cameraInfo) {
        CameraInfoInternal cameraInfoInternal =
                ((CameraInfoInternal) cameraInfo).getImplementation();
        if (cameraInfoInternal instanceof Camera2CameraInfoImpl) {
            return androidx.camera.camera2.interop.Camera2CameraInfo.extractCameraCharacteristics(
                    cameraInfo);
        } else if (cameraInfoInternal instanceof CameraInfoAdapter) {
            return androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo
                    .extractCameraCharacteristics(cameraInfo);
        } else {
            throw new IllegalArgumentException("Not a Camera2 implementation!");
        }
    }

    @NonNull
    public Map<String, CameraCharacteristics> getCameraCharacteristicsMap() {
        if (mCamera2CameraInfo != null) {
            return mCamera2CameraInfo.getCameraCharacteristicsMap();
        } else {
            return mCameraPipeCameraInfo.getCameraCharacteristicsMap();
        }
    }
}
