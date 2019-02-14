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

package androidx.camera.testapp.camera2interoperror;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;

import androidx.camera.core.BaseUseCase;
import androidx.camera.core.CameraX;
import androidx.camera.core.SessionConfiguration;

import java.util.Collections;
import java.util.Map;

/** A use case which attempts to use camera2 calls directly in an erroneous manner. */
public class Camera2InteropErrorUseCase extends BaseUseCase {
    private static final String TAG = "Camera2InteropErrorUseCase";
    private final Camera2InteropErrorUseCaseConfiguration mConfiguration;
    private final CameraCaptureSession.StateCallback mCaptureSessionStateCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "CameraCaptureSession.StateCallback.onConfigured()");
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "CameraCaptureSession.StateCallback.onConfigured()");
                }
            };
    private CameraDevice mCameraDevice;
    private final CameraDevice.StateCallback mStateCallback =
            new StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d(TAG, "CameraDevice.StateCallback.onOpened()");
                    Camera2InteropErrorUseCase.this.mCameraDevice = camera;
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.d(TAG, "CameraDevice.StateCallback.onDisconnected()");
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.d(TAG, "CameraDevice.StateCallback.onError()");
                }
            };
    private ImageReader mImageReader;

    public Camera2InteropErrorUseCase(Camera2InteropErrorUseCaseConfiguration configuration) {
        super(configuration);
        mConfiguration = configuration;
    }

    /** Closes the {@link CameraDevice} obtained via callback. */
    void closeCamera() {
        if (mCameraDevice != null) {
            Log.d(TAG, "Closing CameraDevice.");
            mCameraDevice.close();
        } else {
            Log.d(TAG, "No CameraDevice to close.");
        }
    }

    /**
     * Opens a {@link CameraCaptureSession} using the {@link CameraDevice} obtained via callback.
     */
    void reopenCaptureSession() {
        try {
            Log.d(TAG, "Opening a CameraCaptureSession.");
            mCameraDevice.createCaptureSession(
                    Collections.singletonList(mImageReader.getSurface()),
                    mCaptureSessionStateCallback,
                    null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "no permission to create capture session");
        }
    }

    @Override
    protected Map<String, Size> onSuggestedResolutionUpdated(
            Map<String, Size> suggestedResolutionMap) {
        mImageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2);

        mImageReader.setOnImageAvailableListener(
                imageReader -> {
                    imageReader.acquireNextImage().close();
                },
                null);

        SessionConfiguration.Builder sessionConfigBuilder = new SessionConfiguration.Builder();
        sessionConfigBuilder.clearSurfaces();
        sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        sessionConfigBuilder.setDeviceStateCallback(mStateCallback);

        try {
            String cameraId = CameraX.getCameraWithLensFacing(mConfiguration.getLensFacing());
            attachToCamera(cameraId, sessionConfigBuilder.build());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + mConfiguration.getLensFacing(),
                    e);
        }

        return suggestedResolutionMap;
    }
}
