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
package androidx.camera.integration.extensions;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.Extensions;
import androidx.camera.extensions.ExtensionsManager;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.math.MathUtils;
import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** An activity that shows off how extensions can be applied */
public class CameraExtensionsActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "CameraExtensionActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 42;

    private CameraSelector mCurrentCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

    boolean mPermissionsGranted = false;
    private CallbackToFutureAdapter.Completer<Boolean> mPermissionCompleter;

    @Nullable
    private Preview mPreview;

    @Nullable
    private ImageCapture mImageCapture;

    @NonNull
    private ImageCaptureType mCurrentImageCaptureType = ImageCaptureType.IMAGE_CAPTURE_TYPE_DEFAULT;

    // Espresso testing variables
    @VisibleForTesting
    CountingIdlingResource mTakePictureIdlingResource = new CountingIdlingResource("TakePicture");

    private PreviewView mPreviewView;

    ProcessCameraProvider mCameraProvider;

    Camera mCamera;
    Extensions mExtensions;

    enum ImageCaptureType {

        IMAGE_CAPTURE_TYPE_HDR(0),
        IMAGE_CAPTURE_TYPE_BOKEH(1),
        IMAGE_CAPTURE_TYPE_NIGHT(2),
        IMAGE_CAPTURE_TYPE_BEAUTY(3),
        IMAGE_CAPTURE_TYPE_AUTO(4),
        IMAGE_CAPTURE_TYPE_DEFAULT(5);

        private final int mOrdinal;
        private static final ImageCaptureType[] sTypes = ImageCaptureType.values();

        ImageCaptureType(int i) {
            this.mOrdinal = i;
        }

        ImageCaptureType getNextType() {
            return sTypes[(mOrdinal + 1) % sTypes.length];
        }
    }

    void setupButtons() {
        Button btnToggleMode = findViewById(R.id.PhotoToggle);
        Button btnSwitchCamera = findViewById(R.id.Switch);
        btnToggleMode.setOnClickListener(view -> bindUseCasesWithNextExtension());
        btnSwitchCamera.setOnClickListener(view -> switchCameras());
    }

    void switchCameras() {
        mCameraProvider.unbindAll();
        mCurrentCameraSelector = (mCurrentCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;
        bindUseCasesWithExtension(mCurrentImageCaptureType);
    }

    @ExtensionMode.Mode
    int extensionModeFrom(ImageCaptureType imageCaptureType) {
        switch (imageCaptureType) {
            case IMAGE_CAPTURE_TYPE_HDR:
                return ExtensionMode.HDR;
            case IMAGE_CAPTURE_TYPE_BOKEH:
                return ExtensionMode.BOKEH;
            case IMAGE_CAPTURE_TYPE_NIGHT:
                return ExtensionMode.NIGHT;
            case IMAGE_CAPTURE_TYPE_BEAUTY:
                return ExtensionMode.BEAUTY;
            case IMAGE_CAPTURE_TYPE_AUTO:
                return ExtensionMode.AUTO;
            case IMAGE_CAPTURE_TYPE_DEFAULT:
                return ExtensionMode.NONE;
            default:
                throw new IllegalArgumentException(
                        "ImageCaptureType does not exist: " + imageCaptureType);
        }
    }

    void bindUseCasesWithNextExtension() {
        do {
            mCurrentImageCaptureType = mCurrentImageCaptureType.getNextType();
        } while (!bindUseCasesWithExtension(mCurrentImageCaptureType));
    }

    // TODO(b/162875208) Suppress until new extensions API made public
    @SuppressLint("RestrictedAPI")
    boolean bindUseCasesWithExtension(ImageCaptureType imageCaptureType) {
        // Check that extension can be enabled and if so enable it
        @ExtensionMode.Mode
        int extensionMode = extensionModeFrom(imageCaptureType);
        if (!mExtensions.isExtensionAvailable(mCameraProvider, mCurrentCameraSelector,
                extensionMode)) {
            return false;
        }

        ImageCapture.Builder imageCaptureBuilder = new ImageCapture.Builder().setTargetName(
                "ImageCapture");
        mImageCapture = imageCaptureBuilder.build();

        Preview.Builder previewBuilder = new Preview.Builder().setTargetName("Preview");

        mPreview = previewBuilder.build();
        mPreview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        CameraSelector cameraSelector = mExtensions.getExtensionCameraSelector(
                mCurrentCameraSelector, extensionMode);

        mCameraProvider.unbindAll();
        mCamera = mCameraProvider.bindToLifecycle(this, cameraSelector, mImageCapture, mPreview);

        // Update the UI and save location for ImageCapture
        Button toggleButton = findViewById(R.id.PhotoToggle);
        toggleButton.setText(mCurrentImageCaptureType.toString());

        Button captureButton = findViewById(R.id.Picture);

        Format formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US);
        File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "ExtensionsPictures");

        captureButton.setOnClickListener((view) -> {
            mTakePictureIdlingResource.increment();

            String fileName = formatter.format(Calendar.getInstance().getTime())
                    + mCurrentImageCaptureType.name() + ".jpg";
            File saveFile = new File(dir, fileName);
            ImageCapture.OutputFileOptions outputFileOptions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        "Pictures/ExtensionsPictures");
                outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues).build();
            } else {
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                outputFileOptions = new ImageCapture.OutputFileOptions.Builder(saveFile).build();
            }

            mImageCapture.takePicture(
                    outputFileOptions,
                    ContextCompat.getMainExecutor(CameraExtensionsActivity.this),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(
                                @NonNull ImageCapture.OutputFileResults outputFileResults) {
                            Log.d(TAG, "Saved image to " + saveFile);

                            if (!mTakePictureIdlingResource.isIdleNow()) {
                                mTakePictureIdlingResource.decrement();
                            }

                            // Trigger MediaScanner to scan the file
                            if (outputFileResults.getSavedUri() == null) {
                                Intent intent = new Intent(
                                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                intent.setData(Uri.fromFile(saveFile));
                                sendBroadcast(intent);
                            }

                            Toast.makeText(getApplicationContext(),
                                    "Saved image to " + saveFile,
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e(TAG, "Failed to save image - " + exception.getMessage(),
                                    exception.getCause());
                        }
                    });
        });

        return true;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_extensions);

        StrictMode.VmPolicy policy =
                new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build();
        StrictMode.setVmPolicy(policy);
        mPreviewView = findViewById(R.id.previewView);
        setupPinchToZoomAndTapToFocus(mPreviewView);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        Futures.addCallback(setupPermissions(), new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@Nullable Boolean result) {
                mPermissionsGranted = Preconditions.checkNotNull(result);
                Futures.addCallback(cameraProviderFuture,
                        new FutureCallback<ProcessCameraProvider>() {
                            @Override
                            public void onSuccess(@Nullable ProcessCameraProvider result) {
                                mCameraProvider = result;
                                setupCamera();
                            }

                            @Override
                            public void onFailure(@NonNull Throwable t) {
                                throw new RuntimeException("Failed to get camera provider", t);
                            }
                        }, ContextCompat.getMainExecutor(CameraExtensionsActivity.this));
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                throw new RuntimeException("Failed to get permissions", t);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // TODO(b/162875208) Suppress until new extensions API made public
    @SuppressLint("RestrictedAPI")
    void setupCamera() {
        if (!mPermissionsGranted) {
            Log.d(TAG, "Permissions denied.");
            return;
        }

        mCamera = mCameraProvider.bindToLifecycle(this, mCurrentCameraSelector);
        ListenableFuture<ExtensionsManager.ExtensionsAvailability> availability =
                ExtensionsManager.init(getApplicationContext());

        Futures.addCallback(availability,
                new FutureCallback<ExtensionsManager.ExtensionsAvailability>() {
                    @Override
                    public void onSuccess(
                            @Nullable ExtensionsManager.ExtensionsAvailability availability) {
                        // Run this on the UI thread to manipulate the Textures & Views.
                        switch (availability) {
                            case LIBRARY_AVAILABLE:
                            case NONE:
                                mExtensions = ExtensionsManager.getExtensions(
                                        getApplicationContext());
                                ExtensionsManager.setExtensionsErrorListener((errorCode) ->
                                        Log.d(TAG, "Extensions error in error code: " + errorCode));
                                bindUseCasesWithNextExtension();
                                setupButtons();
                                break;
                            case LIBRARY_UNAVAILABLE_ERROR_LOADING:
                            case LIBRARY_UNAVAILABLE_MISSING_IMPLEMENTATION:
                                throw new RuntimeException("Failed to load up extensions "
                                        + "implementation");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Throwable throwable) {
                    }
                },
                ContextCompat.getMainExecutor(CameraExtensionsActivity.this)
        );
    }

    ScaleGestureDetector.SimpleOnScaleGestureListener mScaleGestureListener =
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    if (mCamera == null) {
                        return true;
                    }

                    CameraInfo cameraInfo = mCamera.getCameraInfo();
                    CameraControl cameraControl = mCamera.getCameraControl();
                    float newZoom =
                            cameraInfo.getZoomState().getValue().getZoomRatio()
                                    * detector.getScaleFactor();
                    float clampedNewZoom = MathUtils.clamp(newZoom,
                            cameraInfo.getZoomState().getValue().getMinZoomRatio(),
                            cameraInfo.getZoomState().getValue().getMaxZoomRatio());

                    ListenableFuture<Void> listenableFuture = cameraControl.setZoomRatio(
                            clampedNewZoom);
                    Futures.addCallback(listenableFuture, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void result) {
                            Log.d(TAG, "setZoomRatio onSuccess: " + clampedNewZoom);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            Log.d(TAG, "setZoomRatio failed, " + t);
                        }
                    }, ContextCompat.getMainExecutor(CameraExtensionsActivity.this));
                    return true;
                }
            };

    private void setupPinchToZoomAndTapToFocus(PreviewView previewView) {
        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(this, mScaleGestureListener);

        previewView.setOnTouchListener((view, motionEvent) -> {
            scaleDetector.onTouchEvent(motionEvent);

            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                if (mCamera == null) {
                    return true;
                }
                MeteringPoint point =
                        previewView.getMeteringPointFactory().createPoint(
                                motionEvent.getX(), motionEvent.getY());

                mCamera.getCameraControl().startFocusAndMetering(
                        new FocusMeteringAction.Builder(point).build()).addListener(() -> {},
                        ContextCompat.getMainExecutor(CameraExtensionsActivity.this));
            }
            return true;
        });
    }

    private ListenableFuture<Boolean> setupPermissions() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            mPermissionCompleter = completer;
            if (!allPermissionsGranted()) {
                makePermissionRequest();
            } else {
                mPermissionCompleter.set(true);
            }

            return "get_permissions";
        });
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
            info = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_PERMISSIONS);
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

    @Nullable
    public Preview getPreview() {
        return mPreview;
    }

    @Nullable
    public ImageCapture getImageCapture() {
        return mImageCapture;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permissions Granted.");
                    mPermissionCompleter.set(true);
                } else {
                    Log.d(TAG, "Permissions Denied.");
                    mPermissionCompleter.set(false);
                }
                return;
            }
            default:
                // No-op
        }
    }
}
