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

package androidx.camera.camera2.internal.compat;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
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

    static CameraManagerCompatBaseImpl create(@NonNull Context context,
            @NonNull Handler compatHandler) {
        return new CameraManagerCompatBaseImpl(context,
                new CameraManagerCompatParamsApi21(compatHandler));
    }

    @NonNull
    @Override
    public String[] getCameraIdList() throws CameraAccessExceptionCompat {
        try {
            return mCameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            throw CameraAccessExceptionCompat.toCameraAccessExceptionCompat(e);
        }
    }

    @Override
    public void registerAvailabilityCallback(@NonNull Executor executor,
            @NonNull CameraManager.AvailabilityCallback callback) {
        if (executor == null) {
            throw new IllegalArgumentException("executor was null");
        }

        CameraManagerCompat.AvailabilityCallbackExecutorWrapper wrapper = null;
        CameraManagerCompatParamsApi21 params = (CameraManagerCompatParamsApi21) mObject;
        if (callback != null) {
            synchronized (params.mWrapperMap) {
                wrapper = params.mWrapperMap.get(callback);
                if (wrapper == null) {
                    wrapper = new CameraManagerCompat.AvailabilityCallbackExecutorWrapper(executor,
                            callback);
                    params.mWrapperMap.put(callback, wrapper);
                }
            }
        }

        mCameraManager.registerAvailabilityCallback(wrapper, params.mCompatHandler);
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

        if (wrapper != null) {
            wrapper.setDisabled();
        }
        mCameraManager.unregisterAvailabilityCallback(wrapper);
    }

    @Override
    @NonNull
    public CameraCharacteristics getCameraCharacteristics(@NonNull String cameraId)
            throws CameraAccessExceptionCompat {
        try {
            return mCameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            throw CameraAccessExceptionCompat.toCameraAccessExceptionCompat(e);
        }
    }

    @RequiresPermission(android.Manifest.permission.CAMERA)
    @Override
    public void openCamera(@NonNull String cameraId, @NonNull Executor executor,
            @NonNull CameraDevice.StateCallback callback) throws CameraAccessExceptionCompat {
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(callback);

        // Wrap the executor in the callback
        CameraDevice.StateCallback cb =
                new CameraDeviceCompat.StateCallbackExecutorWrapper(executor, callback);

        CameraManagerCompatParamsApi21 params = (CameraManagerCompatParamsApi21) mObject;
        try {
            mCameraManager.openCamera(cameraId, cb, params.mCompatHandler);
        } catch (CameraAccessException e) {
            throw CameraAccessExceptionCompat.toCameraAccessExceptionCompat(e);
        }
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
        final Handler mCompatHandler;

        CameraManagerCompatParamsApi21(@NonNull Handler compatHandler) {
            mCompatHandler = compatHandler;
        }
    }
}

