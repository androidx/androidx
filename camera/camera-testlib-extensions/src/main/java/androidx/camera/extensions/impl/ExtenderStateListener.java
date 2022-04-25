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

package androidx.camera.extensions.impl;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.SessionConfiguration;

import androidx.annotation.RequiresApi;

/**
 * Provides interfaces that the OEM needs to implement to handle the state change.
 *
 * @since 1.0
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ExtenderStateListener {

    /**
     * Notify to initialize the extension. This will be called after bindToLifeCycle. This is
     * where the use case is started and would be able to allocate resources here. After onInit() is
     * called, the camera ID, cameraCharacteristics and context will not change until onDeInit()
     * has been called.
     *
     * @param cameraId The camera2 id string of the camera.
     * @param cameraCharacteristics The {@link CameraCharacteristics} of the camera.
     * @param context The {@link Context} used for CameraX.
     */
    void onInit(String cameraId, CameraCharacteristics cameraCharacteristics, Context context);

    /**
     * Notify to de-initialize the extension. This callback will be invoked after unbind.
     * After onDeInit() was called, it is expected that the camera ID, cameraCharacteristics will
     * no longer hold, this should be where to clear all resources allocated for this use case.
     */
    void onDeInit();

    /**
     * This will be invoked before creating a
     * {@link android.hardware.camera2.CameraCaptureSession}. The {@link CaptureRequest}
     * parameters returned via {@link CaptureStageImpl} will be passed to the camera device as
     * part of the capture session initialization via
     * {@link SessionConfiguration#setSessionParameters(CaptureRequest)} which only supported
     * from API level 28. The valid parameter is a subset of the available capture request
     * parameters.
     *
     * @return The request information to set the session wide camera parameters.
     */
    CaptureStageImpl onPresetSession();

    /**
     * This will be invoked once after the {@link android.hardware.camera2.CameraCaptureSession}
     * has been created. The {@link CaptureRequest} parameters returned via
     * {@link CaptureStageImpl} will be used to generate a single request to the current
     * configured {@link CameraDevice}. The generated request will be submitted to camera before
     * processing other single requests.
     *
     * @return The request information to create a single capture request to camera device.
     */
    CaptureStageImpl onEnableSession();

    /**
     * This will be invoked before the {@link android.hardware.camera2.CameraCaptureSession} is
     * closed. The {@link CaptureRequest} parameters returned via {@link CaptureStageImpl} will
     * be used to generate a single request to the currently configured {@link CameraDevice}. The
     * generated request will be submitted to camera before the CameraCaptureSession is closed.
     *
     * @return The request information to customize the session.
     */
    CaptureStageImpl onDisableSession();
}
