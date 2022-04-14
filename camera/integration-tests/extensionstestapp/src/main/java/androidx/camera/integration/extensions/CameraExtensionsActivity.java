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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import androidx.camera.extensions.ExtensionsManager;
import androidx.camera.integration.extensions.utils.CameraSelectorUtil;
import androidx.camera.integration.extensions.utils.ExtensionModeUtil;
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity;
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

    /**
     * The camera with the specified camera id will be opened if the intent used to launch the
     * activity includes the information.
     */
    @VisibleForTesting
    static final String INTENT_EXTRA_CAMERA_ID = "camera_id";
    /**
     * The specified extension mode will be tried to be enabled if the intent used to launch the
     * activity includes the information.
     */
    @VisibleForTesting
    static final String INTENT_EXTRA_EXTENSION_MODE = "extension_mode";
    /**
     * The captured image will be deleted automatically if the intent used to launch the activity
     * includes the setting as true.
     */
    @VisibleForTesting
    static final String INTENT_EXTRA_DELETE_CAPTURED_IMAGE = "delete_captured_image";

    private static final String TAG = "CameraExtensionActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 42;

    private CameraSelector mCurrentCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

    boolean mPermissionsGranted = false;
    private CallbackToFutureAdapter.Completer<Boolean> mPermissionCompleter;

    @Nullable
    private Preview mPreview;

    @Nullable
    private ImageCapture mImageCapture;

    @ExtensionMode.Mode
    private int mCurrentExtensionMode = ExtensionMode.BOKEH;

    // Espresso testing variables
    @VisibleForTesting
    CountingIdlingResource mInitializationIdlingResource = new CountingIdlingResource(
            "Initialization");

    @VisibleForTesting
    CountingIdlingResource mTakePictureIdlingResource = new CountingIdlingResource("TakePicture");

    @VisibleForTesting
    CountingIdlingResource mPreviewViewIdlingResource = new CountingIdlingResource("PreviewView");

    private PreviewView mPreviewView;

    ProcessCameraProvider mCameraProvider;

    Camera mCamera;

    ExtensionsManager mExtensionsManager;

    boolean mDeleteCapturedImage = false;

    void setupButtons() {
        Button btnToggleMode = findViewById(R.id.PhotoToggle);
        Button btnSwitchCamera = findViewById(R.id.Switch);
        btnToggleMode.setOnClickListener(view -> bindUseCasesWithNextExtensionMode());
        btnSwitchCamera.setOnClickListener(view -> switchCameras());
    }

    void switchCameras() {
        mCameraProvider.unbindAll();
        mCurrentCameraSelector = (mCurrentCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;
        bindUseCasesWithCurrentExtensionMode();
    }

    void bindUseCasesWithNextExtensionMode() {
        do {
            mCurrentExtensionMode = getNextExtensionMode(mCurrentExtensionMode);
        } while (!bindUseCasesWithCurrentExtensionMode());
    }

    boolean bindUseCasesWithCurrentExtensionMode() {
        if (!mExtensionsManager.isExtensionAvailable(mCurrentCameraSelector,
                mCurrentExtensionMode)) {
            return false;
        }

        mPreviewViewIdlingResource.increment();

        ImageCapture.Builder imageCaptureBuilder = new ImageCapture.Builder().setTargetName(
                "ImageCapture");
        mImageCapture = imageCaptureBuilder.build();

        Preview.Builder previewBuilder = new Preview.Builder().setTargetName("Preview");

        mPreview = previewBuilder.build();
        mPreview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        // Observes the stream state for the unit tests to know the preview status.
        mPreviewView.getPreviewStreamState().removeObservers(this);
        mPreviewView.getPreviewStreamState().observe(this, streamState -> {
            if (streamState == PreviewView.StreamState.STREAMING
                    && !mPreviewViewIdlingResource.isIdleNow()) {
                mPreviewViewIdlingResource.decrement();
            }
        });

        CameraSelector cameraSelector = mExtensionsManager.getExtensionEnabledCameraSelector(
                mCurrentCameraSelector, mCurrentExtensionMode);

        mCameraProvider.unbindAll();
        mCamera = mCameraProvider.bindToLifecycle(this, cameraSelector, mImageCapture, mPreview);

        // Update the UI and save location for ImageCapture
        Button toggleButton = findViewById(R.id.PhotoToggle);
        String extensionModeString =
                ExtensionModeUtil.getExtensionModeStringFromId(mCurrentExtensionMode);
        toggleButton.setText(extensionModeString);

        Button captureButton = findViewById(R.id.Picture);

        Format formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US);
        File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "ExtensionsPictures");

        captureButton.setOnClickListener((view) -> {
            mTakePictureIdlingResource.increment();

            String fileName = formatter.format(Calendar.getInstance().getTime())
                    + extensionModeString + ".jpg";
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

                            Uri outputUri = outputFileResults.getSavedUri();

                            if (mDeleteCapturedImage) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    try {
                                        getContentResolver().delete(outputUri, null, null);
                                    } catch (RuntimeException e) {
                                        Log.w(TAG, "Failed to delete uri: " + outputUri);
                                    }
                                } else {
                                    if (!saveFile.delete()) {
                                        Log.w(TAG, "Failed to delete file: " + saveFile);
                                    }
                                }
                            } else {
                                // Trigger MediaScanner to scan the file
                                // The output Uri is already inserted into media store if the
                                // device API level is equal to or larger than Android Q(29)."
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                    Intent intent = new Intent(
                                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                    intent.setData(Uri.fromFile(saveFile));
                                    sendBroadcast(intent);
                                }

                                Toast.makeText(getApplicationContext(),
                                        "Saved image to " + saveFile,
                                        Toast.LENGTH_SHORT).show();
                            }
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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_extensions);

        mInitializationIdlingResource.increment();

        String cameraId = getIntent().getStringExtra(INTENT_EXTRA_CAMERA_ID);
        if (cameraId != null) {
            mCurrentCameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId);
        }

        mCurrentExtensionMode = getIntent().getIntExtra(INTENT_EXTRA_EXTENSION_MODE,
                mCurrentExtensionMode);

        mDeleteCapturedImage = getIntent().getBooleanExtra(INTENT_EXTRA_DELETE_CAPTURED_IMAGE,
                mDeleteCapturedImage);

        StrictMode.VmPolicy policy =
                new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build();
        StrictMode.setVmPolicy(policy);
        mPreviewView = findViewById(R.id.previewView);
        setupPinchToZoomAndTapToFocus(mPreviewView);
        Futures.addCallback(setupPermissions(), new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@Nullable Boolean result) {
                mPermissionsGranted = Preconditions.checkNotNull(result);

                if (!mPermissionsGranted) {
                    Log.d(TAG, "Required permissions are not all granted!");
                    Toast.makeText(CameraExtensionsActivity.this, "Required permissions are not "
                            + "all granted!", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                        ProcessCameraProvider.getInstance(CameraExtensionsActivity.this);

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

    @Override
    public boolean onCreateOptionsMenu(@Nullable Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_validation_tool) {
            Intent intent = new Intent(this, CameraValidationResultActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void setupCamera() {
        if (!mPermissionsGranted) {
            Log.d(TAG, "Permissions denied.");
            return;
        }

        mCamera = mCameraProvider.bindToLifecycle(this, mCurrentCameraSelector);
        ListenableFuture<ExtensionsManager> extensionsManagerFuture =
                ExtensionsManager.getInstanceAsync(getApplicationContext(), mCameraProvider);

        Futures.addCallback(extensionsManagerFuture,
                new FutureCallback<ExtensionsManager>() {
                    @Override
                    public void onSuccess(@Nullable ExtensionsManager extensionsManager) {
                        mExtensionsManager = extensionsManager;
                        if (!bindUseCasesWithCurrentExtensionMode()) {
                            bindUseCasesWithNextExtensionMode();
                        }
                        setupButtons();
                        mInitializationIdlingResource.decrement();
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
    @SuppressWarnings("deprecation")
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != PERMISSIONS_REQUEST_CODE) {
            return;
        }

        // If request is cancelled, the result arrays are empty.
        if (grantResults.length == 0) {
            mPermissionCompleter.set(false);
            return;
        }

        boolean allPermissionGranted = true;

        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                allPermissionGranted = false;
                break;
            }
        }

        Log.d(TAG, "All permissions granted: " + allPermissionGranted);
        mPermissionCompleter.set(allPermissionGranted);
    }

    @ExtensionMode.Mode
    private int getNextExtensionMode(@ExtensionMode.Mode int extensionMode) {
        switch (extensionMode) {
            case ExtensionMode.NONE:
                return ExtensionMode.BOKEH;
            case ExtensionMode.BOKEH:
                return ExtensionMode.HDR;
            case ExtensionMode.HDR:
                return ExtensionMode.NIGHT;
            case ExtensionMode.NIGHT:
                return ExtensionMode.FACE_RETOUCH;
            case ExtensionMode.FACE_RETOUCH:
                return ExtensionMode.AUTO;
            case ExtensionMode.AUTO:
                return ExtensionMode.NONE;
            default:
                throw new IllegalStateException("Unexpected value: " + extensionMode);
        }
    }

    @VisibleForTesting
    boolean isExtensionModeSupported(@NonNull String cameraId, @ExtensionMode.Mode int mode) {
        CameraSelector cameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId);

        return mExtensionsManager.isExtensionAvailable(cameraSelector, mode);
    }

    @VisibleForTesting
    @ExtensionMode.Mode
    int getCurrentExtensionMode() {
        return mCurrentExtensionMode;
    }
}
