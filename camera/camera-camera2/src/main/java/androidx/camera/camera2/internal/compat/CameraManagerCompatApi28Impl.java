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
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import java.util.concurrent.Executor;

@RequiresApi(28)
class CameraManagerCompatApi28Impl extends CameraManagerCompatBaseImpl {

    CameraManagerCompatApi28Impl(@NonNull Context context) {
        // No extra params needed for this API level
        super(context, null);
    }

    static CameraManagerCompatApi28Impl create(@NonNull Context context) {
        return new CameraManagerCompatApi28Impl(context);
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
            @NonNull CameraDevice.StateCallback callback) throws CameraAccessExceptionCompat {

        try {
            // Pass through directly to the executor API that exists on this API level.
            mCameraManager.openCamera(cameraId, executor, callback);
        } catch (CameraAccessException e) {
            throw CameraAccessExceptionCompat.toCameraAccessExceptionCompat(e);
        } catch (IllegalArgumentException | SecurityException e) {
            // Re-throw those RuntimeException will be thrown by CameraManager#openCamera.
            throw e;
        } catch (RuntimeException e) {
            if (isDndFailCase(e)) {
                throwDndException(e);
            }
            throw e;
        }
    }

    @NonNull
    @Override
    public CameraCharacteristics getCameraCharacteristics(@NonNull String cameraId)
            throws CameraAccessExceptionCompat {
        CameraCharacteristics cameraCharacteristics;
        try {
            cameraCharacteristics = super.getCameraCharacteristics(cameraId);
        } catch (RuntimeException e) {
            if (isDndFailCase(e)) {
                // No need to get from cache here because we always get the instance from cache in
                // CameraManagerCompat.  So when DnDFail happens, it happens only when it gets
                // the CameraCharacteristics for the first time.
                throwDndException(e);
            }
            throw e;
        }
        return cameraCharacteristics;
    }

    private void throwDndException(@NonNull Throwable cause) throws CameraAccessExceptionCompat {
        throw new CameraAccessExceptionCompat(
                CameraAccessExceptionCompat.CAMERA_UNAVAILABLE_DO_NOT_DISTURB, cause);
    }

    /*
     * Check if the exception is due to Do Not Disturb being on, which is only on specific builds
     * of P. See b/149413835 and b/132362603.
     */
    private boolean isDndFailCase(@NonNull Throwable throwable) {
        return Build.VERSION.SDK_INT == 28 && isDndRuntimeException(throwable);
    }

    /*
     * The full stack
     *
     * java.lang.RuntimeException: Camera is being used after Camera.release() was called
     *  at android.hardware.Camera._enableShutterSound(Native Method)
     *  at android.hardware.Camera.updateAppOpsPlayAudio(Camera.java:1770)
     *  at android.hardware.Camera.initAppOps(Camera.java:582)
     *  at android.hardware.Camera.<init>(Camera.java:575)
     *  at android.hardware.Camera.getEmptyParameters(Camera.java:2130)
     *  at android.hardware.camera2.legacy.LegacyMetadataMapper.createCharacteristics
     *  (LegacyMetadataMapper.java:151)
     *  at android.hardware.camera2.CameraManager.getCameraCharacteristics(CameraManager.java:274)
     *
     * This method check the first stack is "_enableShutterSound"
     */
    private static boolean isDndRuntimeException(@NonNull Throwable throwable) {
        if (throwable.getClass().equals(RuntimeException.class)) {
            StackTraceElement[] stackTraceElement;
            if ((stackTraceElement = throwable.getStackTrace()) == null
                    || stackTraceElement.length < 0) {
                return false;
            }
            return "_enableShutterSound".equals(stackTraceElement[0].getMethodName());
        }
        return false;
    }
}

