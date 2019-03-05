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

package androidx.camera.integration.core;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.BaseUseCase;
import androidx.camera.core.CameraDeviceConfiguration;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.FlashMode;
import androidx.camera.core.ImageAnalysisUseCase;
import androidx.camera.core.ImageAnalysisUseCaseConfiguration;
import androidx.camera.core.ImageCaptureUseCase;
import androidx.camera.core.ImageCaptureUseCaseConfiguration;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.VideoCaptureUseCase;
import androidx.camera.core.VideoCaptureUseCaseConfiguration;
import androidx.camera.core.ViewFinderUseCase;
import androidx.camera.core.ViewFinderUseCaseConfiguration;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.test.espresso.idling.CountingIdlingResource;

import java.io.File;
import java.math.BigDecimal;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


/**
 * An activity with four use cases: (1) view finder, (2) image capture, (3) image analysis, (4)
 * video capture.
 *
 * <p>All four use cases are created with CameraX and tied to the activity's lifecycle. CameraX
 * automatically connects and disconnects the use cases from the camera in response to changes in
 * the activity's lifecycle. Therefore, the use cases function properly when the app is paused and
 * resumed and when the device is rotated. The complex interactions between the camera and these
 * lifecycle events are handled internally by CameraX.
 */
public class CameraXActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "CameraXActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 42;
    // Possible values for this intent key: "backward" or "forward".
    private static final String INTENT_EXTRA_CAMERA_DIRECTION = "camera_direction";

    private final SettableCallable<Boolean> mSettableResult = new SettableCallable<>();
    private final FutureTask<Boolean> mCompletableFuture = new FutureTask<>(mSettableResult);
    private final AtomicLong mImageAnalysisFrameCount = new AtomicLong(0);
    private final MutableLiveData<String> mImageAnalysisResult = new MutableLiveData<>();
    private VideoFileSaver mVideoFileSaver;
    /** The cameraId to use. Assume that 0 is the typical back facing camera. */
    private LensFacing mCurrentCameraLensFacing = LensFacing.BACK;

    // TODO: Move the analysis processing, capture processing to separate threads, so
    // there is smaller impact on the preview.
    private String mCurrentCameraDirection = "BACKWARD";
    private ViewFinderUseCase mViewFinderUseCase;
    private ImageAnalysisUseCase mImageAnalysisUseCase;
    private ImageCaptureUseCase mImageCaptureUseCase;
    private VideoCaptureUseCase mVideoCaptureUseCase;

    // Espresso testing variables
    @VisibleForTesting
    CountingIdlingResource mIdlingResource = new CountingIdlingResource("view");
    private static final int FRAMES_UNTIL_VIEW_IS_READY = 5;
    @VisibleForTesting
    CountingIdlingResource mAnalysisIdlingResource =
            new CountingIdlingResource("analysis");
    @VisibleForTesting
    CountingIdlingResource mImageSavedIdlingResource =
            new CountingIdlingResource("imagesaved");


    /**
     * Creates a view finder use case.
     *
     * <p>This use case observes a {@link SurfaceTexture}. The texture is connected to a {@link
     * TextureView} to display a camera preview.
     */
    private void createViewFinderUseCase() {
        Button button = this.findViewById(R.id.PreviewToggle);
        button.setBackgroundColor(Color.LTGRAY);
        enableViewFinderUseCase();

        button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Button buttonView = (Button) view;
                        if (mViewFinderUseCase != null) {
                            // Remove the use case
                            buttonView.setBackgroundColor(Color.RED);
                            CameraX.unbind(mViewFinderUseCase);
                            mViewFinderUseCase = null;
                        } else {
                            // Add the use case
                            buttonView.setBackgroundColor(Color.LTGRAY);

                            CameraXActivity.this.enableViewFinderUseCase();
                        }
                    }
                });

        Log.i(TAG, "Got UseCase: " + mViewFinderUseCase);
    }

    void enableViewFinderUseCase() {
        ViewFinderUseCaseConfiguration configuration =
                new ViewFinderUseCaseConfiguration.Builder()
                        .setLensFacing(mCurrentCameraLensFacing)
                        .setTargetName("ViewFinder")
                        .build();

        mViewFinderUseCase = new ViewFinderUseCase(configuration);
        TextureView textureView = findViewById(R.id.textureView);
        mViewFinderUseCase.setOnViewFinderOutputUpdateListener(
                new ViewFinderUseCase.OnViewFinderOutputUpdateListener() {
                    @Override
                    public void onUpdated(ViewFinderUseCase.ViewFinderOutput viewFinderOutput) {
                        // If TextureView was already created, need to re-add it to change the
                        // SurfaceTexture.
                        ViewGroup viewGroup = (ViewGroup) textureView.getParent();
                        viewGroup.removeView(textureView);
                        viewGroup.addView(textureView);
                        textureView.setSurfaceTexture(viewFinderOutput.getSurfaceTexture());
                    }
                });

        for (int i = 0; i < FRAMES_UNTIL_VIEW_IS_READY; i++) {
            mIdlingResource.increment();
        }

        if (!bindToLifecycleSafely(mViewFinderUseCase, R.id.PreviewToggle)) {
            mViewFinderUseCase = null;
            return;
        }

        transformPreview();

        textureView.setSurfaceTextureListener(
                new SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(
                            SurfaceTexture surfaceTexture, int i, int i1) {
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(
                            SurfaceTexture surfaceTexture, int i, int i1) {
                        transformPreview();
                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                        return false;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                        // Wait until surface texture receives enough updates.
                        if (!mIdlingResource.isIdleNow()) {
                            mIdlingResource.decrement();
                        }
                    }
                });
    }

    void transformPreview() {
        String cameraId = null;
        LensFacing viewFinderLensFacing =
                ((CameraDeviceConfiguration) mViewFinderUseCase.getUseCaseConfiguration())
                        .getLensFacing(/*valueIfMissing=*/ null);
        if (viewFinderLensFacing != mCurrentCameraLensFacing) {
            throw new IllegalStateException(
                    "Invalid view finder lens facing: "
                            + viewFinderLensFacing
                            + " Should be: "
                            + mCurrentCameraLensFacing);
        }
        try {
            cameraId = CameraX.getCameraWithLensFacing(viewFinderLensFacing);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to get camera id for lens facing " + viewFinderLensFacing, e);
        }
        Size srcResolution = mViewFinderUseCase.getAttachedSurfaceResolution(cameraId);

        if (srcResolution.getWidth() == 0 || srcResolution.getHeight() == 0) {
            return;
        }

        TextureView textureView = this.findViewById(R.id.textureView);

        if (textureView.getWidth() == 0 || textureView.getHeight() == 0) {
            return;
        }

        Matrix matrix = new Matrix();

        int left = textureView.getLeft();
        int right = textureView.getRight();
        int top = textureView.getTop();
        int bottom = textureView.getBottom();

        // Compute the viewfinder ui size based on the available width, height, and ui orientation.
        int viewWidth = (right - left);
        int viewHeight = (bottom - top);

        int displayRotation = getDisplayRotation();
        Size scaled =
                calculateViewfinderViewDimens(
                        srcResolution, viewWidth, viewHeight, displayRotation);

        // Compute the center of the view.
        int centerX = viewWidth / 2;
        int centerY = viewHeight / 2;

        // Do corresponding rotation to correct the preview direction
        matrix.postRotate(-getDisplayRotation(), centerX, centerY);

        // Compute the scale value for center crop mode
        float xScale = scaled.getWidth() / (float) viewWidth;
        float yScale = scaled.getHeight() / (float) viewHeight;

        if (getDisplayRotation() == 90 || getDisplayRotation() == 270) {
            xScale = scaled.getWidth() / (float) viewHeight;
            yScale = scaled.getHeight() / (float) viewWidth;
        }

        // Only two digits after the decimal point are valid for postScale. Need to get ceiling of
        // two
        // digits floating value to do the scale operation. Otherwise, the result may be scaled not
        // large enough and will have some blank lines on the screen.
        xScale = new BigDecimal(xScale).setScale(2, BigDecimal.ROUND_CEILING).floatValue();
        yScale = new BigDecimal(yScale).setScale(2, BigDecimal.ROUND_CEILING).floatValue();

        // Do corresponding scale to resolve the deformation problem
        matrix.postScale(xScale, yScale, centerX, centerY);

        // Compute the new left/top positions to do translate
        int layoutL = centerX - (scaled.getWidth() / 2);
        int layoutT = centerY - (scaled.getHeight() / 2);

        // Do corresponding translation to be center crop
        matrix.postTranslate(layoutL, layoutT);

        textureView.setTransform(matrix);
    }

    /** @return One of 0, 90, 180, 270. */
    private int getDisplayRotation() {
        int displayRotation = getWindowManager().getDefaultDisplay().getRotation();

        switch (displayRotation) {
            case Surface.ROTATION_0:
                displayRotation = 0;
                break;
            case Surface.ROTATION_90:
                displayRotation = 90;
                break;
            case Surface.ROTATION_180:
                displayRotation = 180;
                break;
            case Surface.ROTATION_270:
                displayRotation = 270;
                break;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported display rotation: " + displayRotation);
        }

        return displayRotation;
    }

    private Size calculateViewfinderViewDimens(
            Size srcSize, int parentWidth, int parentHeight, int displayRotation) {
        int inWidth = srcSize.getWidth();
        int inHeight = srcSize.getHeight();
        if (displayRotation == 0 || displayRotation == 180) {
            // Need to reverse the width and height since we're in landscape orientation.
            inWidth = srcSize.getHeight();
            inHeight = srcSize.getWidth();
        }

        int outWidth = parentWidth;
        int outHeight = parentHeight;
        if (inWidth != 0 && inHeight != 0) {
            float vfRatio = inWidth / (float) inHeight;
            float parentRatio = parentWidth / (float) parentHeight;

            // Match shortest sides together.
            if (vfRatio < parentRatio) {
                outWidth = parentWidth;
                outHeight = Math.round(parentWidth / vfRatio);
            } else {
                outWidth = Math.round(parentHeight * vfRatio);
                outHeight = parentHeight;
            }
        }

        return new Size(outWidth, outHeight);
    }

    /**
     * Creates an image analysis use case.
     *
     * <p>This use case observes a stream of analysis results computed from the frames.
     */
    private void createImageAnalysisUseCase() {
        Button button = this.findViewById(R.id.AnalysisToggle);
        button.setBackgroundColor(Color.LTGRAY);
        enableImageAnalysisUseCase();

        button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Button buttonView = (Button) view;
                        if (mImageAnalysisUseCase != null) {
                            // Remove the use case
                            buttonView.setBackgroundColor(Color.RED);
                            CameraX.unbind(mImageAnalysisUseCase);
                            mImageAnalysisUseCase = null;
                        } else {
                            // Add the use case
                            buttonView.setBackgroundColor(Color.LTGRAY);
                            CameraXActivity.this.enableImageAnalysisUseCase();
                        }
                    }
                });

        Log.i(TAG, "Got UseCase: " + mImageAnalysisUseCase);
    }

    void enableImageAnalysisUseCase() {
        ImageAnalysisUseCaseConfiguration configuration =
                new ImageAnalysisUseCaseConfiguration.Builder()
                        .setLensFacing(mCurrentCameraLensFacing)
                        .setTargetName("ImageAnalysis")
                        .setCallbackHandler(new Handler(Looper.getMainLooper()))
                        .build();

        mImageAnalysisUseCase = new ImageAnalysisUseCase(configuration);
        TextView textView = this.findViewById(R.id.textView);
        mAnalysisIdlingResource.increment();

        if (!bindToLifecycleSafely(mImageAnalysisUseCase, R.id.AnalysisToggle)) {
            mImageAnalysisUseCase = null;
            return;
        }

        mImageAnalysisUseCase.setAnalyzer(
                new ImageAnalysisUseCase.Analyzer() {
                    @Override
                    public void analyze(ImageProxy image, int rotationDegrees) {
                        // Since we set the callback handler to a main thread handler, we can call
                        // setValue()
                        // here. If we weren't on the main thread, we would have to call postValue()
                        // instead.
                        mImageAnalysisResult.setValue(Long.toString(image.getTimestamp()));

                        if (!mAnalysisIdlingResource.isIdleNow()) {
                            mAnalysisIdlingResource.decrement();
                        }
                    }
                });
        mImageAnalysisResult.observe(
                this,
                new Observer<String>() {
                    @Override
                    public void onChanged(String text) {
                        if (mImageAnalysisFrameCount.getAndIncrement() % 30 == 0) {
                            textView.setText(
                                    "ImgCount: " + mImageAnalysisFrameCount.get() + " @ts: "
                                            + text);
                        }
                    }
                });
    }

    /**
     * Creates an image capture use case.
     *
     * <p>This use case takes a picture and saves it to a file, whenever the user clicks a button.
     */
    private void createImageCaptureUseCase() {

        Button button = this.findViewById(R.id.PhotoToggle);
        button.setBackgroundColor(Color.LTGRAY);
        enableImageCaptureUseCase();

        button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Button buttonView = (Button) view;
                        if (mImageCaptureUseCase != null) {
                            // Remove the use case
                            buttonView.setBackgroundColor(Color.RED);
                            CameraXActivity.this.disableImageCaptureUseCase();
                        } else {
                            // Add the use case
                            buttonView.setBackgroundColor(Color.LTGRAY);
                            CameraXActivity.this.enableImageCaptureUseCase();
                        }
                    }
                });

        Log.i(TAG, "Got UseCase: " + mImageCaptureUseCase);
    }

    void enableImageCaptureUseCase() {
        ImageCaptureUseCaseConfiguration configuration =
                new ImageCaptureUseCaseConfiguration.Builder()
                        .setLensFacing(mCurrentCameraLensFacing)
                        .setTargetName("ImageCapture")
                        .build();

        mImageCaptureUseCase = new ImageCaptureUseCase(configuration);

        if (!bindToLifecycleSafely(mImageCaptureUseCase, R.id.PhotoToggle)) {
            Button button = this.findViewById(R.id.Picture);
            button.setOnClickListener(null);
            mImageCaptureUseCase = null;
            return;
        }

        Button button = this.findViewById(R.id.Picture);
        final Format formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
        final File dir = this.getExternalFilesDir(null);
        button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mImageSavedIdlingResource.increment();

                        mImageCaptureUseCase.takePicture(
                                new File(
                                        dir,
                                        formatter.format(Calendar.getInstance().getTime())
                                                + ".jpg"),
                                new ImageCaptureUseCase.OnImageSavedListener() {
                                    @Override
                                    public void onImageSaved(File file) {
                                        Log.d(TAG, "Saved image to " + file);
                                        if (!mImageSavedIdlingResource.isIdleNow()) {
                                            mImageSavedIdlingResource.decrement();
                                        }
                                    }

                                    @Override
                                    public void onError(
                                            ImageCaptureUseCase.UseCaseError useCaseError,
                                            String message,
                                            Throwable cause) {
                                        Log.e(TAG, "Failed to save image.", cause);
                                        if (!mImageSavedIdlingResource.isIdleNow()) {
                                            mImageSavedIdlingResource.decrement();
                                        }
                                    }
                                });
                    }
                });

        refreshFlashButtonIcon();
    }

    void disableImageCaptureUseCase() {
        CameraX.unbind(mImageCaptureUseCase);

        mImageCaptureUseCase = null;
        Button button = this.findViewById(R.id.Picture);
        button.setOnClickListener(null);

        refreshFlashButtonIcon();
    }

    private void refreshFlashButtonIcon() {
        ImageButton flashToggle = findViewById(R.id.flash_toggle);
        if (mImageCaptureUseCase != null) {
            flashToggle.setVisibility(View.VISIBLE);
            flashToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FlashMode flashMode = mImageCaptureUseCase.getFlashMode();
                    if (flashMode == FlashMode.ON) {
                        mImageCaptureUseCase.setFlashMode(FlashMode.OFF);
                    } else if (flashMode == FlashMode.OFF) {
                        mImageCaptureUseCase.setFlashMode(FlashMode.AUTO);
                    } else if (flashMode == FlashMode.AUTO) {
                        mImageCaptureUseCase.setFlashMode(FlashMode.ON);
                    }
                    refreshFlashButtonIcon();
                }
            });
            FlashMode flashMode = mImageCaptureUseCase.getFlashMode();
            switch (flashMode) {
                case ON:
                    flashToggle.setImageResource(R.drawable.ic_flash_on);
                    break;
                case OFF:
                    flashToggle.setImageResource(R.drawable.ic_flash_off);
                    break;
                case AUTO:
                    flashToggle.setImageResource(R.drawable.ic_flash_auto);
                    break;
            }

        } else {
            flashToggle.setVisibility(View.GONE);
            flashToggle.setOnClickListener(null);
        }
    }

    /**
     * Creates a video capture use case.
     *
     * <p>This use case records a video segment and saves it to a file, in response to user button
     * clicks.
     */
    private void createVideoCaptureUseCase() {
        Button button = this.findViewById(R.id.VideoToggle);
        button.setBackgroundColor(Color.LTGRAY);
        enableVideoCaptureUseCase();

        mVideoFileSaver = new VideoFileSaver();
        mVideoFileSaver.setRootDirectory(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));

        button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Button buttonView = (Button) view;
                        if (mVideoCaptureUseCase != null) {
                            // Remove the use case
                            buttonView.setBackgroundColor(Color.RED);
                            CameraXActivity.this.disableVideoCaptureUseCase();
                        } else {
                            // Add the use case
                            buttonView.setBackgroundColor(Color.LTGRAY);
                            CameraXActivity.this.enableVideoCaptureUseCase();
                        }
                    }
                });

        Log.i(TAG, "Got UseCase: " + mVideoCaptureUseCase);
    }

    void enableVideoCaptureUseCase() {
        VideoCaptureUseCaseConfiguration configuration =
                new VideoCaptureUseCaseConfiguration.Builder()
                        .setLensFacing(mCurrentCameraLensFacing)
                        .setTargetName("VideoCapture")
                        .build();

        mVideoCaptureUseCase = new VideoCaptureUseCase(configuration);

        if (!bindToLifecycleSafely(mVideoCaptureUseCase, R.id.VideoToggle)) {
            Button button = this.findViewById(R.id.Video);
            button.setOnClickListener(null);
            mVideoCaptureUseCase = null;
            return;
        }

        Button button = this.findViewById(R.id.Video);
        button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Button buttonView = (Button) view;
                        String text = button.getText().toString();
                        if (text.equals("Record") && !mVideoFileSaver.isSaving()) {
                            mVideoCaptureUseCase.startRecording(
                                    mVideoFileSaver.getNewVideoFile(), mVideoFileSaver);
                            mVideoFileSaver.setSaving();
                            buttonView.setText("Stop");
                        } else if (text.equals("Stop") && mVideoFileSaver.isSaving()) {
                            buttonView.setText("Record");
                            mVideoCaptureUseCase.stopRecording();
                        } else if (text.equals("Record") && mVideoFileSaver.isSaving()) {
                            buttonView.setText("Stop");
                            mVideoFileSaver.setSaving();
                        } else if (text.equals("Stop") && !mVideoFileSaver.isSaving()) {
                            buttonView.setText("Record");
                        }
                    }
                });
    }

    void disableVideoCaptureUseCase() {
        Button button = this.findViewById(R.id.Video);
        button.setOnClickListener(null);
        CameraX.unbind(mVideoCaptureUseCase);

        mVideoCaptureUseCase = null;
    }

    /** Creates all the use cases. */
    private void createUseCases() {
        createImageCaptureUseCase();
        createViewFinderUseCase();
        createImageAnalysisUseCase();
        createVideoCaptureUseCase();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_xmain);

        StrictMode.VmPolicy policy =
                new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build();
        StrictMode.setVmPolicy(policy);

        // Get params from adb extra string
        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null) {
            String newCameraDirection = bundle.getString(INTENT_EXTRA_CAMERA_DIRECTION);
            if (newCameraDirection != null) {
                mCurrentCameraDirection = newCameraDirection;
            }
        }

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        CameraXActivity.this.setupCamera();
                    }
                })
                .start();
        setupPermissions();
    }

    private void setupCamera() {
        try {
            // Wait for permissions before proceeding.
            if (!mCompletableFuture.get()) {
                Log.d(TAG, "Permissions denied.");
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception occurred getting permission future: " + e);
        }

        Log.d(TAG, "Camera direction: " + mCurrentCameraDirection);
        if (mCurrentCameraDirection.equalsIgnoreCase("BACKWARD")) {
            mCurrentCameraLensFacing = LensFacing.BACK;
        } else if (mCurrentCameraDirection.equalsIgnoreCase("FORWARD")) {
            mCurrentCameraLensFacing = LensFacing.FRONT;
        } else {
            throw new RuntimeException("Invalid camera direction: " + mCurrentCameraDirection);
        }
        Log.d(TAG, "Using camera lens facing: " + mCurrentCameraLensFacing);

        // Run this on the UI thread to manipulate the Textures & Views.
        CameraXActivity.this.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        CameraXActivity.this.createUseCases();

                        ImageButton directionToggle = findViewById(R.id.direction_toggle);
                        directionToggle.setVisibility(View.VISIBLE);
                        directionToggle.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (mCurrentCameraLensFacing == LensFacing.BACK) {
                                    mCurrentCameraLensFacing = LensFacing.FRONT;
                                } else if (mCurrentCameraLensFacing == LensFacing.FRONT) {
                                    mCurrentCameraLensFacing = LensFacing.BACK;
                                }

                                Log.d(TAG, "Change camera direction: " + mCurrentCameraLensFacing);

                                // Rebind all use cases.
                                CameraX.unbindAll();
                                if (mImageCaptureUseCase != null) {
                                    enableImageCaptureUseCase();
                                }
                                if (mViewFinderUseCase != null) {
                                    enableViewFinderUseCase();
                                }
                                if (mImageAnalysisUseCase != null) {
                                    enableImageAnalysisUseCase();
                                }
                                if (mVideoCaptureUseCase != null) {
                                    enableVideoCaptureUseCase();
                                }
                            }
                        });

                        ImageButton torchToggle = findViewById(R.id.torch_toggle);
                        torchToggle.setVisibility(View.VISIBLE);
                        torchToggle.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (mViewFinderUseCase != null) {
                                    boolean toggledState = !mViewFinderUseCase.isTorchOn();
                                    Log.d(TAG, "Set camera torch: " + toggledState);
                                    mViewFinderUseCase.enableTorch(toggledState);
                                }
                            }
                        });
                    }
                });
    }

    private void setupPermissions() {
        if (!allPermissionsGranted()) {
            makePermissionRequest();
        } else {
            mSettableResult.set(true);
            mCompletableFuture.run();
        }
    }

    private void makePermissionRequest() {
        ActivityCompat.requestPermissions(this, getRequiredPermissions(), PERMISSIONS_REQUEST_CODE);
    }

    /** Returns true if all the necessary permissions have been granted already. */
    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /** Tries to acquire all the necessary permissions through a dialog. */
    private String[] getRequiredPermissions() {
        PackageInfo info;
        try {
            info =
                    getPackageManager()
                            .getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException exception) {
            Log.e(TAG, "Failed to obtain all required permissions.", exception);
            return new String[0];
        }
        String[] permissions = info.requestedPermissions;
        if (permissions != null && permissions.length > 0) {
            return permissions;
        } else {
            return new String[0];
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permissions Granted.");
                    mSettableResult.set(true);
                    mCompletableFuture.run();
                } else {
                    Log.d(TAG, "Permissions Denied.");
                    mSettableResult.set(false);
                    mCompletableFuture.run();
                }
                return;
            }
            default:
                // No-op
        }
    }

    private boolean bindToLifecycleSafely(BaseUseCase useCase, int buttonViewId) {
        try {
            CameraX.bindToLifecycle(this, useCase);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(getApplicationContext(), "Bind too many use cases.", Toast.LENGTH_SHORT)
                    .show();
            Button button = this.findViewById(buttonViewId);
            button.setBackgroundColor(Color.RED);
            return false;
        }

        return true;
    }

    /** A {@link Callable} whose return value can be set. */
    private static final class SettableCallable<V> implements Callable<V> {
        private final AtomicReference<V> mValue = new AtomicReference<>();

        public void set(V value) {
            mValue.set(value);
        }

        @Override
        public V call() {
            return mValue.get();
        }
    }

    ViewFinderUseCase getViewFinderUseCase() {
        return mViewFinderUseCase;
    }

    ImageAnalysisUseCase getImageAnalysisUseCase() {
        return mImageAnalysisUseCase;
    }

    ImageCaptureUseCase getImageCaptureUseCase() {
        return mImageCaptureUseCase;
    }

    VideoCaptureUseCase getVideoCaptureUseCase() {
        return mVideoCaptureUseCase;
    }
}
