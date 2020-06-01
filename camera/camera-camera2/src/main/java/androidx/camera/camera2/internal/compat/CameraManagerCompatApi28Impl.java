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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

@RequiresApi(28)
class CameraManagerCompatApi28Impl extends CameraManagerCompatBaseImpl {

    CameraManagerCompatApi28Impl(@NonNull Context context, @Nullable Object cameraManagerParams) {
        super(context, cameraManagerParams);
    }

    static CameraManagerCompatApi28Impl create(@NonNull Context context) {
        return new CameraManagerCompatApi28Impl(context, new CameraManagerCompatParamsApi28());
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

            if (Build.VERSION.SDK_INT == 28) {
                // Update the cache if it doesn't exist.
                synchronized (CameraManagerCompatParamsApi28.sCameraCharacteristicsMap) {
                    if (!CameraManagerCompatParamsApi28.sCameraCharacteristicsMap.containsKey(
                            cameraId)) {
                        CameraManagerCompatParamsApi28.sCameraCharacteristicsMap.put(cameraId,
                                cameraCharacteristics);
                    }
                }
            }
        } catch (RuntimeException e) {
            if (isDndFailCase(e)) {
                // Get from cache if it exists.
                synchronized (CameraManagerCompatParamsApi28.sCameraCharacteristicsMap) {
                    if (CameraManagerCompatParamsApi28.sCameraCharacteristicsMap.containsKey(
                            cameraId)) {
                        return CameraManagerCompatParamsApi28.sCameraCharacteristicsMap.get(
                                cameraId);
                    }
                }
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

    static final class CameraManagerCompatParamsApi28 {
        /*
         * This is a workaround by caching CameraCharacteristics. There is an issue with the early
         * version of API 28. If the device "Do Not Disturb (DND)" mode is enabled, a
         * RuntimeException is thrown when calling
         * CameraManager#getCameraCharacteristics() and CameraManager#openCamera(). Caching the
         * CameraCharacteristics can prevent library users from encountering the exception as
         * possible, even if it will rethrow CameraAccessExceptionCompat.
         *
         * When DND error occurs, we expect the developer can notify the user to turn off DND.
         * Since developer usually do not expect API CameraManager#getCameraCharacteristics() to
         * throw exception, and this API may be widely and discretely called by users, it may not be
         * easy to display notification with a single design. If the exception can be thrown in a
         * single location such as CameraManager#openCamera(), it is much easier to notify the
         * user of the DND situation. Usually the situation that the camera fails to open is well
         * handled.
         *
         * The cache will only be used in the case of DND failure, so it won't affect
         * general use. Make the cache "static" because it is shared between CameraManagerCompat
         * instances.
         */
        @GuardedBy("sCameraCharacteristicsMap")
        static final Map<String, CameraCharacteristics> sCameraCharacteristicsMap = new HashMap<>();

        CameraManagerCompatParamsApi28() {
        }
    }
}

