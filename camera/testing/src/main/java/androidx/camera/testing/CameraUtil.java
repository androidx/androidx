/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.testing;

import android.Manifest;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.camera.core.BaseCamera;
import androidx.camera.core.UseCase;
import androidx.test.core.app.ApplicationProvider;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Utility functions for obtaining instances of camera2 classes. */
public final class CameraUtil {
    /** Amount of time to wait before timing out when trying to open a {@link CameraDevice}. */
    private static final int CAMERA_OPEN_TIMEOUT_SECONDS = 2;

    /**
     * Gets a new instance of a {@link CameraDevice}.
     *
     * <p>This method attempts to open up a new camera. Since the camera api is asynchronous it
     * needs to wait for camera open
     *
     * <p>After the camera is no longer needed {@link #releaseCameraDevice(CameraDevice)} should be
     * called to clean up resources.
     *
     * @throws CameraAccessException if the device is unable to access the camera
     * @throws InterruptedException  if a {@link CameraDevice} can not be retrieved within a set
     *                               time
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    public static CameraDevice getCameraDevice()
            throws CameraAccessException, InterruptedException {
        // Setup threading required for callback on openCamera()
        final HandlerThread handlerThread = new HandlerThread("handler thread");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());

        CameraManager cameraManager = getCameraManager();

        // Use the first camera available.
        String[] cameraIds = cameraManager.getCameraIdList();
        if (cameraIds.length <= 0) {
            throw new CameraAccessException(
                    CameraAccessException.CAMERA_ERROR, "Device contains no cameras.");
        }
        String cameraName = cameraIds[0];

        // Use an AtomicReference to store the CameraDevice because it is initialized in a lambda.
        // This way the AtomicReference itself is effectively final.
        final AtomicReference<CameraDevice> cameraDeviceHolder = new AtomicReference<>();

        // Open the camera using the CameraManager which returns a valid and open CameraDevice only
        // when onOpened() is called.
        final CountDownLatch latch = new CountDownLatch(1);
        cameraManager.openCamera(
                cameraName,
                new StateCallback() {
                    @Override
                    public void onOpened(CameraDevice camera) {
                        cameraDeviceHolder.set(camera);
                        latch.countDown();
                    }

                    @Override
                    public void onClosed(CameraDevice cameraDevice) {
                        handlerThread.quit();
                    }

                    @Override
                    public void onDisconnected(CameraDevice camera) {
                    }

                    @Override
                    public void onError(CameraDevice camera, int error) {
                    }
                },
                handler);

        // Wait for the callback to initialize the CameraDevice
        latch.await(CAMERA_OPEN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        return cameraDeviceHolder.get();
    }

    /**
     * Cleans up resources that need to be kept around while the camera device is active.
     *
     * @param cameraDevice camera that was obtained via {@link #getCameraDevice()}
     */
    public static void releaseCameraDevice(CameraDevice cameraDevice) {
        cameraDevice.close();
    }

    public static CameraManager getCameraManager() {
        return (CameraManager)
                ApplicationProvider.getApplicationContext()
                        .getSystemService(Context.CAMERA_SERVICE);
    }

    /**
     * Opens a camera and associates the camera with multiple use cases.
     *
     * <p>Sets the use case to be online and active, so that the use case is in a state to issue
     * capture requests to the camera. The caller is responsible for making the use case inactive
     * and offline and for closing the camera afterwards.
     *
     * @param cameraId to open
     * @param camera   to open
     * @param useCases to associate with
     */
    public static void openCameraWithUseCase(String cameraId, BaseCamera camera,
            UseCase... useCases) {
        camera.addOnlineUseCase(Arrays.asList(useCases));
        for (UseCase useCase : useCases) {
            useCase.attachCameraControl(cameraId, camera.getCameraControlInternal());
            camera.onUseCaseActive(useCase);
        }
    }

    /**
     * Detach multiple use cases from a camera.
     *
     * <p>Sets the use cases to be inactive and remove from the online list.
     *
     * @param camera   to detach from
     * @param useCases to be detached
     */
    public static void detachUseCaseFromCamera(BaseCamera camera, UseCase... useCases) {
        for (UseCase useCase : useCases) {
            camera.onUseCaseInactive(useCase);
        }
        camera.removeOnlineUseCase(Arrays.asList(useCases));
    }

    /**
     * Check if there is any camera in the device.
     *
     * <p>If there is no camera in the device, most tests will failed.
     *
     * @return false if no camera
     */
    public static boolean deviceHasCamera() {
        // TODO Think about external camera case,
        //  especially no built in camera but there might be some external camera

        // It also could be checked by PackageManager's hasSystemFeature() with following:
        //     FEATURE_CAMERA, FEATURE_CAMERA_FRONT, FEATURE_CAMERA_ANY.
        // But its needed to consider one case that platform build with camera feature but there is
        // no built in camera or external camera.

        int numberOfCamera = 0;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            try {
                numberOfCamera = ((CameraManager) ApplicationProvider.getApplicationContext()
                        .getSystemService(Context.CAMERA_SERVICE)).getCameraIdList().length;
            } catch (CameraAccessException e) {
                Log.e(CameraUtil.class.getSimpleName(), "Unable to check camera availability.", e);
            }
        } else {
            numberOfCamera = Camera.getNumberOfCameras();
        }

        return numberOfCamera > 0;
    }
}
