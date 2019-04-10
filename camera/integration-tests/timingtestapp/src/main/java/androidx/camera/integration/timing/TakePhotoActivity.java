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

package androidx.camera.integration.timing;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.camera.camera2.Camera2Configuration;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageCaptureUseCase;
import androidx.camera.core.ImageCaptureUseCase.CaptureMode;
import androidx.camera.core.ImageCaptureUseCaseConfiguration;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ViewFinderUseCase;
import androidx.camera.core.ViewFinderUseCaseConfiguration;

/** This Activity is used to run image capture performance test in mobileharness. */
public class TakePhotoActivity extends BaseActivity {

    private static final String TAG = "TakePhotoActivity";
    // How many sample frames we should use to calculate framerate.
    private static final int FRAMERATE_SAMPLE_WINDOW = 5;
    private static final String EXTRA_CAPTURE_MODE = "capture_mode";
    private static final String EXTRA_CAMERA_FACING = "camera_facing";
    private static final String CAMERA_FACING_FRONT = "FRONT";
    private static final String CAMERA_FACING_BACK = "BACK";
    private final String mDefaultCameraFacing = CAMERA_FACING_BACK;
    private final CameraDevice.StateCallback mDeviceStateCallback =
            new CameraDevice.StateCallback() {

                @Override
                public void onOpened(CameraDevice cameraDevice) {
                    openCameraTotalTime = System.currentTimeMillis() - openCameraStartTime;
                    Log.d(TAG, "[onOpened] openCameraTotalTime: " + openCameraTotalTime);
                    startRreviewTime = System.currentTimeMillis();
                }

                @Override
                public void onClosed(CameraDevice camera) {
                    super.onClosed(camera);
                    closeCameraTotalTime = System.currentTimeMillis() - closeCameraStartTime;
                    Log.d(TAG, "[onClosed] closeCameraTotalTime: " + closeCameraTotalTime);
                    onUseCaseFinish();
                }

                @Override
                public void onDisconnected(CameraDevice cameraDevice) {
                }

                @Override
                public void onError(CameraDevice cameraDevice, int i) {
                    Log.e(TAG, "[onError] open camera failed, error code: " + i);
                }
            };
    private final CameraCaptureSession.StateCallback mCaptureSessionStateCallback =
            new CameraCaptureSession.StateCallback() {

                @Override
                public void onActive(CameraCaptureSession session) {
                    super.onActive(session);
                    startPreviewTotalTime = System.currentTimeMillis() - startRreviewTime;
                    Log.d(TAG, "[onActive] previewStartTotalTime: " + startPreviewTotalTime);
                }

                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    Log.d(TAG, "[onConfigured] CaptureSession configured!");
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Log.e(TAG, "[onConfigureFailed] CameraX preview initialization failed.");
                }
            };
    /** The default cameraId to use. */
    private LensFacing mCurrentCameraLensFacing = LensFacing.BACK;
    private ImageCaptureUseCase mImageCaptureUseCase;
    private ViewFinderUseCase mViewFinderUseCase;
    private int mFrameCount;
    private long mPreviewSampleStartTime;
    private CaptureMode mCaptureMode = CaptureMode.MIN_LATENCY;
    private CustomLifecycle mCustomLifecycle;

    @Override
    public void runUseCase() throws InterruptedException {

        // Length of time to let the preview stream run before capturing the first image.
        // This can help ensure capture latency is real latency and not merely the device
        // filling the buffer.
        Thread.sleep(PREVIEW_FILL_BUFFER_TIME);

        startTime = System.currentTimeMillis();
        mImageCaptureUseCase.takePicture(
                new ImageCaptureUseCase.OnImageCapturedListener() {
                    @Override
                    public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                        totalTime = System.currentTimeMillis() - startTime;
                        if (image != null) {
                            imageResolution = image.getWidth() + "x" + image.getHeight();
                        } else {
                            Log.e(TAG, "[onCaptureSuccess] image is null");
                        }
                    }
                });
    }

    @Override
    public void prepareUseCase() {
        createViewFinderUseCase();
        createImageCaptureUseCase();
    }

    void createViewFinderUseCase() {
        ViewFinderUseCaseConfiguration.Builder configurationBuilder =
                new ViewFinderUseCaseConfiguration.Builder()
                        .setLensFacing(mCurrentCameraLensFacing)
                        .setTargetName("ViewFinder");

        new Camera2Configuration.Extender(configurationBuilder)
                .setDeviceStateCallback(mDeviceStateCallback)
                .setSessionStateCallback(mCaptureSessionStateCallback);

        mViewFinderUseCase = new ViewFinderUseCase(configurationBuilder.build());
        openCameraStartTime = System.currentTimeMillis();

        mViewFinderUseCase.setOnViewFinderOutputUpdateListener(
                new ViewFinderUseCase.OnViewFinderOutputUpdateListener() {
                    @Override
                    public void onUpdated(ViewFinderUseCase.ViewFinderOutput viewFinderOutput) {
                        TextureView textureView = TakePhotoActivity.this.findViewById(
                                R.id.textureView);
                        ViewGroup viewGroup = (ViewGroup) textureView.getParent();
                        viewGroup.removeView(textureView);
                        viewGroup.addView(textureView);
                        textureView.setSurfaceTexture(viewFinderOutput.getSurfaceTexture());
                        textureView.setSurfaceTextureListener(
                                new SurfaceTextureListener() {
                                    @Override
                                    public void onSurfaceTextureAvailable(
                                            SurfaceTexture surfaceTexture, int i, int i1) {
                                    }

                                    @Override
                                    public void onSurfaceTextureSizeChanged(
                                            SurfaceTexture surfaceTexture, int i, int i1) {
                                    }

                                    @Override
                                    public boolean onSurfaceTextureDestroyed(
                                            SurfaceTexture surfaceTexture) {
                                        return false;
                                    }

                                    @Override
                                    public void onSurfaceTextureUpdated(
                                            SurfaceTexture surfaceTexture) {
                                        Log.d(TAG, "[onSurfaceTextureUpdated]");
                                        if (0 == totalTime) {
                                            return;
                                        }

                                        if (0 == mFrameCount) {
                                            mPreviewSampleStartTime = System.currentTimeMillis();
                                        } else if (FRAMERATE_SAMPLE_WINDOW == mFrameCount) {
                                            final long duration =
                                                    System.currentTimeMillis()
                                                            - mPreviewSampleStartTime;
                                            previewFrameRate =
                                                    (MICROS_IN_SECOND
                                                            * FRAMERATE_SAMPLE_WINDOW
                                                            / duration);
                                            closeCameraStartTime = System.currentTimeMillis();
                                            mCustomLifecycle.doDestroyed();
                                        }
                                        mFrameCount++;
                                    }
                                });
                    }
                });

        CameraX.bindToLifecycle(mCustomLifecycle, mViewFinderUseCase);
    }

    void createImageCaptureUseCase() {
        ImageCaptureUseCaseConfiguration configuration =
                new ImageCaptureUseCaseConfiguration.Builder()
                        .setTargetName("ImageCapture")
                        .setLensFacing(mCurrentCameraLensFacing)
                        .setCaptureMode(mCaptureMode)
                        .build();

        mImageCaptureUseCase = new ImageCaptureUseCase(configuration);
        CameraX.bindToLifecycle(mCustomLifecycle, mImageCaptureUseCase);

        final Button button = this.findViewById(R.id.Picture);
        button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startTime = System.currentTimeMillis();
                        mImageCaptureUseCase.takePicture(
                                new ImageCaptureUseCase.OnImageCapturedListener() {
                                    @Override
                                    public void onCaptureSuccess(
                                            ImageProxy image, int rotationDegrees) {
                                        totalTime = System.currentTimeMillis() - startTime;
                                        if (image != null) {
                                            imageResolution =
                                                    image.getWidth() + "x" + image.getHeight();
                                        } else {
                                            Log.e(TAG, "[onCaptureSuccess] image is null");
                                        }
                                    }
                                });
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            final String captureModeString = bundle.getString(EXTRA_CAPTURE_MODE);
            if (captureModeString != null) {
                mCaptureMode = CaptureMode.valueOf(captureModeString.toUpperCase());
            }
            final String cameraLensFacing = bundle.getString(EXTRA_CAMERA_FACING);
            if (cameraLensFacing != null) {
                setupCamera(cameraLensFacing);
            } else {
                setupCamera(mDefaultCameraFacing);
            }
        }
        mCustomLifecycle = new CustomLifecycle();
        prepareUseCase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCustomLifecycle.doOnResume();
    }

    void setupCamera(String cameraFacing) {
        Log.d(TAG, "Camera Facing: " + cameraFacing);
        if (CAMERA_FACING_BACK.equalsIgnoreCase(cameraFacing)) {
            mCurrentCameraLensFacing = LensFacing.BACK;
        } else if (CAMERA_FACING_FRONT.equalsIgnoreCase(cameraFacing)) {
            mCurrentCameraLensFacing = LensFacing.FRONT;
        } else {
            throw new RuntimeException("Invalid lens facing: " + cameraFacing);
        }
    }
}
