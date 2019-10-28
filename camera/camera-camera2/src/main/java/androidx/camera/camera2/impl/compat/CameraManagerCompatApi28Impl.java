/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.camera2.impl.compat;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import java.util.concurrent.Executor;

@RequiresApi(28)
class CameraManagerCompatApi28Impl extends CameraManagerCompatBaseImpl {

    CameraManagerCompatApi28Impl(@NonNull Context context) {
        // No extra params needed for this API level
        super(context, /*cameraManagerParams=*/ null);
    }

    @Override
    public void registerAvailabilityCallback(@NonNull Executor executor,
            @NonNull CameraManager.AvailabilityCallback callback) {

        // Pass through directly to the executor API that exists on this API level.
        mCameraManager.registerAvailabilityCallback(executor, callback);
    }

    @Override
    public void unregisterAvailabilityCallback(
            @NonNull CameraManager.AvailabilityCallback callback) {

        // Pass through directly to override behavior defined by API 21
        mCameraManager.unregisterAvailabilityCallback(callback);
    }

    @RequiresPermission(android.Manifest.permission.CAMERA)
    @Override
    public void openCamera(@NonNull String cameraId, @NonNull Executor executor,
            @NonNull CameraDevice.StateCallback callback) throws CameraAccessException {

        // Pass through directly to the executor API that exists on this API level.
        mCameraManager.openCamera(cameraId, executor, callback);
    }
}

