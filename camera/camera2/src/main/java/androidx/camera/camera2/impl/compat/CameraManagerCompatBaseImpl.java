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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.camera.core.impl.utils.MainThreadAsyncHandler;
import androidx.core.util.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

@RequiresApi(21)
class CameraManagerCompatBaseImpl implements CameraManagerCompat.CameraManagerCompatImpl {

    final CameraManager mCameraManager;
    final Object mObject;

    CameraManagerCompatBaseImpl(@NonNull Context context, @Nullable Object cameraManagerParams) {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mObject = cameraManagerParams;
    }

    CameraManagerCompatBaseImpl(@NonNull Context context) {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mObject = new CameraManagerCompatParamsApi21();
    }

    @Override
    public void registerAvailabilityCallback(@NonNull Executor executor,
            @NonNull CameraManager.AvailabilityCallback callback) {
        if (executor == null) {
            throw new IllegalArgumentException("executor was null");
        }

        CameraManagerCompat.AvailabilityCallbackExecutorWrapper wrapper = null;
        if (callback != null) {
            CameraManagerCompatParamsApi21 params = (CameraManagerCompatParamsApi21) mObject;
            synchronized (params.mWrapperMap) {
                wrapper = params.mWrapperMap.get(callback);
                if (wrapper == null) {
                    wrapper = new CameraManagerCompat.AvailabilityCallbackExecutorWrapper(executor,
                            callback);
                    params.mWrapperMap.put(callback, wrapper);
                }
            }
        }

        mCameraManager.registerAvailabilityCallback(wrapper, MainThreadAsyncHandler.getInstance());
    }

    @Override
    public void unregisterAvailabilityCallback(
            @NonNull CameraManager.AvailabilityCallback callback) {
        CameraManagerCompat.AvailabilityCallbackExecutorWrapper wrapper = null;
        if (callback != null) {
            CameraManagerCompatParamsApi21 params = (CameraManagerCompatParamsApi21) mObject;
            synchronized (params.mWrapperMap) {
                wrapper = params.mWrapperMap.remove(callback);
            }
        }

        mCameraManager.unregisterAvailabilityCallback(wrapper);
    }

    @RequiresPermission(android.Manifest.permission.CAMERA)
    @Override
    public void openCamera(@NonNull String cameraId, @NonNull Executor executor,
            @NonNull CameraDevice.StateCallback callback) throws CameraAccessException {
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(callback);

        // Wrap the executor in the callback
        CameraDevice.StateCallback cb =
                new CameraDeviceCompat.StateCallbackExecutorWrapper(executor, callback);

        mCameraManager.openCamera(cameraId, cb, MainThreadAsyncHandler.getInstance());
    }

    @NonNull
    @Override
    public CameraManager getCameraManager() {
        return mCameraManager;
    }

    static final class CameraManagerCompatParamsApi21 {
        @GuardedBy("mWrapperMap")
        final Map<CameraManager.AvailabilityCallback,
                CameraManagerCompat.AvailabilityCallbackExecutorWrapper>
                mWrapperMap = new HashMap<>();
    }
}

